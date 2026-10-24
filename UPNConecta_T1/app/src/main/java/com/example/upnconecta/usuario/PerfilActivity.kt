package com.example.upnconecta.usuario

import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import com.example.upnconecta.data.local.HistorialDb
import com.example.upnconecta.data.Ajustes
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.key
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.MiEvaluacion
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Roles
import com.example.upnconecta.sede.ElegirSedeActivity
import com.example.upnconecta.ui.componentes.Avatar
import com.example.upnconecta.ui.componentes.BarraNavegacion
import com.example.upnconecta.ui.componentes.ConectaActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.EstrellasPuntaje
import com.example.upnconecta.ui.componentes.Pestana
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.componentes.abrir
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.pantallaConecta

class PerfilActivity : ConectaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaConecta(barraOscura = true) {
            // key(): al volver a la pantalla se vuelve a leer SharedPreferences
            key(refresco.intValue) {
                PantallaPerfil(refresco.intValue)
            }
        }
    }
}

private data class Insignia(val emoji: String, val titulo: String, val detalle: String, val obtenida: Boolean)

@Composable
fun PantallaPerfil(refresco: Int) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val usuario = preferencias.usuarioActual() ?: return
    var menu by remember { mutableStateOf(false) }
    var verTerminos by remember { mutableStateOf(false) }

    val ajustes = remember { Ajustes(context, usuario.correo) }
    var anonimoPorDefecto by remember { mutableStateOf(ajustes.anonimoPorDefecto) }
    var avisoSorteos by remember { mutableStateOf(ajustes.avisoSorteos) }

    // El historial sale de SQLite, así se ve aunque no haya conexión
    val evaluaciones = remember(refresco) { HistorialDb(context).historial(usuario.correo) }
    val comentarios = preferencias.totalComentariosUsuario()
    val sorteos = preferencias.totalSorteosInscritos()
    val evaluoSostenibilidad = DatosDemo.sedes.any { preferencias.yaEvaluoArea(it.id, "sostenibilidad") }

    val insignias = listOf(
        Insignia("🌟", "Primera evaluación", "Tu primer aporte", evaluaciones.isNotEmpty()),
        Insignia("🧐", "Crítico frecuente", "5 o más evaluaciones", evaluaciones.size >= 5),
        Insignia("♻️", "Eco-guardián", "Evaluaste sostenibilidad", evaluoSostenibilidad),
        Insignia("💬", "Voz del campus", "Publicaste una opinión", comentarios > 0)
    )

    fun cerrarSesion() {
        preferencias.cerrarSesion()
        context.abrir(LoginActivity::class.java) {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    Scaffold(
        containerColor = Fondo,
        bottomBar = { BarraNavegacion(Pestana.PERFIL) }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mi Perfil", color = Amarillo, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Filled.Settings, "Ajustes", tint = Blanco)
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Blanco) {
                                DropdownMenuItem(
                                    text = { Text("Cambiar de sede") },
                                    onClick = { menu = false; context.abrir(ElegirSedeActivity::class.java) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Términos y condiciones") },
                                    onClick = { menu = false; verTerminos = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cerrar sesión", color = Rojo) },
                                    onClick = { menu = false; cerrarSesion() }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(usuario.nombre, 64.dp, Amarillo)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(usuario.nombre, color = Blanco, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                (if (usuario.rol == Roles.ALUMNO) "Código ${usuario.codigo} · " else "") +
                                    "Sede ${DatosDemo.sede(usuario.sedeId).nombre}",
                                color = GrisClaro, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Etiqueta(usuario.rol)
                        }
                    }
                }
            }

            // Estadísticas
            item {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Estadistica(evaluaciones.size.toString(), "Evaluaciones", Modifier.weight(1f))
                    Estadistica(comentarios.toString(), "Opiniones", Modifier.weight(1f))
                    Estadistica(sorteos.toString(), "Sorteos", Modifier.weight(1f))
                }
            }

            item { Titulo("Insignias obtenidas") }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    insignias.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            fila.forEach { TarjetaInsignia(it, Modifier.weight(1f)) }
                        }
                    }
                }
            }

            item { Titulo("Mis calificaciones recientes") }
            if (evaluaciones.isEmpty()) {
                item {
                    Text(
                        "Aún no has calificado. ¡Empieza por un área de tu sede! 🚀",
                        color = GrisTexto, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            items(evaluaciones.take(6)) { FilaEvaluacion(it) }

            // Ajustes guardados en SharedPreferences (archivo upn_conecta_ajustes)
            item { Titulo("Ajustes") }
            item {
                TarjetaBlanca(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        FilaAjuste("Publicar como anónimo por defecto", anonimoPorDefecto) {
                            anonimoPorDefecto = it
                            ajustes.anonimoPorDefecto = it
                        }
                        FilaAjuste("Mostrar aviso de sorteos en el inicio", avisoSorteos) {
                            avisoSorteos = it
                            ajustes.avisoSorteos = it
                        }
                        Text(
                            "Ingreso con " + if (ajustes.metodoIngreso == Ajustes.METODO_MICROSOFT)
                                "cuenta Microsoft 365" else "correo y contraseña",
                            color = GrisTexto, fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { cerrarSesion() },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Rojo, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión", color = Rojo, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (verTerminos) {
        DialogoTerminos(onAceptar = { verTerminos = false }, onCerrar = { verTerminos = false })
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun Estadistica(valor: String, texto: String, modifier: Modifier) {
    TarjetaBlanca(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(texto, color = GrisTexto, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TarjetaInsignia(insignia: Insignia, modifier: Modifier) {
    TarjetaBlanca(modifier) {
        Row(
            Modifier
                .padding(12.dp)
                .alpha(if (insignia.obtenida) 1f else 0.4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (insignia.obtenida) Amarillo else GrisClaro),
                contentAlignment = Alignment.Center
            ) { Text(insignia.emoji, fontSize = 18.sp) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(insignia.titulo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(if (insignia.obtenida) insignia.detalle else "🔒 Bloqueada", color = GrisTexto, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun FilaEvaluacion(evaluacion: MiEvaluacion) {
    TarjetaBlanca(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(evaluacion.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(evaluacion.subtitulo, color = GrisTexto, fontSize = 12.sp)
                Text(Preferencias.fechaRelativa(evaluacion.fecha), color = GrisTexto, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                EstrellasPuntaje(evaluacion.promedio, 14.dp)
                Text(
                    "%.1f".format(evaluacion.promedio), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmarilloSuave)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun FilaAjuste(texto: String, activo: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = activo, onCheckedChange = alCambiar,
            colors = SwitchDefaults.colors(checkedThumbColor = Negro, checkedTrackColor = Amarillo)
        )
    }
}
