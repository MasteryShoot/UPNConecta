package pe.upn.conecta.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

/** Datos que muestra la página /swagger-ui.html. */
@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "UPN Conecta API",
        version = "1.0",
        description = "Backend de la app UPN Conecta: Spring Boot + PostgreSQL, publicado en Render."
    )
)
@SecurityScheme(name = "sesion", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class DocumentacionApi
