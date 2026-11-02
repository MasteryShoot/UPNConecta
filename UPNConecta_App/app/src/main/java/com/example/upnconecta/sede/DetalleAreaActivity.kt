package com.example.upnconecta.sede

import android.app.Activity
import android.os.Bundle
import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.Area
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Opinion
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.ResenaRepositorio
import com.example.upnconecta.data.ResumenArea
import com.example.upnconecta.data.Roles
import com.example.upnconecta.ui.componentes.Avatar
import com.example.upnconecta.ui.componentes.BarraCriterio
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.ConectaActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.EstrellasPuntaje
import com.example.upnconecta.ui.componentes.ImagenSede
import com.example.upnconecta.ui.componentes.InsigniaEstado
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.componentes.abrir
import com.example.upnconecta.ui.componentes.estadoPuntaje
import com.example.upnconecta.ui.componentes.iconoArea
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Azul
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.RojoSuave
import com.example.upnconecta.ui.theme.Verde
import com.example.upnconecta.ui.theme.VerdeSuave
import com.example.upnconecta.ui.theme.pantallaConecta
import kotlin.math.roundToInt

class DetalleAreaActivity : ConectaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Datos recibidos desde el Home mediante el Intent
        val sedeId = intent.getStringExtra(EXTRA_SEDE) ?: DatosDemo.sedes.first().id
        val areaId = intent.getStringExtra(EXTRA_AREA) ?: DatosDemo.areas.first().id

        pantallaConecta(barraOscura = true) {
            // key(): al volver a la pantalla se vuelve a leer SharedPreferences
            key(refresco.intValue) {
                PantallaDetalleArea(sedeId, areaId, refresco.intValue)
            }
        }
    }

    companion object {
        const val EXTRA_SEDE = "sede"
        const val EXTRA_AREA = "area"
    }
}

