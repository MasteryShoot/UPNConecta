# UPN Conecta

App Android donde alumnos y docentes califican las áreas de su sede. Curso Desarrollo de Aplicaciones Móviles, UPN, Grupo 8.

## Qué es cada cosa

| Pieza | Tecnología | Dónde corre | Carpeta |
|---|---|---|---|
| App | Kotlin + Jetpack Compose | El celular o el emulador | `UPNConecta_T1/` |
| Backend (API REST) | Spring Boot 3.5, Java 21, Gradle | **Render, en la nube**. No corre en la PC. | `UPNConecta_Backend/` |
| Base de datos central | PostgreSQL 18 | **Render** | Tablas en `UPNConecta_Backend/src/main/resources/db/migration/` |
| Datos locales | SQLite (historial) y SharedPreferences (ajustes y sesión) | El celular | `UPNConecta_T1/.../data/` |
| Identidad | Microsoft Entra ID | Microsoft | `.../data/remote/MicrosoftAuth.kt` |
| Mapas | Google Maps | Google | `.../explorar/ExplorarActivity.kt` |
| Código fuente | Git | GitHub: [MasteryShoot/UPNConecta](https://github.com/MasteryShoot/UPNConecta) | Este repositorio |

Cada `git push` a `main` hace que Render vuelva a construir y publicar el backend. Por eso no hay que levantar nada en la PC para usar la app.

El backend no es un proyecto Node: **no usa `npm`** y no está en Vercel.

## Ver el backend funcionando

No hace falta instalar nada, basta un navegador:

- Estado y conexión a la base: <https://upnconecta-backend.onrender.com/api/estado>
- Documentación interactiva: <https://upnconecta-backend.onrender.com/swagger-ui.html>
- Dato real de la base: <https://upnconecta-backend.onrender.com/api/sedes>

El plan gratuito de Render apaga el servicio tras 15 minutos sin uso. La primera visita puede tardar más de un minuto en responder; después responde normal.

Contrato de la API: [`docs/API_CONTRATO.md`](docs/API_CONTRATO.md). Colección para Postman: [`docs/UPNConecta.postman_collection.json`](docs/UPNConecta.postman_collection.json).

## Cómo se ingresa

1. La persona escribe su correo institucional y no llena ningún formulario de registro.
2. El backend valida el dominio (`GET /api/auth/dominio`). Hoy están habilitados `@upn.pe` y `@upn.edu.pe`.
3. Se abre la página oficial de Microsoft (OAuth 2.0 con PKCE). La contraseña solo la ve Microsoft.
4. El backend confirma la identidad con Microsoft Graph, crea la cuenta si es la primera vez y entrega un token de sesión (JWT).
5. La app envía ese token en cada operación privada. El autor de una reseña sale del token, no de lo que envía la app.

Correo y contraseña (`/api/auth/registro` y `/api/auth/login`) quedan como alternativa para quien no pueda usar Microsoft. Esa vía no comprueba que el correo sea de quien lo escribe.

## Compilar

**App:** abrir `UPNConecta_T1/` en Android Studio y ejecutar. Para ver el mapa, copiar `local.properties.example` como `local.properties` y poner la clave de Google Maps. Sin esa clave todo funciona, pero el mapa sale en blanco.

**Backend en la PC (opcional, solo para desarrollar):** necesita PostgreSQL y la variable `JWT_SECRET` (mínimo 32 caracteres).

```bash
cd UPNConecta_Backend
./gradlew test
```

Las pruebas usan una base H2 en memoria, no necesitan PostgreSQL.

## Dónde está cada dato sensible

| Dato | Dónde está | ¿Es secreto? |
|---|---|---|
| Client ID de Microsoft | `MicrosoftAuth.kt`, en el repositorio | No. Identifica a la app. Una app móvil es un cliente público: no tiene secreto de cliente y se protege con PKCE y la URI de redirección. |
| Clave de Google Maps | `local.properties`, fuera del repositorio | Va dentro del APK de todas formas. Se protege restringiéndola en Google Cloud al paquete y SHA-1 de la app. |
| `JWT_SECRET` y credenciales de la base | Variables de entorno en Render | **Sí.** Nunca se suben a Git. |
| Contraseñas de usuarios | PostgreSQL, cifradas con BCrypt | **Sí.** Solo existe el hash. |
