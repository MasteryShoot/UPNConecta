package com.example.upnconecta.usuario

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Roles
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.pantallaConecta

class RegistroActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaConecta {
            PantallaRegistro()
        }
    }
}

@Composable
fun PantallaRegistro() {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var sedeId by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var aceptaTerminos by remember { mutableStateOf(false) }
    var menuSedes by remember { mutableStateOf(false) }
    var verTerminos by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val rolDetectado = Roles.rolDeCorreo(correo)

    fun registrar() {
        error = when {
            nombre.isBlank() || correo.isBlank() || sedeId.isEmpty() ||
                contrasena.isEmpty() || confirmar.isEmpty() -> "Complete todos los campos"
            rolDetectado == null -> "Ingrese un correo institucional válido (@upn.pe)"
            preferencias.existeUsuario(correo) -> "Ya existe una cuenta con este correo"
            contrasena.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            contrasena != confirmar -> "Las contraseñas no coinciden"
            !aceptaTerminos -> "Debe aceptar los términos y condiciones"
            else -> null
        }
        if (error == null) {
            preferencias.registrarUsuario(nombre, correo, sedeId, contrasena)
            Toast.makeText(context, "Cuenta creada correctamente. Inicia sesión.", Toast.LENGTH_SHORT).show()
            // Regresa al Login (diagrama: Registro ↔ Login)
            (context as? Activity)?.finish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineMedium)
        Text("Regístrate para calificar tu campus", color = GrisTexto, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        EtiquetaCampo("NOMBRE COMPLETO")
        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            placeholder = { Text("Ej. Juan Pérez") }, singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        EtiquetaCampo("CORREO INSTITUCIONAL")
        OutlinedTextField(
            value = correo, onValueChange = { correo = it.trim() },
            placeholder = { Text("Ej. N00123456@upn.pe") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp), colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        // RN02: se muestra el rol que se asignará según el correo
        if (correo.contains("@")) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (rolDetectado != null) {
                    Text("Te registrarás como ", fontSize = 12.sp, color = GrisTexto)
                    Etiqueta(rolDetectado, fondo = AmarilloSuave)
                } else {
                    Text("Solo se aceptan correos @upn.pe", fontSize = 12.sp, color = Rojo)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        EtiquetaCampo("SEDE (CAMPUS)")
        Box {
            OutlinedTextField(
                value = if (sedeId.isEmpty()) "" else "UPN " + DatosDemo.sede(sedeId).nombre,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Selecciona tu sede") },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                shape = RoundedCornerShape(12.dp), colors = coloresCampo(),
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable { menuSedes = true }
            )
            DropdownMenu(
                expanded = menuSedes,
                onDismissRequest = { menuSedes = false },
                containerColor = Blanco
            ) {
                DatosDemo.sedes.forEach { sede ->
                    DropdownMenuItem(
                        text = { Text("UPN ${sede.nombre} · ${sede.ciudad}") },
                        onClick = { sedeId = sede.id; menuSedes = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        EtiquetaCampo("CONTRASEÑA")
        OutlinedTextField(
            value = contrasena, onValueChange = { contrasena = it },
            placeholder = { Text("Mínimo 6 caracteres") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp), colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))

        EtiquetaCampo("CONFIRMAR CONTRASEÑA")
        OutlinedTextField(
            value = confirmar, onValueChange = { confirmar = it },
            placeholder = { Text("••••••••") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp), colors = coloresCampo(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Checkbox(
                checked = aceptaTerminos,
                onCheckedChange = { aceptaTerminos = it },
                colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Negro)
            )
            Text(
                "Acepto los ", fontSize = 13.sp, color = GrisTexto,
                modifier = Modifier.clickable { aceptaTerminos = !aceptaTerminos }
            )
            Text(
                "términos y condiciones",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Negro,
                modifier = Modifier.clickable { verTerminos = true }
            )
        }

        if (error != null) {
            Text(error!!, color = Rojo, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(20.dp))
        BotonAmarillo("Registrarse", onClick = { registrar() })

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿Ya tienes cuenta?", color = GrisTexto, fontSize = 13.sp)
            TextButton(onClick = { (context as? Activity)?.finish() }) {
                Text("Iniciar sesión", color = Negro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (verTerminos) {
        DialogoTerminos(
            onAceptar = { aceptaTerminos = true; verTerminos = false },
            onCerrar = { verTerminos = false }
        )
    }
}

/** Términos y condiciones definidos en la PC 03. */
@Composable
fun DialogoTerminos(onAceptar: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Blanco,
        title = { Text("Términos y condiciones") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val puntos = listOf(
                    "Usarás UPN Conecta de forma respetuosa y responsable. Tus evaluaciones deben basarse en experiencias reales dentro de la universidad.",
                    "No se permite contenido ofensivo, información falsa ni datos personales de terceros. Las publicaciones que incumplan estas reglas podrán ser ocultadas o eliminadas.",
                    "Puedes comentar de forma anónima; tu identidad no será visible, pero quedará registrada internamente para fines de moderación.",
                    "No intentarás acceder a módulos restringidos, usar cuentas ajenas ni manipular los resultados.",
                    "Tus datos solo se usan para los fines de la app (Ley N.° 29733 de Protección de Datos Personales). No se usarán para publicidad sin tu consentimiento.",
                    "Si incumples las normas de forma repetida, tu cuenta podrá ser suspendida hasta por 7 días."
                )
                puntos.forEachIndexed { i, punto ->
                    Text("${i + 1}. $punto", fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAceptar) { Text("Aceptar", color = Negro, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cerrar", color = GrisTexto) }
        }
    )
}
