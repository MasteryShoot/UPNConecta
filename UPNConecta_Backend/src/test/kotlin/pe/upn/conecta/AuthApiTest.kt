package pe.upn.conecta

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/** Registro e inicio de sesión contra la API (T2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AuthApiTest {

    @Autowired lateinit var mvc: MockMvc

    private val registro = """
        {"nombreCompleto":"Vanessa Bances","correo":"N00366994@upn.pe",
         "sedeCodigo":"los_olivos","clave":"secreta1"}
    """.trimIndent()

    @Test
    @Order(1)
    fun `registra la cuenta y asigna el rol de alumno`() {
        mvc.post("/api/auth/registro") {
            contentType = MediaType.APPLICATION_JSON
            content = registro
        }.andExpect {
            status { isCreated() }
            jsonPath("$.codigo") { value("N00366994") }
            jsonPath("$.rol") { value("ALUMNO") }
            jsonPath("$.universidad") { value("UPN") }
        }
    }

    @Test
    @Order(2)
    fun `no permite dos cuentas con el mismo correo`() {
        mvc.post("/api/auth/registro") {
            contentType = MediaType.APPLICATION_JSON
            content = registro
        }.andExpect { status { isConflict() } }
    }

    @Test
    @Order(3)
    fun `inicia sesion solo con la contrasena correcta`() {
        mvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"correo":"n00366994@upn.pe","clave":"secreta1"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.nombreCompleto") { value("Vanessa Bances") }
        }

        mvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"correo":"n00366994@upn.pe","clave":"otra-clave"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    @Order(4)
    fun `rechaza correos que no son institucionales`() {
        mvc.post("/api/auth/registro") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nombreCompleto":"Ana","correo":"ana@gmail.com","sedeCodigo":"brena","clave":"secreta1"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    @Order(5)
    fun `lista las cuatro universidades de Lima`() {
        mvc.get("/api/universidades").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(4) }
        }
    }
}
