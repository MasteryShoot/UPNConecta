package com.example.upnconecta.sede

import com.example.upnconecta.data.local.HistorialDb
import com.example.upnconecta.data.Ajustes
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.DatosResena
import com.example.upnconecta.data.Moderacion
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Usuario
import com.example.upnconecta.data.remote.CriterioApi
import com.example.upnconecta.data.remote.ResenaApi
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.CabeceraSimple
import com.example.upnconecta.ui.componentes.SelectorEstrellas
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.RojoSuave
import com.example.upnconecta.ui.theme.pantallaConecta

class CalificarAreaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sedeId = intent.getStringExtra(DetalleAreaActivity.EXTRA_SEDE) ?: DatosDemo.sedes.first().id
        val areaId = intent.getStringExtra(DetalleAreaActivity.EXTRA_AREA) ?: DatosDemo.areas.first().id

        pantallaConecta {
            PantallaCalificarArea(sedeId, areaId)
        }
    }
}

// Reacciones para la experiencia general
private val reacciones = listOf(
    "😣" to "Muy mala", "😕" to "Mala", "😐" to "Regular", "🙂" to "Buena", "🤩" to "¡Excelente!"
)

@Composable
fun PantallaCalificarArea(sedeId: String, areaId: String, vm: CalificarAreaViewModel = viewModel()) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val usuario = remember { preferencias.usuarioActual() }
    val area = DatosDemo.area(areaId)

    if (usuario == null) {
        EvaluacionBloqueada(area.nombre, "Inicia sesión para calificar esta área")
        return
    }

    // READ al abrir la pantalla: criterios del área y evaluación previa del usuario
    LaunchedEffect(Unit) {
        if (vm.estado == EstadoCalificar.Cargando) vm.cargar(usuario, sedeId, areaId)
    }

    when (val estado = vm.estado) {
        EstadoCalificar.Cargando -> PantallaCargando(area.nombre)
        is EstadoCalificar.Error -> PantallaErrorRed(area.nombre, estado.mensaje) {
            vm.cargar(usuario, sedeId, areaId)
        }
        is EstadoCalificar.Listo -> FormularioCalificacion(
            sedeId, areaId, usuario, estado.criterios, estado.existente, vm, preferencias
        )
    }
}

