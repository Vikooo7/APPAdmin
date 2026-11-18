package com.example.rutalogadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogadmin.data.repository.EnvioRepository
import com.example.rutalogadmin.data.repository.OperacionRepository
import com.example.rutalogadmin.data.repository.ResultadoAccion
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.Courier
import com.example.rutalogadmin.model.CourierConCarga
import com.example.rutalogadmin.model.EntregaFallida
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.example.rutalogadmin.model.calcularCarga
import com.example.rutalogadmin.model.estaCerrado
import com.example.rutalogadmin.model.estadoEnvio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AsignacionUiState(
    /** null = todas las zonas. */
    val zona: String? = null,
    val soloSinCourier: Boolean = true,
    /** Ids locales de los envíos marcados. */
    val seleccion: Set<Int> = emptySet(),
    val courierId: Int? = null,
    val enviando: Boolean = false,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false
)

data class FallidasUiState(
    val cargando: Boolean = false,
    /** true después de la primera carga, haya salido bien o no. */
    val cargada: Boolean = false,
    val lista: List<EntregaFallida> = emptyList(),
    val error: String? = null,
    /** Id remoto de la entrega que se está reprogramando. */
    val procesando: Int? = null,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false
)

/**
 * Operación del día (proyecto final): asignar envíos a couriers por zona y capacidad (HU-O01)
 * y gestionar las entregas fallidas (reprogramar). No conoce Room ni la API.
 */
