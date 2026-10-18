package com.example.upnconecta.ui.componentes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.Sede
import com.example.upnconecta.explorar.ExplorarActivity
import com.example.upnconecta.inicio.HomeActivity
import com.example.upnconecta.sorteo.SorteosActivity
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Naranja
import com.example.upnconecta.ui.theme.NaranjaSuave
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.NegroSuave
import com.example.upnconecta.ui.theme.Rojo
import com.example.upnconecta.ui.theme.RojoSuave
import com.example.upnconecta.ui.theme.Verde
import com.example.upnconecta.ui.theme.VerdeSuave
import com.example.upnconecta.usuario.PerfilActivity

/**
 * Activity base: cada vez que la pantalla vuelve a primer plano aumenta [refresco],
 * así la interfaz vuelve a leer las SharedPreferences (por ejemplo, al regresar de calificar).
 */
open class ConectaActivity : ComponentActivity() {
    val refresco = mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        refresco.intValue++
    }
}

/** Abre otra Activity mediante un Intent explícito. */
fun Context.abrir(destino: Class<*>, extras: Intent.() -> Unit = {}) {
    val intent = Intent(this, destino)
    intent.extras()
    startActivity(intent)
}

// ------------------------------------------------------------ Navegación

enum class Pestana { INICIO, EXPLORAR, SORTEOS, PERFIL }

/** Barra inferior del prototipo: Inicio, Explorar, Sorteos y Perfil. */
@Composable
fun BarraNavegacion(actual: Pestana) {
    val context = LocalContext.current
    val opciones = listOf(
        Triple(Pestana.INICIO, "Inicio", Icons.Filled.Home),
        Triple(Pestana.EXPLORAR, "Explorar", Icons.Filled.Explore),
        Triple(Pestana.SORTEOS, "Sorteos", Icons.Filled.CardGiftcard),
        Triple(Pestana.PERFIL, "Perfil", Icons.Filled.Person)
    )
    NavigationBar(containerColor = Blanco, tonalElevation = 0.dp) {
        opciones.forEach { (pestana, texto, icono) ->
            NavigationBarItem(
                selected = pestana == actual,
                onClick = {
                    if (pestana != actual) {
                        val destino = when (pestana) {
                            Pestana.INICIO -> HomeActivity::class.java
                            Pestana.EXPLORAR -> ExplorarActivity::class.java
                            Pestana.SORTEOS -> SorteosActivity::class.java
                            Pestana.PERFIL -> PerfilActivity::class.java
                        }
                        // Si la pantalla ya estaba abierta se reutiliza en vez de crear otra
                        context.abrir(destino) {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                    }
                },
                icon = { Icon(icono, contentDescription = texto) },
                label = {
                    Text(
                        texto,
                        fontSize = 11.sp,
                        fontWeight = if (pestana == actual) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Negro,
                    selectedTextColor = Negro,
                    indicatorColor = Amarillo,
                    unselectedIconColor = GrisTexto,
                    unselectedTextColor = GrisTexto
                )
            )
        }
    }
}

/** Cabecera clara con botón de regresar (pantallas secundarias). */
@Composable
fun CabeceraSimple(titulo: String, subtitulo: String? = null) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blanco)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { (context as? Activity)?.finish() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
        }
        Column {
            if (subtitulo != null) {
                Text(subtitulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = GrisTexto)
            }
            Text(titulo, style = MaterialTheme.typography.titleLarge)
        }
    }
}

// ------------------------------------------------------------ Botones y tarjetas

@Composable
fun BotonAmarillo(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Amarillo,
            contentColor = Negro,
            disabledContainerColor = GrisClaro,
            disabledContentColor = GrisTexto
        )
    ) {
        Text(texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun TarjetaBlanca(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    val forma = RoundedCornerShape(16.dp)
    val colores = CardDefaults.cardColors(containerColor = Blanco)
    val borde = BorderStroke(1.dp, GrisClaro)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = forma, colors = colores, border = borde) {
            contenido()
        }
    } else {
        Card(modifier = modifier, shape = forma, colors = colores, border = borde) {
            contenido()
        }
    }
}

/** Etiqueta pequeña en mayúsculas (ej. "CAFETERÍA" en el prototipo). */
@Composable
fun Etiqueta(texto: String, fondo: Color = Amarillo, color: Color = Negro) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ------------------------------------------------------------ Puntajes

/** Estado visual de un puntaje: en vez de solo números se muestra una reacción y un color. */
data class EstadoPuntaje(val texto: String, val emoji: String, val color: Color, val fondo: Color)

