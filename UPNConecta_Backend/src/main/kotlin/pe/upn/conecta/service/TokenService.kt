package pe.upn.conecta.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Token de sesión (JWT firmado con HS256). El backend lo entrega al iniciar sesión y la app
 * lo envía en cada operación privada; así el autor de una reseña no se puede falsificar.
 */
@Service
class TokenService(@Value("\${app.jwt-secret}") secreto: String) {

    private val algoritmo: Algorithm

    init {
        require(secreto.length >= 32) { "JWT_SECRET debe tener al menos 32 caracteres" }
        algoritmo = Algorithm.HMAC256(secreto)
    }

    private val verificador = JWT.require(algoritmo).withIssuer(EMISOR).build()

    fun crear(codigoUsuario: String): String {
        val ahora = Instant.now()
        return JWT.create()
            .withIssuer(EMISOR)
            .withSubject(codigoUsuario)
            .withIssuedAt(ahora)
            .withExpiresAt(ahora.plus(DURACION))
            .sign(algoritmo)
    }

    /** Devuelve el código del usuario, o null si el token es falso o ya venció. */
    fun verificar(token: String): String? =
        try {
            verificador.verify(token).subject
        } catch (e: JWTVerificationException) {
            null
        }

    private companion object {
        const val EMISOR = "upnconecta-backend"
        val DURACION: Duration = Duration.ofDays(7)
    }
}
