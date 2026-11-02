package com.example.upnconecta.explorar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.upnconecta.data.DatosDemo
import com.example.upnconecta.data.Sede
import com.example.upnconecta.data.sinTildes
import com.example.upnconecta.ui.componentes.BarraNavegacion
import com.example.upnconecta.ui.componentes.ConectaActivity
import com.example.upnconecta.ui.componentes.Etiqueta
import com.example.upnconecta.ui.componentes.ImagenSede
import com.example.upnconecta.ui.componentes.Pestana
import com.example.upnconecta.ui.componentes.TarjetaBlanca
import com.example.upnconecta.ui.theme.Amarillo
import com.example.upnconecta.ui.theme.AmarilloSuave
import com.example.upnconecta.ui.theme.Blanco
import com.example.upnconecta.ui.theme.Fondo
import com.example.upnconecta.ui.theme.GrisClaro
import com.example.upnconecta.ui.theme.GrisTexto
import com.example.upnconecta.ui.theme.Negro
import com.example.upnconecta.ui.theme.pantallaConecta
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

class ExplorarActivity : ConectaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaConecta {
            PantallaExplorar()
        }
    }
}

// Centro de cada ciudad con sedes UPN
private val ciudades = listOf(
    Triple("Lima", LatLng(-12.06, -77.03), 10.5f),
    Triple("Trujillo", LatLng(-8.095, -79.032), 13f),
    Triple("Cajamarca", LatLng(-7.155, -78.512), 13f)
)

/** Revisa si en el Manifest se configuró la API key de Google Maps. */
private fun tieneApiKeyMaps(context: Context): Boolean = try {
    val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
    !info.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
} catch (e: Exception) {
    false
}