@Composable
fun PantallaDetalleArea(sedeId: String, areaId: String, refresco: Int) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val sede = DatosDemo.sede(sedeId)
    val area = DatosDemo.area(areaId)
    val resumen = preferencias.resumenArea(sedeId, areaId)
    val yaEvaluo = preferencias.yaEvaluoArea(sedeId, areaId)

    // Opiniones guardadas en el backend (GET /resenas); si no hay conexión solo se ven las locales
    var opinionesServidor by remember { mutableStateOf<List<Opinion>>(emptyList()) }
    LaunchedEffect(refresco) {
        ResenaRepositorio().opiniones(sedeId, areaId).onSuccess { lista ->
            opinionesServidor = lista.filter { !it.comentario.isNullOrBlank() }.map {
                Opinion(
                    autor = it.autor,
                    rol = if (it.rol == "DOCENTE") Roles.DOCENTE else Roles.ALUMNO,
                    texto = it.comentario!!,
                    puntaje = it.promedio.roundToInt(),
                    etiqueta = area.nombre,
                    fecha = it.fecha.take(10)
                )
            }
        }
    }
    val opiniones = opinionesServidor + preferencias.comentariosArea(sedeId, areaId) + DatosDemo.opinionesBase(areaId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera con la foto del campus
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                ImagenSede(sede, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0x88000000), Color.Transparent, Color(0xCC000000))))
                )
                IconButton(
                    onClick = { (context as? Activity)?.finish() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = Blanco)
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Amarillo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconoArea(area.id), null, tint = Negro, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Etiqueta(area.categoria)
                        Spacer(Modifier.height(4.dp))
                        Text(area.nombre, color = Blanco, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("UPN ${sede.nombre} · ${area.descripcion}", color = GrisClaro, fontSize = 12.sp)
                    }
                }
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (resumen.sinDatos) {
                    // HU4.01 - Escenario: sede sin calificaciones registradas
                    TarjetaBlanca(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Sin datos disponibles para esta sede en el ciclo actual",
                                fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                            Text("¡Sé el primero en evaluar esta área!", color = GrisTexto, fontSize = 13.sp)
                        }
                    }
                } else {
                    TarjetaIndice(resumen)
                    TarjetaCriterios(area, resumen)
                    TarjetaAlumnosDocentes(resumen)
                }

                if (area.id == "sostenibilidad") {
                    TarjetaSostenibilidad(preferencias, sedeId, resumen)
                }

                Text("Opiniones de la comunidad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                opiniones.forEach { TarjetaOpinion(it) }
            }
        }

        // Acciones fijas en la parte inferior
        Column(
            Modifier
                .fillMaxWidth()
                .background(Blanco)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            if (yaEvaluo) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(VerdeSuave)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Verde)
                    Spacer(Modifier.width(8.dp))
                    Text("Ya evaluaste esta área en el ciclo ${DatosDemo.CICLO_ACTUAL}", color = Verde, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                // Permite consultar, modificar o eliminar la calificación enviada
                TextButton(
                    onClick = {
                        context.abrir(CalificarAreaActivity::class.java) {
                            putExtra(DetalleAreaActivity.EXTRA_SEDE, sedeId)
                            putExtra(DetalleAreaActivity.EXTRA_AREA, areaId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver o editar mi calificación", color = Negro, fontWeight = FontWeight.SemiBold)
                }
            } else {
                BotonAmarillo("Calificar esta área", onClick = {
                    context.abrir(CalificarAreaActivity::class.java) {
                        putExtra(DetalleAreaActivity.EXTRA_SEDE, sedeId)
                        putExtra(DetalleAreaActivity.EXTRA_AREA, areaId)
                    }
                })
            }
            TextButton(
                onClick = {
                    context.abrir(ComentariosSedeActivity::class.java) {
                        putExtra(DetalleAreaActivity.EXTRA_SEDE, sedeId)
                        putExtra(DetalleAreaActivity.EXTRA_AREA, areaId)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💬  Dejar un comentario o sugerencia", color = Negro, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Índice general del área con reacción visual (reemplaza al rating clásico de estrellas). */
@Composable
private fun TarjetaIndice(resumen: ResumenArea) {
    val estado = estadoPuntaje(resumen.promedioGeneral)
    TarjetaBlanca(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(estado.fondo),
                contentAlignment = Alignment.Center
            ) {
                Text(estado.emoji, fontSize = 38.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("ÍNDICE CONECTA", style = MaterialTheme.typography.labelSmall, color = GrisTexto)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%.1f".format(resumen.promedioGeneral), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text(" / 5", color = GrisTexto, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                EstrellasPuntaje(resumen.promedioGeneral, 18.dp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InsigniaEstado(resumen.promedioGeneral)
                    Spacer(Modifier.width(8.dp))
                    Text("${resumen.totalEvaluaciones} evaluaciones", color = GrisTexto, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TarjetaCriterios(area: Area, resumen: ResumenArea) {
    val peor = area.criterios.indices.minByOrNull { resumen.promedioCriterios[it] } ?: 0
    TarjetaBlanca(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Puntaje por criterio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            area.criterios.forEachIndexed { i, criterio ->
                BarraCriterio(criterio, resumen.promedioCriterios[i])
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmarilloSuave)
                    .padding(10.dp)
            ) {
                Text(
                    "💡 Prioridad de mejora: ${area.criterios[peor]}",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** HU4.01: comparación de los votos de alumnos y docentes. */
@Composable
private fun TarjetaAlumnosDocentes(resumen: ResumenArea) {
    TarjetaBlanca(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("¿Cómo lo ven alumnos y docentes?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FilaComparativa("🎓 Alumnos", resumen.promedioAlumnos, Amarillo)
            FilaComparativa("👩‍🏫 Docentes", resumen.promedioDocentes, Azul)
            val diferencia = kotlin.math.abs(resumen.promedioAlumnos - resumen.promedioDocentes)
            if (diferencia >= 0.8f) {
                Text("⚠️ Hay una diferencia notable entre ambas percepciones.", color = GrisTexto, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FilaComparativa(texto: String, valor: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texto, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        LinearProgressIndicator(
            progress = { valor / 5f },
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = color,
            trackColor = GrisClaro,
            drawStopIndicator = {}
        )
        Spacer(Modifier.width(10.dp))
        Text("%.1f".format(valor), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** HU5.02: indicador de sostenibilidad y comparativo entre sedes. */
@Composable
private fun TarjetaSostenibilidad(preferencias: Preferencias, sedeId: String, resumen: ResumenArea) {
    var comparar by remember { mutableStateOf(false) }
    val alertas = preferencias.alertasReciclaje(sedeId)
    TarjetaBlanca(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("♻️ Indicador de sostenibilidad (ODS 12)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniDato("Puntaje", if (resumen.sinDatos) "—" else "%.1f".format(resumen.promedioGeneral), Modifier.weight(1f))
                MiniDato("Alertas activas", alertas.toString(), Modifier.weight(1f), if (alertas > 0) Rojo else Negro)
            }
            if (alertas > 0) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RojoSuave)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Rojo, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sin infraestructura de reciclaje reportada", color = Rojo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(onClick = { comparar = !comparar }, modifier = Modifier.fillMaxWidth()) {
                Text(if (comparar) "Ocultar comparación" else "Comparar sedes", color = Negro)
            }
            if (comparar) {
                val ranking = DatosDemo.sedes
                    .map { it to preferencias.resumenArea(it.id, "sostenibilidad") }
                    .sortedByDescending { it.second.promedioGeneral }
                ranking.forEachIndexed { i, (sede, r) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}.", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        Text(sede.nombre, fontSize = 13.sp, modifier = Modifier.weight(1f),
                            fontWeight = if (sede.id == sedeId) FontWeight.Bold else FontWeight.Normal)
                        Text(if (r.sinDatos) "Sin datos" else "%.1f".format(r.promedioGeneral), fontSize = 13.sp)
                    }
                    if (i < ranking.lastIndex) HorizontalDivider(color = GrisClaro)
                }
            }
        }
    }
}

@Composable
private fun MiniDato(titulo: String, valor: String, modifier: Modifier, color: Color = Negro) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Fondo)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(titulo, fontSize = 11.sp, color = GrisTexto)
    }
}

@Composable
fun TarjetaOpinion(opinion: Opinion) {
    TarjetaBlanca(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(opinion.autor, 38.dp, if (opinion.rol == Roles.DOCENTE) Color(0xFFDBEAFE) else AmarilloSuave)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(opinion.autor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (opinion.rol == Roles.DOCENTE) "Docente" else "Alumno",
                            fontSize = 10.sp,
                            color = if (opinion.rol == Roles.DOCENTE) Azul else GrisTexto,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(opinion.fecha, color = GrisTexto, fontSize = 11.sp)
                }
                if (opinion.puntaje > 0) {
                    Text(estadoPuntaje(opinion.puntaje.toFloat()).emoji, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(opinion.texto, fontSize = 14.sp)
            if (opinion.etiqueta.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Etiqueta(opinion.etiqueta, fondo = Fondo, color = GrisTexto)
            }
        }
    }
}
