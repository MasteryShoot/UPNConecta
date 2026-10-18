package com.example.upnconecta.usuario

import android.content.Intent
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.R
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Roles
import com.example.upnconecta.sede.ElegirSedeActivity
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

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaConecta(barraOscura = true) {
            PantallaLogin()
        }
    }
}

@Composable
fun PantallaLogin() {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }

    var correo by remember { mutableStateOf(preferencias.ultimoCorreo()) }
    var contrasena by remember { mutableStateOf("") }
    var recordar by remember { mutableStateOf(preferencias.ultimoCorreo().isNotEmpty()) }
    var verContrasena by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mostrarAyuda by remember { mutableStateOf(false) }

    fun iniciarSesion() {
        val correoLimpio = correo.trim()
        error = when {
            correoLimpio.isEmpty() || contrasena.isEmpty() -> "Complete todos los campos"
            // HU1.01 - Escenario: correo no institucional
            Roles.rolDeCorreo(correoLimpio) == null -> "Debe usar su correo institucional para ingresar"
            !preferencias.existeUsuario(correoLimpio) -> "Esta cuenta no está registrada. Crea una cuenta primero."
            !preferencias.credencialesValidas(correoLimpio, contrasena) -> "Correo o contraseña incorrectos"
            else -> null
        }
        if (error == null) {
            preferencias.iniciarSesion(correoLimpio, recordar)
            val usuario = preferencias.usuarioActual()
            Toast.makeText(context, "Bienvenido(a), ${usuario?.primerNombre}", Toast.LENGTH_SHORT).show()
            // Login exitoso → Elegir sede (según el diagrama de ventanas)
            context.abrir(ElegirSedeActivity::class.java) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
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
                TextButton(onClick = { mostrarAyuda = true }) {
                    Text("¿Olvidaste tu contraseña?", color = Negro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (error != null) {
                Text(error!!, color = Rojo, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(24.dp))
            BotonAmarillo("Iniciar sesión", onClick = { iniciarSesion() })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Nuevo en UPN Conecta?", color = GrisTexto, fontSize = 13.sp)
                TextButton(onClick = { context.abrir(RegistroActivity::class.java) }) {
                    Text("Crear cuenta", color = Negro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        "En la siguiente versión se enviará un enlace a tu correo @upn.pe.",
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
