package com.example.upnconecta.usuario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.R
import com.example.upnconecta.data.Ajustes
import com.example.upnconecta.data.CuentaRepositorio
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.remote.MicrosoftAuth
import com.example.upnconecta.data.remote.UsuarioRespuesta
import com.example.upnconecta.sede.ElegirSedeActivity
import com.example.upnconecta.ui.componentes.AvisoServidorLento
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.abrir
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.pantallaConecta
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    // Respuesta de Microsoft: upnconecta://auth?code=...
    private var respuestaMicrosoft by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        respuestaMicrosoft = intent?.data
        pantallaConecta(barraOscura = true) {
            PantallaLogin(respuestaMicrosoft) { respuestaMicrosoft = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        respuestaMicrosoft = intent.data
    }
}

@Composable
fun PantallaLogin(respuestaMicrosoft: Uri? = null, alProcesar: () -> Unit = {}) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val repositorio = remember { CuentaRepositorio() }
    val alcance = rememberCoroutineScope()
    var cargando by remember { mutableStateOf(false) }

    var correo by remember { mutableStateOf(preferencias.ultimoCorreo()) }
    var contrasena by remember { mutableStateOf("") }
    var recordar by remember { mutableStateOf(preferencias.ultimoCorreo().isNotEmpty()) }
    var verContrasena by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarAyuda by remember { mutableStateOf(false) }
    // El camino normal es Microsoft; la contraseña solo aparece si la persona la pide
    var conContrasena by remember { mutableStateOf(false) }

    // La API ya validó la cuenta: se guarda la copia local y se abre Elegir sede
    fun entrar(cuenta: UsuarioRespuesta, metodo: String) {
        preferencias.guardarCuenta(cuenta.nombreCompleto, cuenta.correo, cuenta.sedeCodigo)
        preferencias.iniciarSesion(cuenta.correo, recordar, cuenta.token)
        Ajustes(context, cuenta.correo).metodoIngreso = metodo
        Toast.makeText(context, "Bienvenido(a), ${cuenta.nombreCompleto.substringBefore(" ")}", Toast.LENGTH_SHORT).show()
        context.abrir(ElegirSedeActivity::class.java) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    fun iniciarSesion() {
        val correoLimpio = correo.trim()
        error = when {
            correoLimpio.isEmpty() || contrasena.isEmpty() -> "Complete todos los campos"
            else -> null
        }
        if (error != null || cargando) return
        cargando = true
        alcance.launch {
            repositorio.iniciarSesion(correoLimpio, contrasena).fold(
                onSuccess = { entrar(it, Ajustes.METODO_CORREO) },
                onFailure = { error = it.message }
            )
            cargando = false
        }
    }

    // No hay registro: el backend valida el dominio del correo y Microsoft confirma la identidad.
    // La cuenta se crea sola la primera vez.
    fun continuarConMicrosoft() {
        val correoLimpio = correo.trim()
        if (correoLimpio.isEmpty()) {
            error = "Escribe tu correo institucional"
            return
        }
        if (cargando) return
        error = null
        cargando = true
        alcance.launch {
            repositorio.validarDominio(correoLimpio).fold(
                onSuccess = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, MicrosoftAuth.urlInicio(correoLimpio)))
                },
                onFailure = { error = it.message }
            )
            cargando = false
        }
    }

    // Regreso desde la página de Microsoft
    LaunchedEffect(respuestaMicrosoft) {
        val uri = respuestaMicrosoft ?: return@LaunchedEffect
        alProcesar()
        val codigo = uri.getQueryParameter("code")
        if (codigo == null) {
            error = uri.getQueryParameter("error_description") ?: "Se canceló el inicio con Microsoft"
            return@LaunchedEffect
        }
        cargando = true
        repositorio.iniciarConMicrosoft(codigo, uri.getQueryParameter("state")).fold(
            onSuccess = { entrar(it, Ajustes.METODO_MICROSOFT) },
            onFailure = { error = it.message }
        )
        cargando = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Cabecera negra con el logo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Negro)
                .statusBarsPadding()
                .padding(top = 36.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_escudo),
                contentDescription = "Logo UPN Conecta",
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("UPN Conecta", color = Amarillo, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Ingresa con tus credenciales institucionales", color = GrisClaro, fontSize = 13.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            EtiquetaCampo("CORREO INSTITUCIONAL")
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it; error = null },
                placeholder = { Text("Ej. N00123456@upn.pe") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )

            if (conContrasena) {
                Spacer(Modifier.height(16.dp))

                EtiquetaCampo("CONTRASEÑA")
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it; error = null },
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    visualTransformation = if (verContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { verContrasena = !verContrasena }) {
                            Icon(
                                if (verContrasena) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Mostrar contraseña"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = coloresCampo(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.clickable { recordar = !recordar },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordar,
                        onCheckedChange = { recordar = it },
                        colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Negro)
                    )
                    Text("Recordarme", fontSize = 13.sp, color = GrisTexto)
                }
                if (conContrasena) {
                    TextButton(onClick = { mostrarAyuda = true }) {
                        Text("¿Olvidaste tu contraseña?", color = Negro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = Rojo, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
            if (cargando) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Amarillo, modifier = Modifier.size(28.dp))
                }
                AvisoServidorLento()
            } else if (conContrasena) {
                BotonAmarillo("Iniciar sesión", onClick = { iniciarSesion() })
            } else {
                // Ingreso principal con la cuenta institucional de Microsoft 365
                Button(
                    onClick = { continuarConMicrosoft() },
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Negro, contentColor = Blanco),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    LogoMicrosoft()
                    Spacer(Modifier.width(10.dp))
                    Text("Continuar con Microsoft", fontWeight = FontWeight.Bold)
                }
                Text(
                    "No necesitas crear una cuenta: validamos el dominio de tu correo e ingresas con " +
                        "la misma contraseña de Outlook y Teams.",
                    color = GrisTexto, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(onClick = { conContrasena = !conContrasena; error = null }) {
                    Text(
                        if (conContrasena) "Volver a ingresar con Microsoft" else "¿No puedes usar Microsoft? Usa una contraseña",
                        color = Negro, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
                if (conContrasena) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("¿Aún no tienes contraseña?", color = GrisTexto, fontSize = 13.sp)
                        TextButton(onClick = { context.abrir(RegistroActivity::class.java) }) {
                            Text("Crear cuenta", color = Negro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (mostrarAyuda) {
        AlertDialog(
            onDismissRequest = { mostrarAyuda = false },
            confirmButton = {
                TextButton(onClick = { mostrarAyuda = false }) { Text("Entendido", color = Negro) }
            },
            title = { Text("Recuperar contraseña") },
            text = {
                Text(
                    "Por ahora la recuperación se realiza con Soporte Técnico de tu sede. " +
                        "También puedes ingresar con Microsoft, sin contraseña de la app.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            containerColor = Blanco
        )
    }
}

@Composable
fun EtiquetaCampo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelSmall,
        color = Negro,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Amarillo,
    unfocusedBorderColor = GrisClaro,
    focusedContainerColor = Blanco,
    unfocusedContainerColor = Blanco,
    cursorColor = Negro
)

/** Los cuatro cuadros de colores del logo de Microsoft. */
@Composable
private fun LogoMicrosoft() {
    val colores = listOf(Color(0xFFF25022), Color(0xFF7FBA00), Color(0xFF00A4EF), Color(0xFFFFB900))
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        colores.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                fila.forEach { Spacer(Modifier.size(8.dp).background(it)) }
            }
        }
    }
}
