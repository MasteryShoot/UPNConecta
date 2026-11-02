package com.example.upnconecta.inicio

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.ResumenArea
import com.example.upnconecta.data.Roles
import com.example.upnconecta.data.Sede
import com.example.upnconecta.explorar.ExplorarActivity
import com.example.upnconecta.profesor.ProfesoresActivity
import com.example.upnconecta.sede.DetalleAreaActivity
import com.example.upnconecta.sede.ElegirSedeActivity
import com.example.upnconecta.ui.componentes.BarraNavegacion
import com.example.upnconecta.ui.componentes.ConectaActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.EstrellasPuntaje
import com.example.upnconecta.ui.componentes.IconoArea
import com.example.upnconecta.ui.componentes.ImagenSede
import com.example.upnconecta.ui.componentes.InsigniaEstado
import com.example.upnconecta.ui.componentes.Pestana
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.componentes.abrir
import com.example.upnconecta.ui.componentes.iconoArea
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.NegroSuave
import com.example.upnconecta.ui.theme.Verde
import com.example.upnconecta.ui.theme.pantallaConecta
import com.example.upnconecta.usuario.LoginActivity

class HomeActivity : ConectaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si no hay sesión abierta se regresa al Login
        if (Preferencias(this).usuarioActual() == null) {
            abrir(LoginActivity::class.java) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            finish()
            return
        }

        pantallaConecta {
            // key(): al volver a la pantalla se vuelve a leer SharedPreferences
            key(refresco.intValue) {
                PantallaHome(refresco.intValue)
            }
        }
    }
}

private enum class Filtro(val texto: String) { TODAS("Todas"), MEJORES("Mejor valoradas"), MEJORAR("Por mejorar") }

