package com.example.upnconecta.inicio

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.upnconecta.R
import com.example.upnconecta.data.Preferencias
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.NegroSuave
import com.example.upnconecta.ui.theme.pantallaConecta
import com.example.upnconecta.usuario.LoginActivity
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pantallaConecta(barraOscura = true) {
            LaunchedEffect(Unit) {
                delay(2000)
                irSiguientePantalla()
            }
            PantallaSplash()
        }
    }

    /** Si el usuario marcó "Recordarme" entra directo al Home; si no, va al Login. */
    private fun irSiguientePantalla() {
        val preferencias = Preferencias(this)
        val destino = if (preferencias.sesionRecordada()) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destino))
        finish()
    }
}

@Composable
fun PantallaSplash() {
    val animacion = rememberInfiniteTransition(label = "carga")
    val avance = animacion.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "barra"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Negro)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_escudo),
                contentDescription = "Logo UPN Conecta",
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text("UPN Conecta", color = Amarillo, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Tu experiencia universitaria, en un solo lugar", color = GrisTexto, fontSize = 14.sp)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(56.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NegroSuave)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = avance.value
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                        .background(Amarillo)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Cargando portal universitario...", color = GrisTexto, fontSize = 12.sp)
        }
    }
}
