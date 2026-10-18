package com.example.upnconecta.sede

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Moderacion
import com.example.upnconecta.data.Opinion
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.CabeceraSimple
import com.example.upnconecta.ui.componentes.iconoArea
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.pantallaConecta

class ComentariosSedeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sedeId = intent.getStringExtra(DetalleAreaActivity.EXTRA_SEDE) ?: DatosDemo.sedes.first().id
        val areaId = intent.getStringExtra(DetalleAreaActivity.EXTRA_AREA) ?: DatosDemo.areas.first().id

        pantallaConecta {
            PantallaComentariosSede(sedeId, areaId)
        }
    }
}

@Composable
fun PantallaComentariosSede(sedeId: String, areaInicial: String) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val sede = DatosDemo.sede(sedeId)

    var areaSeleccionada by remember { mutableStateOf(areaInicial) }
    var comentario by remember { mutableStateOf("") }
    var anonimo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val yaComento = preferencias.yaComentoArea(sedeId, areaSeleccionada)

    fun publicar() {
        error = if (yaComento) {
            "Ya publicaste una opinión sobre esta área en el semestre"
        } else {
            Moderacion.validarComentario(comentario, obligatorio = true)
        }
        if (error != null) return

        val usuario = preferencias.usuarioActual()
        preferencias.publicarComentarioArea(
            sedeId, areaSeleccionada,
            Opinion(
                // RN07: si es anónimo se oculta el nombre, pero la cuenta queda registrada
                autor = if (anonimo) "Anónimo" else usuario?.nombre ?: "Usuario",
                rol = usuario?.rol ?: "",
                texto = comentario.trim(),
                puntaje = 0,
                etiqueta = "Sugerencia",
                fecha = ""
            )
        )
        Toast.makeText(context, "Comentario enviado correctamente", Toast.LENGTH_SHORT).show()
        (context as? Activity)?.finish()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
            .imePadding()
    ) {
        CabeceraSimple("Comentarios y sugerencias", "UPN ${sede.nombre}")

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¿Sobre qué área quieres opinar?", fontWeight = FontWeight.Bold)
            // Chips de áreas (2 por fila)
            DatosDemo.areas.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fila.forEach { area ->
                        FilterChip(
                            selected = areaSeleccionada == area.id,
                            onClick = { areaSeleccionada = area.id; error = null },
                            label = { Text(area.nombre, fontSize = 13.sp) },
                            leadingIcon = { Icon(iconoArea(area.id), null, modifier = Modifier.size(18.dp)) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Blanco,
                                selectedContainerColor = Amarillo,
                                selectedLabelColor = Negro,
                                selectedLeadingIconColor = Negro
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = areaSeleccionada == area.id,
                                borderColor = GrisClaro, selectedBorderColor = Amarillo
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (yaComento) {
                Text(
                    "✅ Ya publicaste una opinión sobre esta área este semestre.",
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmarilloSuave)
                        .padding(12.dp)
                )
            }

            Text("TU OPINIÓN O PROPUESTA DE MEJORA", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = comentario,
                // HU2.05: el texto se corta en el carácter 500
                onValueChange = { comentario = it.take(Moderacion.MAX_CARACTERES); error = null },
                placeholder = { Text("Ej. Sería útil tener más tachos de reciclaje cerca de la cafetería...") },
                minLines = 5,
                enabled = !yaComento,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            ContadorCaracteres(comentario.length)

            Row(Modifier.clickable { anonimo = !anonimo }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = anonimo, onCheckedChange = { anonimo = it },
                    colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Negro)
                )
                Column {
                    Text("Mantener mi comentario anónimo", fontSize = 14.sp)
                    Text("Tu nombre no será visible para la comunidad", fontSize = 11.sp, color = GrisTexto)
                }
            }

            Text(
                "📌 Recuerda: sin lenguaje ofensivo ni datos personales. Las opiniones con 5 reportes se ocultan para revisión.",
                fontSize = 12.sp, color = GrisTexto
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Blanco)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (error != null) {
                Text(error!!, color = Rojo, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            BotonAmarillo("Enviar comentario", onClick = { publicar() }, habilitado = !yaComento)
        }
    }
}
