package com.example.upnconecta.sorteo

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Sorteo
import com.example.upnconecta.ui.componentes.BarraNavegacion
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.ConectaActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.Pestana
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Verde
import com.example.upnconecta.ui.theme.VerdeSuave
import com.example.upnconecta.ui.theme.pantallaConecta
import java.util.Calendar

class SorteosActivity : ConectaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaConecta(barraOscura = true) {
            // key(): al volver a la pantalla se vuelve a leer SharedPreferences
            key(refresco.intValue) {
                PantallaSorteos(refresco.intValue)
            }
        }
    }
}

/** Tiempo que falta para el cierre del ciclo (15 de diciembre). */
private fun tiempoRestante(): String {
    val ahora = Calendar.getInstance()
    val cierre = Calendar.getInstance().apply {
        set(ahora.get(Calendar.YEAR), Calendar.DECEMBER, 15, 23, 59, 0)
    }
    val minutos = ((cierre.timeInMillis - ahora.timeInMillis) / 60_000L).coerceAtLeast(0)
    val dias = minutos / (60 * 24)
    val horas = (minutos / 60) % 24
    return "${dias}d : ${horas}h : ${minutos % 60}m"
}

@Composable
fun PantallaSorteos(refresco: Int) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val usuario = preferencias.usuarioActual()
    val sedeId = usuario?.sedeId ?: DatosDemo.sedes.first().id
    val total = DatosDemo.areas.size
    val boletos = preferencias.areasEvaluadas(sedeId)

    Scaffold(
        containerColor = Fondo,
        bottomBar = { BarraNavegacion(Pestana.SORTEOS) }
    ) { espacio ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = espacio.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cabecera negra
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Negro)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Etiqueta("Próximo sorteo")
                    Spacer(Modifier.height(10.dp))
                    Text("¡Sorteo Fin de Ciclo!", color = Blanco, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = Amarillo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tiempo restante: ${tiempoRestante()}", color = GrisClaro, fontSize = 13.sp)
                    }
                }
            }

            // Progreso para participar
            item {
                TarjetaBlanca(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tus boletos del ciclo", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { boletos / total.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Amarillo,
                            trackColor = GrisClaro,
                            drawStopIndicator = {}
                        )
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Text(
                                if (boletos < total) "Evalúa ${total - boletos} área(s) más para ganar más boletos"
                                else "¡Tienes todos los boletos posibles! 🎉",
                                color = GrisTexto, fontSize = 12.sp, modifier = Modifier.weight(1f)
                            )
                            Text("🎟️ $boletos / $total", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Amarillo)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CardGiftcard, null, tint = Negro)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "¡Mientras más áreas de tu campus califiques, más opciones tienes de ganar!",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                    )
                }
            }

            // Nota ética (PC 03): el sorteo no depende de la nota que se ponga
            item {
                Text(
                    "⚖️ Tu participación no depende de la calificación que otorgues: " +
                        "cuenta igual si pones 1 o 5 estrellas.",
                    color = GrisTexto, fontSize = 12.sp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmarilloSuave)
                        .padding(12.dp)
                )
            }

            item {
                Text(
                    "Sorteos activos",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(DatosDemo.sorteos) { sorteo ->
                TarjetaSorteo(
                    sorteo = sorteo,
                    inscrito = preferencias.estaInscrito(sorteo.id),
                    onParticipar = {
                        if (boletos == 0) {
                            Toast.makeText(context, "Califica al menos un área de tu sede para participar", Toast.LENGTH_LONG).show()
                        } else {
                            preferencias.inscribirSorteo(sorteo.id)
                            Toast.makeText(context, "¡Te inscribiste en ${sorteo.titulo}!", Toast.LENGTH_SHORT).show()
                            // Se vuelve a dibujar la pantalla con el nuevo estado
                            (context as? ConectaActivity)?.refresco?.let { it.intValue++ }
                        }
                    }
                )
            }

            item {
                TarjetaBlanca(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Últimos ganadores de la sede", fontWeight = FontWeight.Bold)
                        DatosDemo.ganadores.forEach { (nombre, premio) ->
                            Row {
                                Text("🏅 $nombre", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(premio, color = GrisTexto, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSorteo(sorteo: Sorteo, inscrito: Boolean, onParticipar: () -> Unit) {
    TarjetaBlanca(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(sorteo.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(sorteo.detalle, color = GrisTexto, fontSize = 13.sp)
            Text(
                "${sorteo.cierre} · ${sorteo.inscritos + if (inscrito) 1 else 0} inscritos",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            if (inscrito) {
                Text(
                    "✓ Ya estás participando",
                    color = Verde, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(VerdeSuave)
                        .padding(14.dp)
                )
            } else {
                BotonAmarillo("Participar", onClick = onParticipar)
            }
        }
    }
}
