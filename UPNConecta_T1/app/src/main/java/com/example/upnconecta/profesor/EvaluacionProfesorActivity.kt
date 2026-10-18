package com.example.upnconecta.profesor

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.upnconecta.data.Roles
import com.example.upnconecta.sede.ContadorCaracteres
import com.example.upnconecta.sede.EvaluacionBloqueada
import com.example.upnconecta.sede.TarjetaOpinion
import com.example.upnconecta.ui.componentes.Avatar
import com.example.upnconecta.ui.componentes.BotonAmarillo
import com.example.upnconecta.ui.componentes.CabeceraSimple
import com.example.upnconecta.ui.componentes.SelectorEstrellas
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.NegroSuave
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.RojoSuave
import com.example.upnconecta.ui.theme.pantallaConecta

class EvaluacionProfesorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val codigo = intent.getStringExtra(EXTRA_DOCENTE)
        val docente = DatosDemo.docente(codigo)

        // Validación de rol también aquí (HU1.02: intento de acceso directo)
        if (docente == null || Preferencias(this).usuarioActual()?.rol != Roles.ALUMNO) {
            Toast.makeText(this, "No tiene permisos para acceder a este módulo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pantallaConecta {
            PantallaEvaluacionProfesor(docente.codigo)
        }
    }

    companion object {
        const val EXTRA_DOCENTE = "docente"
    }
}

private val criteriosDocente = listOf("Metodología", "Puntualidad", "Dominio del tema", "Respeto")

@Composable
fun PantallaEvaluacionProfesor(codigo: String) {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val docente = DatosDemo.docente(codigo) ?: return

    // HU3.02 - Escenario: intento de doble calificación
    if (preferencias.yaEvaluoDocente(codigo)) {
        EvaluacionBloqueada("Evaluar docente", "Ya calificaste a este docente este semestre")
        return
    }

    val puntajes = remember { mutableStateListOf(0, 0, 0, 0) }
    var comentario by remember { mutableStateOf("") }
    var anonimo by remember { mutableStateOf(false) }
    var intento by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val opiniones = preferencias.comentariosDocente(codigo) + DatosDemo.opinionesDocenteBase()

    fun finalizar() {
        intento = true
        error = if (puntajes.any { it == 0 }) {
            "Califica los cuatro criterios para finalizar"
        } else {
            Moderacion.validarComentario(comentario, obligatorio = false)
        }
        if (error != null) return

        preferencias.guardarEvaluacionDocente(codigo, puntajes.toList())
        val usuario = preferencias.usuarioActual()
        // HU3.03: reseña anónima o pública
        val autor = if (anonimo) "Anónimo" else usuario?.nombre ?: "Alumno"
        if (comentario.isNotBlank()) {
            preferencias.publicarComentarioDocente(
                codigo,
                Opinion(autor, Roles.ALUMNO, comentario.trim(), puntajes.average().toInt(), "Reseña", "")
            )
        }
        Toast.makeText(context, "Evaluación registrada · Autor: $autor", Toast.LENGTH_LONG).show()
        (context as? Activity)?.finish()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Fondo)
            .imePadding()
    ) {
        CabeceraSimple("Evaluar docente", "Ciclo ${DatosDemo.CICLO_ACTUAL}")

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ficha del docente
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(NegroSuave)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(docente.nombre, 56.dp, Amarillo)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(docente.nombre, color = Blanco, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(docente.curso, color = Amarillo, fontSize = 13.sp)
                    Text("${docente.facultad} · UPN ${DatosDemo.sede(docente.sedeId).nombre}", color = GrisClaro, fontSize = 12.sp)
                }
            }

            TarjetaBlanca(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("¿Cómo fue tu experiencia de aprendizaje?", fontWeight = FontWeight.Bold)
                    criteriosDocente.forEachIndexed { i, criterio ->
                        val falta = intento && puntajes[i] == 0
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (falta) RojoSuave else Blanco)
                                .border(BorderStroke(1.dp, if (falta) Rojo else GrisClaro), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(criterio, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            SelectorEstrellas(puntajes[i], { puntajes[i] = it }, tamano = 26.dp, conError = falta)
                        }
                    }
                }
            }

            Text("RESEÑA SOBRE EL DOCENTE (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it.take(Moderacion.MAX_CARACTERES) },
                placeholder = { Text("Cuenta cómo fue la clase para guiar a otros compañeros...") },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            ContadorCaracteres(comentario.length)

            Row(Modifier.clickable { anonimo = !anonimo }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = anonimo, onCheckedChange = { anonimo = it },
                    colors = CheckboxDefaults.colors(checkedColor = Amarillo, checkmarkColor = Negro)
                )
                Text("Mantener mi comentario anónimo", fontSize = 14.sp)
            }

            Spacer(Modifier.height(4.dp))
            Text("Lo que dicen otros alumnos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            opiniones.forEach { TarjetaOpinion(it) }
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
            Text(
                "Solo puedes evaluar a cada docente una vez por semestre.",
                color = GrisTexto, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            BotonAmarillo("Finalizar evaluación", onClick = { finalizar() })
        }
    }
}
