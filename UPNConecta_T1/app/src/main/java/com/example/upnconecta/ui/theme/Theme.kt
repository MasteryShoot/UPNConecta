package com.example.upnconecta.ui.theme

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
            contenido()
        }
    }
}
