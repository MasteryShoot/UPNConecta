package com.example.upnconecta.sede

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.data.Sede
import com.example.upnconecta.inicio.HomeActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.ImagenSede
import com.example.upnconecta.ui.componentes.estadoPuntaje
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.pantallaConecta

class ElegirSedeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaConecta {
            PantallaElegirSede()
        }
    }
}

/** Promedio general de la sede considerando todas sus áreas. */
fun indiceSede(preferencias: Preferencias, sedeId: String): Float {
    val resumenes = DatosDemo.areas
        .map { preferencias.resumenArea(sedeId, it.id) }
        .filter { !it.sinDatos }
    return if (resumenes.isEmpty()) 0f else resumenes.map { it.promedioGeneral }.average().toFloat()
}

@Composable
fun PantallaElegirSede() {
    val context = LocalContext.current
    val preferencias = remember { Preferencias(context) }
    val sedeActual = preferencias.sedeActual()

    fun elegir(sede: Sede) {
        // HU2.01: la sede elegida queda guardada en SharedPreferences
        preferencias.guardarSede(sede.id)
        Toast.makeText(context, "Sede seleccionada: UPN ${sede.nombre}", Toast.LENGTH_SHORT).show()
        val intent = Intent(context, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Text("Elige tu sede", style = MaterialTheme.typography.headlineMedium)
                Text("Selecciona el campus para calificar sus áreas", color = GrisTexto, fontSize = 14.sp)
            }
        }
        items(DatosDemo.sedes) { sede ->
            TarjetaSede(
                sede = sede,
                indice = indiceSede(preferencias, sede.id),
                esMiSede = sede.id == sedeActual,
                onClick = { elegir(sede) }
            )
        }
    }
}

@Composable
fun TarjetaSede(sede: Sede, indice: Float, esMiSede: Boolean, onClick: () -> Unit) {
    val estado = estadoPuntaje(indice)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco),
        border = BorderStroke(if (esMiSede) 2.dp else 1.dp, if (esMiSede) Amarillo else GrisClaro)
    ) {
        Box {
            ImagenSede(
                sede, Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )
            if (esMiSede) {
                Box(Modifier.padding(8.dp)) { Etiqueta("Tu sede") }
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(
                sede.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(sede.ciudad, color = GrisTexto, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, tint = Amarillo, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("%.1f".format(indice), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text("${estado.emoji} ${estado.texto}", color = estado.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
