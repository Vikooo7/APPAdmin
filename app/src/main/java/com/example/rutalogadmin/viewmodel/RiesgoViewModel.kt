package com.example.rutalogadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogadmin.data.remote.dto.RiesgoEntregaDto
import com.example.rutalogadmin.data.remote.dto.RiesgoResponse
import com.example.rutalogadmin.data.remote.dto.TableroResponse
import com.example.rutalogadmin.data.remote.dto.PronosticoResponse
import com.example.rutalogadmin.data.repository.RiesgoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RiesgoUiState(
    val cargando: Boolean = false,
    val respuesta: RiesgoResponse? = null,
    /** true = solo las de riesgo alto (lo que el operador debe atender). */
    val soloAltos: Boolean = true,
    /** Id remoto del envío al que se le está pidiendo o registrando la confirmación. */
    val procesando: Int? = null,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false
) {
    val visibles: List<RiesgoEntregaDto>
        get() = respuesta?.datos.orEmpty().filter { !soloAltos || it.riesgoAlto }

    /** Para marcar en Asignación las entregas de riesgo alto (por número de guía). */
    val altosPorGuia: Map<String, RiesgoEntregaDto>
        get() = respuesta?.datos.orEmpty().filter { it.riesgoAlto }.associateBy { it.envio.numeroGuia }
}

data class TableroUiState(
    val cargando: Boolean = false,
    val tablero: TableroResponse? = null,
    val error: String? = null
)

/**
 * Fase BI y ML en la App Admin: lista de riesgo y aviso previo al destinatario (ML-01, RF-O02, HU-O01)
 * y tablero de la operación con comparación semanal (RF-O05, HU-O03).
 */
class RiesgoViewModel(private val repositorio: RiesgoRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RiesgoUiState())
    val estado: StateFlow<RiesgoUiState> = _estado.asStateFlow()

    private val _tablero = MutableStateFlow(TableroUiState())
    val tablero: StateFlow<TableroUiState> = _tablero.asStateFlow()

    /** ML-03: pronóstico de mañana para Asignar envíos. Si falla, simplemente no se muestra. */
    private val _pronostico = MutableStateFlow<PronosticoResponse?>(null)
    val pronostico: StateFlow<PronosticoResponse?> = _pronostico.asStateFlow()

    fun cargarPronostico() {
        viewModelScope.launch { repositorio.pronostico().onSuccess { p -> _pronostico.value = p } }
    }

    /** [semana]: cualquier fecha AAAA-MM-DD de la semana; null = la semana actual. */
    fun cargarTablero(semana: String? = _tablero.value.tablero?.semana?.inicio) {
        viewModelScope.launch {
            _tablero.update { it.copy(cargando = true, error = null) }
            repositorio.tablero(semana)
                .onSuccess { t -> _tablero.update { it.copy(cargando = false, tablero = t) } }
                .onFailure { e -> _tablero.update { it.copy(cargando = false, error = e.message) } }
        }
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            repositorio.riesgo()
                .onSuccess { r -> _estado.update { it.copy(cargando = false, respuesta = r) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, mensaje = e.message, mensajeEsError = true) } }
        }
    }

    fun cambiarSoloAltos(valor: Boolean) = _estado.update { it.copy(soloAltos = valor) }

    /** Envía el SMS simulado que pide al destinatario confirmar el horario. */
    fun pedirConfirmacion(entrega: RiesgoEntregaDto) = accion(entrega) {
        repositorio.pedirConfirmacion(entrega.envio.id).map { r ->
            "SMS enviado a ${r.aviso?.destino ?: "el destinatario"}: pide confirmar el horario de ${r.numeroGuia}."
        }
    }

    /** El destinatario respondió "1": habrá alguien. Con eso el modelo baja el riesgo. */
    fun registrarConfirmacion(entrega: RiesgoEntregaDto) = accion(entrega) {
        repositorio.registrarConfirmacion(entrega.envio.id).map { r -> "${r.numeroGuia}: horario confirmado por el destinatario." }
    }

    private fun accion(entrega: RiesgoEntregaDto, llamada: suspend () -> Result<String>) {
        viewModelScope.launch {
            _estado.update { it.copy(procesando = entrega.envio.id, mensaje = null) }
            llamada()
                .onSuccess { texto ->
                    _estado.update { it.copy(procesando = null, mensaje = texto, mensajeEsError = false) }
                    cargar()
                }
                .onFailure { e -> _estado.update { it.copy(procesando = null, mensaje = e.message, mensajeEsError = true) } }
        }
    }

    fun mensajeMostrado() = _estado.update { it.copy(mensaje = null) }

    companion object {
        fun factory(repositorio: RiesgoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { RiesgoViewModel(repositorio) }
        }
    }
}
