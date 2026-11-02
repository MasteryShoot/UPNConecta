package com.example.upnconecta.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.data.remote.Conectividad
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import android.graphics.Color as ColorAndroid

// La app usa siempre el tema claro amarillo/negro del prototipo
private val EsquemaConecta = lightColorScheme(
    primary = Amarillo,
    onPrimary = Negro,
    secondary = Negro,
    onSecondary = Blanco,
    background = Fondo,
    onBackground = Negro,
    surface = Blanco,
    onSurface = Negro,
    surfaceVariant = Fondo,
    onSurfaceVariant = GrisTexto,
    outline = GrisClaro,
    error = Rojo
)

@Composable
fun UPNConectaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaConecta,
        typography = Typography,
        content = content
    )
}

/**
 * Atajo que usan todas las Activities: configura la barra de estado
 * (íconos claros si la pantalla es oscura) y aplica el tema de la app.
 */
fun ComponentActivity.pantallaConecta(
    barraOscura: Boolean = false,
    contenido: @Composable () -> Unit
) {
    enableEdgeToEdge(
        statusBarStyle = if (barraOscura) {
            SystemBarStyle.dark(ColorAndroid.TRANSPARENT)
        } else {
            SystemBarStyle.light(ColorAndroid.TRANSPARENT, ColorAndroid.TRANSPARENT)
        },
        navigationBarStyle = SystemBarStyle.light(ColorAndroid.WHITE, ColorAndroid.WHITE)
    )
    setContent {
        UPNConectaTheme {
            val enLinea by Conectividad.enLinea.collectAsState()
            Box(Modifier.fillMaxSize()) {
                contenido()
                if (!enLinea) {
                    Text(
                        "Sin conexión a internet",
                        color = Blanco, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(Rojo)
                            .statusBarsPadding()
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
