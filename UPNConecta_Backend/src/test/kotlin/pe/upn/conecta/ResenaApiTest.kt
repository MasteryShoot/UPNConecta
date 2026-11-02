package pe.upn.conecta

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * Pruebas de la HU2.03 recorriendo el CRUD completo contra la API
 * (Flyway crea las tablas en una base H2 en memoria).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ResenaApiTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper

    /** Crea la cuenta (si hace falta) e inicia sesión; devuelve el encabezado Authorization. */
    private fun sesion(correo: String, nombre: String): String {
        mvc.post("/api/auth/registro") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nombreCompleto":"$nombre","correo":"$correo","sedeCodigo":"los_olivos","clave":"secreta1"}"""
        }
        val respuesta = mvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"correo":"$correo","clave":"secreta1"}"""
        }.andReturn().response.contentAsString
        return "Bearer " + mapper.readTree(respuesta)["token"].asText()
    }

    // Criterios de "limpieza" según V2__datos_iniciales.sql: 4, 5 y 6
    private fun cuerpo(
        puntajes: List<Int> = listOf(4, 3, 5),
        comentario: String? = null
    ) = mapper.writeValueAsString(
        mapOf(
            "codigoSede" to "los_olivos",
            "area" to "limpieza",
            "experiencia" to 4,
            "comentario" to comentario,
            "esAnonima" to false,
            "sinReciclaje" to false,
            "calificaciones" to puntajes.mapIndexed { i, p -> mapOf("idCriterio" to 4 + i, "puntaje" to p) }
        )
    )

    @Test
    @Order(1)
    fun `lista los criterios del area`() {
        mvc.get("/api/criterios?area=limpieza").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[0].nombre") { value("Baños") }
        }
    }

    @Test
    @Order(2)
    fun `sin sesion no se puede publicar ni con un token falso`() {
        mvc.post("/api/resenas") {
            contentType = MediaType.APPLICATION_JSON
            content = cuerpo()
        }.andExpect { status { isUnauthorized() } }

        mvc.post("/api/resenas") {
            contentType = MediaType.APPLICATION_JSON
            content = cuerpo()
            header("Authorization", "Bearer token.inventado.falso")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    @Order(3)
    fun `rechaza si falta calificar un criterio`() {
        val jose = sesion("n00329320@upn.pe", "Jose Pariona")
        mvc.post("/api/resenas") {
            contentType = MediaType.APPLICATION_JSON
            content = cuerpo(puntajes = listOf(4, 3))
            header("Authorization", jose)
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    @Order(4)
    fun `registra, consulta, actualiza y elimina la evaluacion`() {
        val jose = sesion("n00329320@upn.pe", "Jose Pariona")
        val otro = sesion("n00000001@upn.pe", "Otra Persona")

        // CREATE
        val respuesta = mvc.post("/api/resenas") {
            contentType = MediaType.APPLICATION_JSON
            content = cuerpo()
            header("Authorization", jose)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.promedio") { value(4.0) }
            jsonPath("$.periodo") { value("2026-2") }
        }.andReturn().response.contentAsString
        val id = mapper.readTree(respuesta)["idResena"].asInt()

        // Segundo intento en el mismo ciclo -> 409
        mvc.post("/api/resenas") {
            contentType = MediaType.APPLICATION_JSON
            content = cuerpo()
            header("Authorization", jose)
        }.andExpect { status { isConflict() } }

        // READ
        mvc.get("/api/resenas/mia?sede=los_olivos&area=limpieza") {
            header("Authorization", jose)
        }.andExpect {
            status { isOk() }
            jsonPath("$.idResena") { value(id) }
            jsonPath("$.calificaciones[2].puntaje") { value(5) }
        }

        // UPDATE
        mvc.put("/api/resenas/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = cuerpo(
                puntajes = listOf(2, 2, 2),
                comentario = "Los baños del segundo piso casi siempre están sin papel en la noche."
            )
            header("Authorization", jose)
        }.andExpect {
            status { isOk() }
            jsonPath("$.promedio") { value(2.0) }
        }

        // La opinión pública no muestra el código ni el correo
        mvc.get("/api/resenas?sede=los_olivos&area=limpieza").andExpect {
            status { isOk() }
            jsonPath("$[0].autor") { value("Jose P.") }
            jsonPath("$[0].codigoUsuario") { doesNotExist() }
        }

        // Otro usuario no puede eliminarla
        mvc.delete("/api/resenas/$id") {
            header("Authorization", otro)
        }.andExpect { status { isForbidden() } }

        // DELETE
        mvc.delete("/api/resenas/$id") {
            header("Authorization", jose)
        }.andExpect { status { isOk() } }
        mvc.get("/api/resenas/mia?sede=los_olivos&area=limpieza") {
            header("Authorization", jose)
        }.andExpect { status { isNotFound() } }
    }
}