class OperacionViewModel(
    private val repositorio: OperacionRepository,
    envioRepository: EnvioRepository
) : ViewModel() {

    private val envios: StateFlow<List<Envio>> = envioRepository.observarEnvios()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Couriers con las paradas que tienen en curso, calculadas con los envíos de Room. */
    val couriers: StateFlow<List<CourierConCarga>> =
        combine(repositorio.observarCouriers(), envios) { couriers, lista -> calcularCarga(couriers, lista) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- Asignación ----------

    private val _asignacion = MutableStateFlow(AsignacionUiState())
    val asignacion: StateFlow<AsignacionUiState> = _asignacion.asStateFlow()

    /** Envíos que se pueden asignar: abiertos y sin una entrega fallida por reprogramar. */
    val candidatos: StateFlow<List<Envio>> =
        combine(envios, _asignacion) { lista, estado ->
            lista.filter { envio ->
                !envio.estaCerrado &&
                    envio.estadoEnvio != EstadoEnvio.FALLIDO &&
                    (estado.zona == null || zonaDe(envio) == estado.zona) &&
                    (!estado.soloSinCourier || envio.courierId == null)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun zonaDe(envio: Envio): String? = CatalogoRutas.porNombre(envio.ruta)?.zona

    /** Un envío con un cambio local sin enviar no se asigna por lote: primero hay que sincronizarlo. */
    /** Sincronizado y, si es un envío nuevo, ya pagado con QR (RF-R02: la API rechaza asignar uno sin pago). */
    fun puedeAsignarse(envio: Envio): Boolean = envio.estadoSync == EstadoSincronizacion.SINCRONIZADO && envio.pagoEstado != "POR_PAGAR"

    fun cambiarZona(zona: String?) = _asignacion.update { it.copy(zona = zona, seleccion = emptySet()) }

    fun cambiarSoloSinCourier(valor: Boolean) =
        _asignacion.update { it.copy(soloSinCourier = valor, seleccion = emptySet()) }

    fun alternarSeleccion(envio: Envio) = _asignacion.update {
        it.copy(seleccion = if (envio.id in it.seleccion) it.seleccion - envio.id else it.seleccion + envio.id)
    }

    fun seleccionarTodos() = _asignacion.update { estado ->
        estado.copy(seleccion = candidatos.value.filter(::puedeAsignarse).map { it.id }.toSet())
    }

    fun limpiarSeleccion() = _asignacion.update { it.copy(seleccion = emptySet()) }

    fun elegirCourier(courier: CourierConCarga?) = _asignacion.update { it.copy(courierId = courier?.courier?.id) }

    fun asignar() {
        val estado = _asignacion.value
        val courier = couriers.value.firstOrNull { it.courier.id == estado.courierId }
        val elegidos = envios.value.filter { it.id in estado.seleccion }
        val error = validarAsignacion(elegidos, courier)
        if (error != null) {
            mostrarMensajeAsignacion(error, esError = true)
            return
        }
        viewModelScope.launch {
            _asignacion.update { it.copy(enviando = true) }
            val resultado = repositorio.asignar(courier!!.courier, elegidos.map { it.idRemoto })
            _asignacion.update {
                when (resultado) {
                    is ResultadoAccion.Exito ->
                        it.copy(enviando = false, seleccion = emptySet(), mensaje = resultado.mensaje, mensajeEsError = false)
                    is ResultadoAccion.Error ->
                        it.copy(enviando = false, mensaje = resultado.mensaje, mensajeEsError = true)
                }
            }
        }
    }

    fun mensajeAsignacionMostrado() = _asignacion.update { it.copy(mensaje = null) }

    private fun mostrarMensajeAsignacion(texto: String, esError: Boolean) =
        _asignacion.update { it.copy(mensaje = texto, mensajeEsError = esError) }

    // ---------- Entregas fallidas ----------

    private val _fallidas = MutableStateFlow(FallidasUiState())
    val fallidas: StateFlow<FallidasUiState> = _fallidas.asStateFlow()

    fun cargarFallidas() {
        viewModelScope.launch {
            _fallidas.update { it.copy(cargando = true, error = null) }
            repositorio.cargarFallidas()
                .onSuccess { lista -> _fallidas.update { it.copy(cargando = false, cargada = true, lista = lista) } }
                .onFailure { e -> _fallidas.update { it.copy(cargando = false, cargada = true, error = e.message) } }
        }
    }

    /** [courier] null mantiene el mismo courier. */
    fun reprogramar(fallida: EntregaFallida, courier: Courier?) {
        viewModelScope.launch {
            _fallidas.update { it.copy(procesando = fallida.idRemoto) }
            val resultado = repositorio.reprogramar(fallida, courier)
            _fallidas.update {
                when (resultado) {
                    is ResultadoAccion.Exito -> it.copy(procesando = null, mensaje = resultado.mensaje, mensajeEsError = false)
                    is ResultadoAccion.Error -> it.copy(procesando = null, mensaje = resultado.mensaje, mensajeEsError = true)
                }
            }
            if (resultado is ResultadoAccion.Exito) cargarFallidas()
        }
    }

    fun mensajeFallidasMostrado() = _fallidas.update { it.copy(mensaje = null) }

    companion object {
        /** Validación antes de llamar a la API; el servidor vuelve a validar la capacidad. */
        fun validarAsignacion(elegidos: List<Envio>, courier: CourierConCarga?): String? {
            val nuevos = elegidos.count { it.courierId != courier?.courier?.id }
            return when {
                elegidos.isEmpty() -> "Marca al menos un envío."
                courier == null -> "Elige el courier que recibirá los envíos."
                elegidos.any { it.estadoSync != EstadoSincronizacion.SINCRONIZADO } ->
                    "Hay envíos con cambios sin enviar: sincronízalos antes de asignarlos."
                nuevos == 0 -> "Esos envíos ya son de ${courier.courier.nombre}."
                nuevos > courier.disponibles ->
                    "${courier.courier.nombre} tiene ${courier.activos} de ${courier.courier.capacidad} paradas: " +
                        "solo puede recibir ${courier.disponibles} más."
                else -> null
            }
        }

        fun factory(
            repositorio: OperacionRepository,
            envioRepository: EnvioRepository
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { OperacionViewModel(repositorio, envioRepository) }
            }
    }
}
