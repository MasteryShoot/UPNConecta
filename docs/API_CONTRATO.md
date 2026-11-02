# Contrato de la API

URL base: `https://upnconecta-backend.onrender.com/api`

Todo viaja en JSON. Los errores siempre tienen la misma forma:

```json
{ "mensaje": "Texto que la app muestra al usuario" }
```

Las rutas marcadas con 🔒 exigen el encabezado `Authorization: Bearer <token>`.

## Endpoints

| Método | Ruta | Para qué | Respuestas |
|---|---|---|---|
| GET | `/estado` | Comprobar la API y la base | 200 |
| GET | `/auth/dominio?correo=` | Validar el dominio del correo | 200, 400 no institucional, 403 universidad no habilitada |
| POST | `/auth/microsoft` | Ingreso principal, crea la cuenta la primera vez | 200, 400, 401 |
| POST | `/auth/login` | Ingreso alternativo con contraseña | 200, 400, 401, 404 |
| POST | `/auth/registro` | Crear cuenta con contraseña | 201, 400, 404, 409 |
| PUT 🔒 | `/usuarios/me/sede` | Guardar la sede de la cuenta | 200, 401, 404 |
| GET | `/universidades` | Universidades registradas | 200 |
| GET | `/sedes` | Sedes | 200 |
| GET | `/criterios?area=` | Criterios de un área | 200 |
| GET | `/resenas?sede=&area=` | Opiniones públicas, sin datos personales | 200 |
| POST 🔒 | `/resenas` | **Create:** registrar mi evaluación | 201, 400, 401, 404, 409 ya evaluó en el ciclo |
| GET 🔒 | `/resenas/mia?sede=&area=` | **Read:** mi evaluación | 200, 401, 404 |
| PUT 🔒 | `/resenas/{id}` | **Update:** editar mi evaluación | 200, 400, 401, 403, 404, 409 |
| DELETE 🔒 | `/resenas/{id}` | **Delete:** eliminar mi evaluación | 200, 401, 403, 404 |

Valores de `area`: `infraestructura`, `limpieza`, `gestion`, `sostenibilidad`.

## Ejemplos

### Validar dominio

`GET /auth/dominio?correo=N00329320@upn.pe`

```json
{ "correo": "n00329320@upn.pe", "dominio": "@upn.pe", "universidad": "UPN", "rol": "ALUMNO" }
```

### Iniciar sesión

`POST /auth/login`

```json
{ "correo": "n00329320@upn.pe", "clave": "mi-clave" }
```

`POST /auth/microsoft` recibe `{ "accessToken": "<token de Microsoft>" }`. Ambas responden:

```json
{
  "codigo": "N00329320",
  "correo": "n00329320@upn.pe",
  "nombreCompleto": "Jose Pariona",
  "rol": "ALUMNO",
  "sedeCodigo": "los_olivos",
  "universidad": "UPN",
  "proveedor": "CORREO",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Registrar una evaluación

`POST /resenas` 🔒. El autor no se envía: sale del token.

```json
{
  "codigoSede": "los_olivos",
  "area": "limpieza",
  "experiencia": 4,
  "comentario": null,
  "esAnonima": false,
  "sinReciclaje": false,
  "calificaciones": [
    { "idCriterio": 4, "puntaje": 4 },
    { "idCriterio": 5, "puntaje": 3 },
    { "idCriterio": 6, "puntaje": 5 }
  ]
}
```

Respuesta 201:

```json
{
  "idResena": 12,
  "codigoUsuario": "N00329320",
  "codigoSede": "los_olivos",
  "area": "limpieza",
  "periodo": "2026-2",
  "experiencia": 4,
  "comentario": null,
  "esAnonima": false,
  "sinReciclaje": false,
  "fecha": "2026-11-02T15:30:00",
  "promedio": 4.0,
  "calificaciones": [
    { "idCriterio": 4, "nombre": "Baños", "puntaje": 4 },
    { "idCriterio": 5, "nombre": "Áreas comunes", "puntaje": 3 },
    { "idCriterio": 6, "nombre": "Cafeterías", "puntaje": 5 }
  ]
}
```

## Cómo lo consume la app

| Archivo | Papel |
|---|---|
| `data/remote/ApiService.kt` | Interfaz de Retrofit: una función por endpoint |
| `data/remote/ModelosApi.kt` | Data classes que Gson convierte a y desde JSON |
| `data/remote/RetrofitClient.kt` | Instancia única, agrega el token y maneja la sesión vencida |
| `data/remote/LlamadaApi.kt` | Convierte cualquier falla de red o HTTP en un mensaje para el usuario |
| `data/remote/Conectividad.kt` | Detecta si el teléfono tiene internet |
| `data/ResenaRepositorio.kt` | Repositorio: lo único que el ViewModel conoce de la red |
| `sede/CalificarAreaViewModel.kt` | Estados de la pantalla: Cargando, Listo y Error |
