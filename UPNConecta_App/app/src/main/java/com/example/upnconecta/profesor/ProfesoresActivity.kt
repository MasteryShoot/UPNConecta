package com.example.upnconecta.profesor

import android.os.Bundle
import androidx.compose.runtime.key
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Docente
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Roles
import com.example.upnconecta.data.sinTildes
import com.example.upnconecta.ui.componentes.Avatar
import com.example.upnconecta.ui.componentes.CabeceraSimple
import com.example.upnconecta.ui.componentes.ConectaActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.componentes.abrir
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Verde
import com.example.upnconecta.ui.theme.VerdeSuave
import com.example.upnconecta.ui.theme.pantallaConecta

class ProfesoresActivity : ConectaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // HU1.02: un docente no puede ingresar al módulo de calificación de docentes
        if (Preferencias(this).usuarioActual()?.rol != Roles.ALUMNO) {
            Toast.makeText(this, "No tiene permisos para acceder a este módulo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pantallaConecta {
            // key(): al volver a la pantalla se vuelve a leer SharedPreferences
            key(refresco.intValue) {
                PantallaProfesores(refresco.intValue)
            }
        }
    }
}

@Composable
fun PantallaProfesores(refresco: Int) {
    val context = LocalContext.current
    val foco = LocalFocusManager.current
    val preferencias = remember { Preferencias(context) }
    var busqueda by remember { mutableStateOf("") }

    // HU3.01: búsqueda por nombre/apellido o por asignatura
    val texto = busqueda.sinTildes()
    val filtrados = DatosDemo.docentes.filter {
        it.nombre.sinTildes().contains(texto) || it.curso.sinTildes().contains(texto)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        CabeceraSimple("Califica a tus docentes", "Ciclo ${DatosDemo.CICLO_ACTUAL}")

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar por nombre o asignatura") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { foco.clearFocus() }),
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Amarillo, unfocusedBorderColor = GrisClaro,
                focusedContainerColor = Blanco, unfocusedContainerColor = Blanco
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (filtrados.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔍", fontSize = 40.sp)
                Text(
                    "No se encontraron docentes con ese nombre o asignatura",
                    color = GrisTexto, fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtrados) { docente ->
                    FilaDocente(docente, preferencias.yaEvaluoDocente(docente.codigo)) {
                        context.abrir(EvaluacionProfesorActivity::class.java) {
                            putExtra(EvaluacionProfesorActivity.EXTRA_DOCENTE, docente.codigo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDocente(docente: Docente, evaluado: Boolean, onClick: () -> Unit) {
    TarjetaBlanca(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(docente.nombre, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(docente.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(docente.curso, fontSize = 13.sp)
                Text("${docente.facultad} · UPN ${DatosDemo.sede(docente.sedeId).nombre}", color = GrisTexto, fontSize = 12.sp)
            }
            if (evaluado) {
                // HU3.02: la ficha cambia a estado "Evaluado"
                Etiqueta("Evaluado", fondo = VerdeSuave, color = Verde)
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GrisTexto)
            }
        }
    }
}