@Composable
private fun FormularioCalificacion(
    sedeId: String,
    areaId: String,
    usuario: Usuario,
    criterios: List<CriterioApi>,
    existente: ResenaApi?,
    vm: CalificarAreaViewModel,
    preferencias: Preferencias
) {
    val context = LocalContext.current
    val sede = DatosDemo.sede(sedeId)
    val area = DatosDemo.area(areaId)
    val editando = existente != null

    // Si ya existe una evaluación, el formulario se llena con sus datos
    var experiencia by remember { mutableIntStateOf(existente?.experiencia ?: 0) }
    val puntajes = remember {
        mutableStateListOf(*Array(criterios.size) { i ->
            existente?.calificaciones?.firstOrNull { it.idCriterio == criterios[i].idCriterio }?.puntaje ?: 0
        })
    }
    var comentario by remember { mutableStateOf(existente?.comentario ?: "") }
    // Si es una evaluación nueva se usa el ajuste "anónimo por defecto" del perfil
    var anonimo by remember {
        mutableStateOf(existente?.esAnonima ?: Ajustes(context, usuario.correo).anonimoPorDefecto)
    }
    var sinReciclaje by remember { mutableStateOf(existente?.sinReciclaje ?: false) }
    var intentoGuardar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    val envio = vm.envio
    val enviando = envio == EstadoEnvio.Enviando

    // Resultado del envío: se actualiza la copia local y se regresa al detalle
    LaunchedEffect(envio) {
        when (envio) {
            is EstadoEnvio.Exito -> {
                val resena = envio.resena
                val historial = HistorialDb(context)
                if (resena == null) {
                    preferencias.eliminarEvaluacionArea(sedeId, areaId)
                    historial.eliminarEvaluacion(usuario.correo, HistorialDb.TIPO_AREA, "$sedeId|$areaId")
                } else {
                    historial.guardarEvaluacion(
                        usuario.correo, HistorialDb.TIPO_AREA, "$sedeId|$areaId",
                        DatosDemo.area(areaId).nombre, "UPN " + DatosDemo.sede(sedeId).nombre,
                        resena.promedio.toFloat()
                    )
                    preferencias.guardarEvaluacionArea(
                        sedeId, areaId, resena.calificaciones.map { it.puntaje }, resena.experiencia,
                        // la alerta de reciclaje solo se cuenta una vez
                        resena.sinReciclaje && existente?.sinReciclaje != true
                    )
                    preferencias.marcarComentarioArea(sedeId, areaId, resena.comentario != null)
                }
                Toast.makeText(context, envio.mensaje, Toast.LENGTH_LONG).show()
                (context as? Activity)?.finish()
            }
            is EstadoEnvio.Error -> error = envio.mensaje
            else -> Unit
        }
    }

    fun guardar() {
        intentoGuardar = true
        val faltanCriterios = puntajes.any { it == 0 }
        error = when {
            // HU2.02 / HU2.04 - Escenario: envío con campos incompletos
            faltanCriterios && area.id == "gestion" -> "Por favor, califique todas las áreas administrativas"
            faltanCriterios -> "Califica todos los criterios antes de guardar"
            experiencia == 0 -> "Cuéntanos qué tal fue tu experiencia general"
            else -> Moderacion.validarComentario(comentario, obligatorio = false)
        }
        if (error != null) return

        val datos = DatosResena(
            usuario = usuario,
            sedeId = sedeId,
            areaId = areaId,
            experiencia = experiencia,
            comentario = comentario,
            anonimo = anonimo && comentario.isNotBlank(),
            sinReciclaje = sinReciclaje,
            puntajes = criterios.mapIndexed { i, c -> c.idCriterio to puntajes[i] }.toMap()
        )
        vm.guardar(datos, existente)
    }

    if (confirmarEliminar && existente != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar calificación") },
            text = { Text("Se borrará tu evaluación de ${area.nombre.lowercase()} de este ciclo. Podrás volver a calificar después.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    vm.eliminar(existente, usuario)
                }) { Text("Eliminar", color = Rojo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar", color = Negro) }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
            .imePadding()
    ) {
        CabeceraSimple(
            if (editando) "Mi calificación" else "Enviar calificación",
            "${area.nombre} · UPN ${sede.nombre}"
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (existente != null) {
                Text(
                    "Ya enviaste tu evaluación del ciclo ${existente.periodo}. Puedes modificarla o eliminarla.",
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmarilloSuave)
                        .padding(12.dp)
                )
            }

            // Experiencia general con reacciones
            TarjetaBlanca(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("¿Qué tal fue tu experiencia general?", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reacciones.forEachIndexed { i, (emoji, _) ->
                            val elegida = experiencia == i + 1
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .scale(if (elegida) 1.15f else 1f)
                                    .clip(CircleShape)
                                    .background(if (elegida) Amarillo else Fondo)
                                    .clickable { experiencia = i + 1 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 26.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (experiencia == 0) "Toca una reacción" else reacciones[experiencia - 1].second,
                        color = if (intentoGuardar && experiencia == 0) Rojo else GrisTexto,
                        fontSize = 12.sp
                    )
                }
            }

            // Criterios específicos con estrellas (vienen del backend)
            TarjetaBlanca(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Califica cada criterio:", fontWeight = FontWeight.Bold)
                    criterios.forEachIndexed { i, criterio ->
                        val falta = intentoGuardar && puntajes[i] == 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (falta) RojoSuave else Blanco)
                                .border(BorderStroke(1.dp, if (falta) Rojo else GrisClaro), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(criterio.nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                if (falta) Text("Falta calificar", color = Rojo, fontSize = 11.sp)
                            }
                            SelectorEstrellas(puntajes[i], { puntajes[i] = it }, tamano = 26.dp, conError = falta)
                        }
                    }
                }
            }

            // HU5.01 - Escenario: sede sin puntos de reciclaje reportados
            if (area.id == "sostenibilidad") {
                TarjetaBlanca(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .clickable { sinReciclaje = !sinReciclaje }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("No encontré tachos de reciclaje visibles", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Se enviará una alerta al área de gestión ambiental", color = GrisTexto, fontSize = 12.sp)
                        }
                        Switch(
                            checked = sinReciclaje,
                            onCheckedChange = { sinReciclaje = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Amarillo, checkedThumbColor = Negro)
                        )
                    }
                }
            }

            // HU2.05: comentario opcional (máximo 500 caracteres)
            Text("COMENTARIOS ADICIONALES (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it.take(Moderacion.MAX_CARACTERES) },
                placeholder = { Text("Escribe aquí sobre tu experiencia...") },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            ContadorCaracteres(comentario.length)
            if (comentario.isNotBlank()) {
                Row(Modifier.clickable { anonimo = !anonimo }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = anonimo, onCheckedChange = { anonimo = it },
                        colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Negro)
                    )
                    Text("Publicar mi comentario como anónimo", fontSize = 13.sp)
                }
            }
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
            BotonAmarillo(
                when {
                    enviando -> "Enviando..."
                    editando -> "Guardar cambios"
                    else -> "Enviar calificación"
                },
                onClick = { guardar() },
                habilitado = !enviando
            )
            if (editando) {
                TextButton(
                    onClick = { confirmarEliminar = true },
                    enabled = !enviando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar mi calificación", color = Rojo, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Estado "Loading" mientras responde el backend. */
@Composable
private fun PantallaCargando(titulo: String) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        CabeceraSimple(titulo)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Amarillo)
            Spacer(Modifier.height(12.dp))
            Text("Cargando criterios...", color = GrisTexto, fontSize = 13.sp)
        }
    }
}

/** Estado "Error" cuando no se pudo conectar con el backend. */
@Composable
private fun PantallaErrorRed(titulo: String, mensaje: String, reintentar: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        CabeceraSimple(titulo)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📡", fontSize = 44.sp)
            Spacer(Modifier.height(16.dp))
            Text(mensaje, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))
            BotonAmarillo("Reintentar", onClick = reintentar)
        }
    }
}

/** Contador "quedan N caracteres"; se pone en rojo al llegar al límite (HU2.05). */
@Composable
fun ContadorCaracteres(usados: Int) {
    val restantes = Moderacion.MAX_CARACTERES - usados
    Text(
        "$restantes caracteres restantes · mínimo ${Moderacion.MIN_CARACTERES}",
        color = if (restantes == 0) Rojo else GrisTexto,
        fontSize = 12.sp,
        fontWeight = if (restantes == 0) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Pantalla que se muestra cuando el formulario ya fue enviado en el ciclo. */
@Composable
fun EvaluacionBloqueada(titulo: String, mensaje: String) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        CabeceraSimple(titulo)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(AmarilloSuave),
                contentAlignment = Alignment.Center
            ) { Text("🔒", fontSize = 44.sp) }
            Spacer(Modifier.height(20.dp))
            Text(mensaje, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text("Podrás volver a evaluar en el próximo ciclo.", color = GrisTexto, fontSize = 13.sp)
            Spacer(Modifier.width(1.dp).height(24.dp))
            BotonAmarillo("Volver", onClick = { (context as? Activity)?.finish() })
        }
    }
}