fun estadoPuntaje(valor: Float): EstadoPuntaje = when {
    valor >= 4.2f -> EstadoPuntaje("Excelente", "🤩", Verde, VerdeSuave)
    valor >= 3.5f -> EstadoPuntaje("Bueno", "🙂", Verde, VerdeSuave)
    valor >= 2.8f -> EstadoPuntaje("Regular", "😐", Naranja, NaranjaSuave)
    else -> EstadoPuntaje("Por mejorar", "😟", Rojo, RojoSuave)
}

@Composable
fun InsigniaEstado(valor: Float, modifier: Modifier = Modifier) {
    val estado = estadoPuntaje(valor)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(estado.fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(estado.emoji, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(estado.texto, color = estado.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Estrellas de solo lectura; admite medias estrellas (4.3 → 4 llenas y una parcial). */
@Composable
fun EstrellasPuntaje(valor: Float, tamano: Dp = 14.dp) {
    Row {
        for (i in 1..5) {
            val relleno = (valor - (i - 1)).coerceIn(0f, 1f)
            Box(Modifier.size(tamano)) {
                Icon(Icons.Filled.Star, null, tint = GrisClaro, modifier = Modifier.fillMaxSize())
                Icon(
                    Icons.Filled.Star, null, tint = Amarillo,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(right = size.width * relleno) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )
            }
        }
    }
}

/** Estrellas para calificar: se tocan y crecen un poco al seleccionarlas. */
@Composable
fun SelectorEstrellas(
    valor: Int,
    onCambio: (Int) -> Unit,
    tamano: Dp = 30.dp,
    conError: Boolean = false
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            val activa = i <= valor
            val escala = animateFloatAsState(if (activa) 1.12f else 1f, label = "estrella").value
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "$i estrellas",
                tint = when {
                    activa -> Amarillo
                    conError -> Color(0xFFFCA5A5)
                    else -> GrisClaro
                },
                modifier = Modifier
                    .size(tamano)
                    .scale(escala)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCambio(i) }
            )
        }
    }
}

/** Barra de un criterio: nombre, barra de color según el estado y el puntaje con estrella. */
@Composable
fun BarraCriterio(nombre: String, valor: Float) {
    val estado = estadoPuntaje(valor)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Star, null, tint = Amarillo, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(2.dp))
            Text("%.1f".format(valor), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { valor / 5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = estado.color,
            trackColor = GrisClaro,
            drawStopIndicator = {}
        )
    }
}

// ------------------------------------------------------------ Imágenes e íconos

fun iconoArea(areaId: String): ImageVector = when (areaId) {
    "infraestructura" -> Icons.Filled.Apartment
    "limpieza" -> Icons.Filled.CleaningServices
    "gestion" -> Icons.Filled.SupportAgent
    else -> Icons.Filled.Recycling
}

/** Ícono del área dentro de un cuadrado amarillo (como "Todas las áreas" del prototipo). */
@Composable
fun IconoArea(areaId: String, tamano: Dp = 44.dp) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(RoundedCornerShape(12.dp))
            .background(Amarillo),
        contentAlignment = Alignment.Center
    ) {
        Icon(iconoArea(areaId), contentDescription = null, tint = Negro, modifier = Modifier.size(tamano * 0.55f))
    }
}

/**
 * Foto de la sede. Si la imagen aún no se agregó a res/drawable,
 * se muestra un degradado con un ícono para que la app no falle.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun ImagenSede(sede: Sede, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val id = remember(sede.imagen) {
        context.resources.getIdentifier(sede.imagen, "drawable", context.packageName)
    }
    if (id != 0) {
        Image(
            painter = painterResource(id),
            contentDescription = "Campus ${sede.nombre}",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Brush.linearGradient(listOf(NegroSuave, Negro))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Apartment, null, tint = Amarillo, modifier = Modifier.size(40.dp))
        }
    }
}

/** Círculo con las iniciales del usuario (sin fotos de personas). */
@Composable
fun Avatar(nombre: String, tamano: Dp = 40.dp, fondo: Color = AmarilloSuave) {
    val iniciales = if (nombre.startsWith("Anónimo")) {
        "?"
    } else {
        nombre.split(" ").filter { it.isNotBlank() && it != "Prof." }
            .take(2).joinToString("") { it.first().uppercase() }
    }
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.38f).sp, color = Negro)
    }
}

/** Texto de una línea con puntos suspensivos. */
@Composable
fun TextoCorto(texto: String, color: Color = GrisTexto, tamano: Int = 12) {
    Text(texto, color = color, fontSize = tamano.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
