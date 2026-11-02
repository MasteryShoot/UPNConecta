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
            // Registrarse no prueba que el correo sea tuyo: el token solo llega al iniciar sesión
            jsonPath("$.token") { doesNotExist() }
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
            jsonPath("$.token") { isNotEmpty() }
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

    @Test
    @Order(6)
    fun `valida el ingreso por dominio`() {
        mvc.get("/api/auth/dominio?correo=N00329320@upn.pe").andExpect {
            status { isOk() }
            jsonPath("$.universidad") { value("UPN") }
            jsonPath("$.rol") { value("ALUMNO") }
        }
        // La UPN también usa este dominio (por ejemplo, el correo del docente del curso)
        mvc.get("/api/auth/dominio?correo=gerardo.sarmiento@upn.edu.pe").andExpect {
            status { isOk() }
            jsonPath("$.rol") { value("DOCENTE") }
        }
        mvc.get("/api/auth/dominio?correo=jose@gmail.com").andExpect { status { isBadRequest() } }
        mvc.get("/api/auth/dominio?correo=sin-arroba").andExpect { status { isBadRequest() } }
        // Registrada para la siguiente fase, pero todavía sin sedes en la app
        mvc.get("/api/auth/dominio?correo=u202012345@upc.edu.pe").andExpect { status { isForbidden() } }
    }

    @Test
    @Order(7)
    fun `acepta un codigo de docente largo`() {
        mvc.post("/api/auth/registro") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"nombreCompleto":"Docente Largo","correo":"nombre.segundo.apellido.materno@upn.edu.pe",
                "sedeCodigo":"brena","clave":"secreta1"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.rol") { value("DOCENTE") }
        }
    }

    @Test
    @Order(8)
    fun `muestra el estado del servicio y su documentacion`() {
        mvc.get("/").andExpect { status { isOk() } }
        mvc.get("/api/estado").andExpect {
            status { isOk() }
            jsonPath("$.estado") { value("UP") }
            jsonPath("$.sedes") { value(8) }
        }
        mvc.get("/v3/api-docs").andExpect { status { isOk() } }
    }

    @Test
    @Order(9)
    fun `una ruta solo POST abierta en el navegador responde 405`() {
        mvc.get("/api/auth/login").andExpect { status { isMethodNotAllowed() } }
    }
}
