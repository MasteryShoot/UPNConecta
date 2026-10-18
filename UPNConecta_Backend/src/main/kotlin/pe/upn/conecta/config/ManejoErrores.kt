package pe.upn.conecta.config

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import pe.upn.conecta.dto.MensajeResponse

/** Error de negocio con el código HTTP que se debe devolver. */
class ApiException(val estado: HttpStatus, mensaje: String) : RuntimeException(mensaje)

/**
 * Convierte las excepciones en respuestas JSON {"mensaje": "..."}
 * para que la app pueda mostrar el error al usuario.
 */
@RestControllerAdvice
class ManejoErrores {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun negocio(e: ApiException) =
        ResponseEntity.status(e.estado).body(MensajeResponse(e.message ?: "Error"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException): ResponseEntity<MensajeResponse> {
        val mensaje = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Datos inválidos"
        return ResponseEntity.badRequest().body(MensajeResponse(mensaje))
    }

    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun formato(e: Exception) =
        ResponseEntity.badRequest().body(MensajeResponse("La solicitud tiene datos incompletos o mal formados"))

    // Respaldo por si dos envíos llegan al mismo tiempo y choca la restricción UNIQUE
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun integridad(e: DataIntegrityViolationException) =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(MensajeResponse("Ya existe una evaluación registrada con esos datos"))

    @ExceptionHandler(NoResourceFoundException::class)
    fun rutaInexistente(e: NoResourceFoundException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(MensajeResponse("La ruta solicitada no existe"))

    @ExceptionHandler(Exception::class)
    fun inesperado(e: Exception): ResponseEntity<MensajeResponse> {
        // Se registra solo el tipo de error, sin datos del usuario
        log.error("Error no controlado: {}", e.javaClass.simpleName, e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(MensajeResponse("Ocurrió un error en el servidor, inténtalo más tarde"))
    }
}