@Composable
fun PantallaHome(refresco: Int) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val usuario = preferencias.usuarioActual() ?: return
    val sede = DatosDemo.sede(usuario.sedeId)
    var filtro by remember { mutableStateOf(Filtro.TODAS) }

    // Se recalcula cada vez que cambia "refresco" (al volver de otra pantalla)
    val resumenes = DatosDemo.areas.associate { it.id to preferencias.resumenArea(sede.id, it.id) }
    val evaluadas = preferencias.areasEvaluadas(sede.id)
    val mejores = DatosDemo.areas
        .filter { resumenes.getValue(it.id).sinDatos.not() }
        .sortedByDescending { resumenes.getValue(it.id).promedioGeneral }
    val listaFiltrada = when (filtro) {
        Filtro.TODAS -> DatosDemo.areas
        Filtro.MEJORES -> DatosDemo.areas.filter { resumenes.getValue(it.id).promedioGeneral >= 3.5f }
        Filtro.MEJORAR -> DatosDemo.areas.filter {
            val r = resumenes.getValue(it.id)
            !r.sinDatos && r.promedioGeneral < 3.5f
        }
    }

    fun abrirArea(area: Area) {
        context.abrir(DetalleAreaActivity::class.java) {
            putExtra(DetalleAreaActivity.EXTRA_SEDE, sede.id)
            putExtra(DetalleAreaActivity.EXTRA_AREA, area.id)
        }
    }

    Scaffold(
        containerColor = Fondo,
        bottomBar = { BarraNavegacion(Pestana.INICIO) }
    ) { espacio ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = espacio.calculateTopPadding() + 8.dp,
                bottom = espacio.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selector de sede (HU1.02 / HU2.01) y buscador
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { context.abrir(ElegirSedeActivity::class.java) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = Negro)
                        Spacer(Modifier.width(6.dp))
                        Text("UPN ${sede.nombre}", style = MaterialTheme.typography.titleLarge)
                        Icon(Icons.Filled.KeyboardArrowDown, "Cambiar sede", tint = GrisTexto)
                    }
                    IconButton(
                        onClick = { context.abrir(ExplorarActivity::class.java) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Blanco)
                    ) {
                        Icon(Icons.Filled.Search, "Explorar sedes")
                    }
                }
            }

            item { TarjetaBienvenida(usuario.primerNombre, usuario.rol, evaluadas) }

            // Filtros
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Filtro.entries) { opcion ->
                        FilterChip(
                            selected = filtro == opcion,
                            onClick = { filtro = opcion },
                            label = { Text(opcion.texto, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Blanco,
                                selectedContainerColor = Amarillo,
                                selectedLabelColor = Negro
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filtro == opcion,
                                borderColor = GrisClaro, selectedBorderColor = Amarillo
                            )
                        )
                    }
                }
            }

            if (filtro == Filtro.TODAS && mejores.isNotEmpty()) {
                item { TituloSeccion("Mejor valoradas en tu sede") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mejores.take(3)) { area ->
                            TarjetaDestacada(sede, area, resumenes.getValue(area.id)) { abrirArea(area) }
                        }
                    }
                }
            }

            item { TituloSeccion(if (filtro == Filtro.TODAS) "Todas las áreas" else filtro.texto) }

            if (listaFiltrada.isEmpty()) {
                item {
                    Text(
                        "No hay áreas en este filtro por ahora.",
                        color = GrisTexto,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            items(listaFiltrada) { area ->
                FilaArea(
                    area = area,
                    resumen = resumenes.getValue(area.id),
                    evaluada = preferencias.yaEvaluoArea(sede.id, area.id),
                    onClick = { abrirArea(area) }
                )
            }

            item { TituloSeccion("Docentes") }
            item { TarjetaDocentes(usuario.rol) }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
    )
}

/** Tarjeta negra de bienvenida con el progreso del ciclo (motiva a evaluar). */
@Composable
private fun TarjetaBienvenida(nombre: String, rol: String, evaluadas: Int) {
    val total = DatosDemo.areas.size
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Negro, NegroSuave)))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hola, $nombre 👋", color = Blanco, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Etiqueta(rol)
        }
        Spacer(Modifier.height(4.dp))
        Text("Ciclo ${DatosDemo.CICLO_ACTUAL} · Tu opinión mejora el campus", color = GrisClaro, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Row {
            Text("Áreas evaluadas", color = GrisClaro, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("$evaluadas / $total", color = Amarillo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { evaluadas / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Amarillo,
            trackColor = Color(0x33FFFFFF),
            drawStopIndicator = {}
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (evaluadas == total) "🏆 ¡Evaluaste todo tu campus este ciclo!"
            else "🎁 Cada área evaluada te da un boleto para el sorteo",
            color = Blanco, fontSize = 12.sp
        )
    }
}

@Composable
private fun TarjetaDestacada(sede: Sede, area: Area, resumen: ResumenArea, onClick: () -> Unit) {
    TarjetaBlanca(modifier = Modifier.width(200.dp), onClick = onClick) {
        Box {
            ImagenSede(
                sede, Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Box(
                Modifier
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Amarillo),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconoArea(area.id), null, tint = Negro, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(area.categoria, style = MaterialTheme.typography.labelSmall, color = GrisTexto)
            Text(area.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstrellasPuntaje(resumen.promedioGeneral, 13.dp)
                Spacer(Modifier.width(4.dp))
                Text("%.1f".format(resumen.promedioGeneral), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FilaArea(area: Area, resumen: ResumenArea, evaluada: Boolean, onClick: () -> Unit) {
    TarjetaBlanca(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoArea(area.id)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(area.categoria, style = MaterialTheme.typography.labelSmall, color = GrisTexto)
                Text(area.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                if (resumen.sinDatos) {
                    Text("Aún sin evaluaciones este ciclo", color = GrisTexto, fontSize = 12.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EstrellasPuntaje(resumen.promedioGeneral, 13.dp)
                        Spacer(Modifier.width(4.dp))
                        Text("%.1f".format(resumen.promedioGeneral), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        InsigniaEstado(resumen.promedioGeneral)
                    }
                }
            }
            if (evaluada) {
                Icon(Icons.Filled.CheckCircle, "Evaluada", tint = Verde)
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GrisTexto)
            }
        }
    }
}

/** HU1.01 / HU1.02: solo los alumnos ven el módulo "Calificar docentes". */
@Composable
private fun TarjetaDocentes(rol: String) {
    val context = LocalContext.current
    val esAlumno = rol == Roles.ALUMNO
    TarjetaBlanca(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        onClick = if (esAlumno) {
            { context.abrir(ProfesoresActivity::class.java) }
        } else null
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (esAlumno) AmarilloSuave else GrisClaro),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (esAlumno) Icons.Filled.School else Icons.Filled.Lock, null, tint = Negro)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (esAlumno) "Califica a tus docentes" else "Calificar docentes",
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Text(
                    if (esAlumno) "Metodología, puntualidad, dominio del tema y respeto"
                    else "Módulo no disponible para docentes (evita conflictos de interés)",
                    color = GrisTexto, fontSize = 12.sp
                )
            }
            if (esAlumno) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = GrisTexto)
        }
    }
}
