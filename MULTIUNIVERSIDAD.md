# Plan: UPN Conecta para varias universidades de Lima

Recomendación del docente en la T2. Lo dejamos planificado para la T3 (examen final).

## Idea

Hoy la app solo funciona para la UPN. La propuesta es que cualquier alumno o docente de una
universidad de Lima que use **Microsoft 365** pueda entrar con su correo institucional, sin
crear otra contraseña, y califique las sedes, áreas y docentes **de su propia universidad**.

Es gratis y legal porque:

- La app está registrada en **Microsoft Entra ID** como *multiinquilino* (cuenta gratuita de Azure).
- Solo pedimos el permiso `User.Read` (nombre y correo). No usamos ninguna API privada ni el CRM
  de ninguna universidad.
- Cada universidad puede aprobar la app para todos sus usuarios (consentimiento del administrador)
  si su política no deja que el alumno lo haga solo.

## Universidades elegidas (todas en Lima y con correo en Microsoft 365)

| Siglas | Universidad | Dominio de correo | Color de acento |
|---|---|---|---|
| UPN | Universidad Privada del Norte | `@upn.pe` | `#FFD100` (amarillo) |
| UPC | Universidad Peruana de Ciencias Aplicadas | `@upc.edu.pe` | `#E30613` (rojo) |
| UTP | Universidad Tecnológica del Perú | `@utp.edu.pe` | `#C8102E` (rojo oscuro) |
| ULIMA | Universidad de Lima | `@aloe.ulima.edu.pe` | `#F47920` (naranja) |

> Antes de la T3 hay que confirmar con un alumno de cada universidad que su correo entra por
> Microsoft (si alguna usa Google Workspace, se cambia por otra universidad).

## Lo que ya está hecho (T2)

- [x] Tabla `UNIVERSIDAD` con `dominio_correo` y migración `V3` con las 4 universidades.
- [x] `GET /api/universidades`.
- [x] El backend identifica la universidad por el dominio del correo (`AuthService.universidadDe`).
- [x] Login con Microsoft usando el tenant `organizations` (acepta cualquier cuenta de trabajo o estudio).
- [x] App registrada en Entra ID como multiinquilino.

## Lo que falta

### Backend
1. Migración `V4`: sedes de UPC, UTP y ULIMA (código, nombre, dirección, latitud y longitud).
2. Agregar `latitud` y `longitud` a `SEDE` para que el mapa no dependa de datos fijos en la app.
3. `GET /api/sedes?universidad=UPC` en vez de devolver todas.
4. Validar en `ResenaService` que un usuario solo califique sedes de **su** universidad.
5. Tabla `DOCENTE` con `id_universidad` (hoy los docentes están en `DatosDemo.kt`).
6. Regla de rol por universidad: en la UPN el código `N00…` es alumno; en las demás se puede
   usar el campo `jobTitle` de Microsoft Graph o que el usuario lo elija al registrarse.

### App
1. Nombre neutral: **Campus Conecta** (applicationId nuevo, el logo del escudo se mantiene).
2. Quitar `@upn.pe` fijo de `Modelos.kt`, `LoginActivity` y `RegistroActivity`; validar contra la
   lista que devuelve `/api/universidades`.
3. Cargar sedes y docentes desde la API (dejar `DatosDemo` solo como respaldo sin conexión, en SQLite).
4. Tema dinámico: guardar en SharedPreferences la universidad del usuario y usar su color de acento
   en `UPNConectaTheme` (el negro `#16161A` se mantiene como base).
5. Imágenes de sedes: descargarlas por URL (Coil) en lugar de tenerlas en `drawable-nodpi`.

### Documentos
- Actualizar alcance, requisitos y product backlog (nuevas historias: HU1.04 "Elegir universidad",
  HU2.05 "Ver solo sedes de mi universidad").
- Riesgo: que una universidad bloquee el consentimiento de usuario. Mitigación: se mantiene el
  registro con correo y contraseña por la API.

## Estimación

Un sprint de dos semanas para el equipo de 5: backend (2 personas), app (2), documentación y
pruebas (1).
