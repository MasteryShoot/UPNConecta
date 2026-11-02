package com.example.upnconecta.data.remote

import android.net.Uri
import android.util.Base64
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Inicio de sesión con la cuenta institucional de Microsoft 365 (OAuth 2.0 + PKCE).
 *
 * 1. Se abre el navegador con la página de Microsoft (la contraseña nunca pasa por la app).
 * 2. Microsoft regresa a upnconecta://auth con un código temporal.
 * 3. La app cambia ese código por un token y se lo envía al backend.
 *
 * Se usa el tenant "organizations": acepta cualquier cuenta de trabajo o estudio,
 * así sirve para la UPN y para otras universidades que usen Microsoft 365.
 */
object MicrosoftAuth {

    // Identifica a la app ante Microsoft, igual que el nombre de usuario identifica a una persona.
    // No es una clave secreta: una app móvil es un "cliente público", no tiene secreto de cliente
    // y se protege con PKCE y con la URI de redirección registrada en Azure.
    private const val CLIENT_ID = "26c34a39-4a53-4350-98f8-9cf9d7faf639"
    const val REDIRECT_URI = "upnconecta://auth"
    private const val AUTORIDAD = "https://login.microsoftonline.com/organizations/oauth2/v2.0"
    private const val ALCANCE = "openid profile email User.Read"

    // El verificador PKCE se guarda mientras el usuario está en el navegador
    private var verificador: String? = null
    private var estadoEsperado: String? = null

    /** El correo ya validado por dominio se envía para que Microsoft no lo vuelva a pedir. */
    fun urlInicio(correo: String): Uri {
        val nuevo = aleatorio()
        verificador = nuevo
        estadoEsperado = aleatorio()
        return Uri.parse("$AUTORIDAD/authorize").buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", ALCANCE)
            .appendQueryParameter("login_hint", correo)
            .appendQueryParameter("state", estadoEsperado)
            .appendQueryParameter("code_challenge", desafio(nuevo))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    /** Cambia el código que devolvió Microsoft por un access token. */
    suspend fun obtenerToken(codigo: String, estado: String?): Result<String> = withContext(Dispatchers.IO) {
        val pkce = verificador
        // El estado confirma que esta respuesta viene del inicio de sesión que abrió esta app
        if (pkce == null || estado != estadoEsperado) {
            return@withContext Result.failure(Exception("Vuelve a pulsar \"Continuar con Microsoft\""))
        }
        try {
            val cuerpo = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "authorization_code")
                .add("code", codigo)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", pkce)
                .add("scope", ALCANCE)
                .build()
            val pedido = Request.Builder().url("$AUTORIDAD/token").post(cuerpo).build()
            OkHttpClient().newCall(pedido).execute().use { respuesta ->
                val json = JsonParser.parseString(respuesta.body?.string() ?: "{}").asJsonObject
                val token = json.get("access_token")?.asString
                if (respuesta.isSuccessful && token != null) Result.success(token)
                else Result.failure(Exception(json.get("error_description")?.asString?.substringBefore("\r")
                    ?: "Microsoft rechazó el inicio de sesión"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo conectar con Microsoft. Revisa tu conexión."))
        } finally {
            verificador = null
            estadoEsperado = null
        }
    }

    private fun aleatorio(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return base64Url(bytes)
    }

    private fun desafio(verificador: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verificador.toByteArray()))

    private fun base64Url(bytes: ByteArray) =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
