package com.example.upnconecta.sede

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.upnconecta.data.DatosResena
import com.example.upnconecta.data.ResenaRepositorio
import com.example.upnconecta.data.Usuario
import com.example.upnconecta.data.remote.CriterioApi
import com.example.upnconecta.data.remote.ResenaApi
import kotlinx.coroutines.launch

/** Estado de la pantalla al abrirla (Loading / Success / Error). */
sealed interface EstadoCalificar {
    data object Cargando : EstadoCalificar
    data class Listo(val criterios: List<CriterioApi>, val existente: ResenaApi?) : EstadoCalificar
    data class Error(val mensaje: String) : EstadoCalificar
}

/** Estado del envío del formulario (registrar, actualizar o eliminar). */
sealed interface EstadoEnvio {
    data object Inactivo : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Exito(val mensaje: String, val resena: ResenaApi?) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

/**
 * ViewModel de la HU2.03. Las llamadas a la red se hacen con corrutinas
 * (viewModelScope) para no bloquear el hilo principal.
 */
class CalificarAreaViewModel(
    private val repositorio: ResenaRepositorio = ResenaRepositorio()
) : ViewModel() {

    var estado by mutableStateOf<EstadoCalificar>(EstadoCalificar.Cargando)
        private set

    var envio by mutableStateOf<EstadoEnvio>(EstadoEnvio.Inactivo)
        private set

    /** READ: criterios del área y la evaluación del usuario (si ya la hizo). */
    fun cargar(usuario: Usuario, sedeId: String, areaId: String) {
        estado = EstadoCalificar.Cargando
        viewModelScope.launch {
            val criterios = repositorio.criterios(areaId).getOrElse {
                // Si falla la primera llamada no se intenta la segunda
                estado = EstadoCalificar.Error(it.message ?: "Error")
                return@launch
            }
            estado = repositorio.miResena(usuario, sedeId, areaId).fold(
                onSuccess = { EstadoCalificar.Listo(criterios, it) },
                onFailure = { EstadoCalificar.Error(it.message ?: "Error") }
            )
        }
    }

    /** CREATE si es la primera vez en el ciclo, UPDATE si ya existía. */
    fun guardar(datos: DatosResena, existente: ResenaApi?) {
        if (envio == EstadoEnvio.Enviando) return
        envio = EstadoEnvio.Enviando
        viewModelScope.launch {
            val resultado = if (existente == null) repositorio.registrar(datos)
            else repositorio.actualizar(existente.idResena, datos)

            envio = resultado.fold(
                onSuccess = {
                    val mensaje = when {
                        existente != null -> "Tu calificación se actualizó correctamente"
                        datos.areaId == "gestion" -> "Calificación enviada correctamente"
                        datos.sinReciclaje -> "Evaluación guardada. Se registró la alerta ambiental de la sede."
                        else -> "Evaluación guardada correctamente 🎉 ¡Ganaste un boleto!"
                    }
                    EstadoEnvio.Exito(mensaje, it)
                },
                onFailure = { EstadoEnvio.Error(it.message ?: "No se pudo guardar") }
            )
        }
    }

    /** DELETE de la evaluación del usuario. */
    fun eliminar(existente: ResenaApi, usuario: Usuario) {
        if (envio == EstadoEnvio.Enviando) return
        envio = EstadoEnvio.Enviando
        viewModelScope.launch {
            envio = repositorio.eliminar(existente.idResena, usuario).fold(
                onSuccess = { EstadoEnvio.Exito(it.mensaje, null) },
                onFailure = { EstadoEnvio.Error(it.message ?: "No se pudo eliminar") }
            )
        }
    }
}