private fun tienePermisoUbicacion(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Última ubicación conocida del teléfono (sin librerías extra). */
@SuppressLint("MissingPermission")
private fun ultimaUbicacion(context: Context): Location? {
    if (!tienePermisoUbicacion(context)) return null
    val gestor = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { proveedor -> runCatching { gestor.getLastKnownLocation(proveedor) }.getOrNull() }
        .maxByOrNull { it.time }
}

/** Pide una ubicación actual cuando no hay una guardada (por ejemplo, la primera vez). */
@SuppressLint("MissingPermission")
private fun pedirUbicacionActual(context: Context, alRecibir: (Location) -> Unit) {
    if (!tienePermisoUbicacion(context)) return
    val gestor = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val proveedor = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { gestor.isProviderEnabled(it) } ?: return
    LocationManagerCompat.getCurrentLocation(
        gestor, proveedor, null as android.os.CancellationSignal?, ContextCompat.getMainExecutor(context),
        androidx.core.util.Consumer<Location?> { ubicacion -> if (ubicacion != null) alRecibir(ubicacion) }
    )
}

private fun distanciaKm(desde: Location, sede: Sede): Float {
    val resultado = FloatArray(1)
    Location.distanceBetween(desde.latitude, desde.longitude, sede.latitud, sede.longitud, resultado)
    return resultado[0] / 1000f
}

/** Abre la app de Google Maps con la ruta hacia la sede (no necesita API key). */
private fun abrirComoLlegar(context: Context, sede: Sede) {
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${sede.latitud},${sede.longitud}")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

@Composable
fun PantallaExplorar() {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val hayApiKey = remember { tieneApiKeyMaps(context) }

    var permiso by remember { mutableStateOf(tienePermisoUbicacion(context)) }
    var miUbicacion by remember { mutableStateOf(ultimaUbicacion(context)) }
    var busqueda by remember { mutableStateOf("") }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        permiso = resultado.values.any { it }
        if (permiso) {
            miUbicacion = ultimaUbicacion(context)
            pedirUbicacionActual(context) { miUbicacion = it }
        }
    }

    // Si ya había permiso pero no hay ubicación guardada, se pide una al abrir la pantalla
    LaunchedEffect(Unit) {
        if (permiso && miUbicacion == null) pedirUbicacionActual(context) { miUbicacion = it }
    }

    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ciudades.first().second, ciudades.first().third)
    }

    fun centrar(destino: LatLng, zoom: Float) {
        alcance.launch { camara.animate(CameraUpdateFactory.newLatLngZoom(destino, zoom), 800) }
    }

    val ubicacion = miUbicacion
    val sedes = DatosDemo.sedes
        .filter {
            it.nombre.sinTildes().contains(busqueda.sinTildes()) ||
                it.ciudad.sinTildes().contains(busqueda.sinTildes())
        }
        .let { lista -> if (ubicacion != null) lista.sortedBy { distanciaKm(ubicacion, it) } else lista }

    Scaffold(
        containerColor = Fondo,
        bottomBar = { BarraNavegacion(Pestana.EXPLORAR) }
    ) { espacio ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = espacio.calculateTopPadding() + 8.dp,
                bottom = espacio.calculateBottomPadding() + 16.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Explorar sedes", style = MaterialTheme.typography.headlineMedium)
                Text("Ubica los campus UPN y encuentra el más cercano", color = GrisTexto, fontSize = 14.sp)
            }

            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar sede o ciudad") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amarillo, unfocusedBorderColor = GrisClaro,
                        focusedContainerColor = Blanco, unfocusedContainerColor = Blanco
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mapa de Google con un marcador por sede
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (hayApiKey) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = camara,
                            properties = MapProperties(isMyLocationEnabled = permiso),
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = permiso)
                        ) {
                            DatosDemo.sedes.forEach { sede ->
                                key(sede.id) {
                                    val estado = remember { MarkerState(position = LatLng(sede.latitud, sede.longitud)) }
                                    Marker(state = estado, title = "UPN ${sede.nombre}", snippet = sede.direccion)
                                }
                            }
                        }
                    } else {
                        MapaSinConfigurar()
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ciudades) { (nombre, centro, zoom) ->
                        AssistChip(
                            onClick = { centrar(centro, zoom) },
                            label = { Text(nombre) },
                            leadingIcon = { Icon(Icons.Filled.Map, null, modifier = Modifier.size(18.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Blanco)
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                if (permiso) {
                                    miUbicacion = ultimaUbicacion(context)
                                    miUbicacion?.let { centrar(LatLng(it.latitude, it.longitude), 13f) }
                                    pedirUbicacionActual(context) {
                                        miUbicacion = it
                                        centrar(LatLng(it.latitude, it.longitude), 13f)
                                    }
                                } else {
                                    pedirPermiso.launch(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    )
                                }
                            },
                            label = { Text(if (permiso) "Mi ubicación" else "Activar ubicación") },
                            leadingIcon = { Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(18.dp)) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = AmarilloSuave)
                        )
                    }
                }
            }

            item {
                Text(
                    if (ubicacion != null) "Sedes cercanas a ti" else "Todas las sedes",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
            }

            items(sedes) { sede ->
                TarjetaUbicacionSede(
                    sede = sede,
                    distancia = ubicacion?.let { distanciaKm(it, sede) },
                    onVerMapa = { centrar(LatLng(sede.latitud, sede.longitud), 16f) },
                    onComoLlegar = { abrirComoLlegar(context, sede) }
                )
            }
        }
    }
}

@Composable
private fun MapaSinConfigurar() {
    Column(
        Modifier
            .fillMaxSize()
            .background(AmarilloSuave)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🗺️", fontSize = 40.sp)
        Text("Mapa pendiente de configurar", fontWeight = FontWeight.Bold)
        Text(
            "Agrega MAPS_API_KEY en local.properties para ver el mapa. " +
                "Mientras tanto, usa \"Cómo llegar\" para abrir Google Maps.",
            color = GrisTexto, fontSize = 12.sp
        )
    }
}

@Composable
private fun TarjetaUbicacionSede(
    sede: Sede,
    distancia: Float?,
    onVerMapa: () -> Unit,
    onComoLlegar: () -> Unit
) {
    TarjetaBlanca(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImagenSede(
                    sede, Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("UPN ${sede.nombre}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(sede.direccion, color = GrisTexto, fontSize = 12.sp)
                    Text(sede.ciudad, color = GrisTexto, fontSize = 12.sp)
                }
                if (distancia != null) {
                    Etiqueta(if (distancia < 1f) "%.0f m".format(distancia * 1000) else "%.1f km".format(distancia))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerMapa, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Map, null, tint = Negro, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ver en mapa", color = Negro, fontSize = 13.sp)
                }
                OutlinedButton(onClick = onComoLlegar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Directions, null, tint = Negro, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cómo llegar", color = Negro, fontSize = 13.sp)
                }
            }
        }
    }
}
