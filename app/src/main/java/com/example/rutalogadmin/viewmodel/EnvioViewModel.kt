package com.example.rutalogadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogadmin.data.repository.EnvioRepository
import com.example.rutalogadmin.data.repository.RutaRepository
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.NumeroGuia
import com.example.rutalogadmin.model.ReglasOperacion
import com.example.rutalogadmin.model.Ruta
import com.example.rutalogadmin.model.RutaConCarga
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del negocio: envíos (RF06 a RF09) y rutas activas (RF10).
 * No conoce Room ni la API: pide todo a los repositorios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EnvioViewModel(
    private val envioRepository: EnvioRepository,
    private val rutaRepository: RutaRepository,
    /** Deja programado el SyncWorker para enviar la cola cuando haya red. */
    private val programarSincronizacion: () -> Unit
) : ViewModel() {

    // ---------- Envíos ----------

    /** RF06: lista completa leída de Room. */
    val envios: StateFlow<List<Envio>> = envioRepository.observarEnvios()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto.asStateFlow()

    private val _filtroEstado = MutableStateFlow<EstadoEnvio?>(null)
    val filtroEstado: StateFlow<EstadoEnvio?> = _filtroEstado.asStateFlow()

    private val _filtroRuta = MutableStateFlow<String?>(null)
    val filtroRuta: StateFlow<String?> = _filtroRuta.asStateFlow()

    /** RF07: Room filtra por estado y ruta; la búsqueda por guía o transportista se aplica encima. */
    val enviosFiltrados: StateFlow<List<Envio>> =
        combine(_filtroEstado, _filtroRuta) { estado, ruta -> estado to ruta }
            .flatMapLatest { (estado, ruta) -> envioRepository.filtrar(estado?.codigo, ruta) }
            .combine(_texto) { lista, texto ->
                val buscado = texto.trim()
                val guiaBuscada = NumeroGuia.normalizar(buscado)
                if (buscado.isEmpty()) {
                    lista
                } else {
                    lista.filter { envio ->
                        (guiaBuscada.isNotEmpty() && NumeroGuia.normalizar(envio.numeroGuia).contains(guiaBuscada)) ||
                            envio.transportistaAsignado?.contains(buscado, ignoreCase = true) == true
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun buscarTexto(texto: String) { _texto.value = texto }
    fun filtrarPorEstado(estado: EstadoEnvio?) { _filtroEstado.value = estado }
    fun filtrarPorRuta(ruta: String?) { _filtroRuta.value = ruta }

    /** Deja la lista con un solo filtro, por ejemplo al llegar desde el Inicio o desde Rutas. */
    fun mostrarSolo(estado: EstadoEnvio? = null, ruta: String? = null) {
        _texto.value = ""
        _filtroEstado.value = estado
        _filtroRuta.value = ruta
    }

    fun limpiarFiltros() = mostrarSolo()

    fun obtenerPorId(id: Int): Flow<Envio?> = envioRepository.observarPorId(id)

    /**
     * RF08 + RF09: valida con [ReglasOperacion] y guarda el cambio en Room junto con su
     * operación pendiente. Funciona igual con o sin conexión.
     */
    fun guardarOperacion(
        envio: Envio,
        nuevoEstado: EstadoEnvio,
        transportista: String?,
        onError: (String) -> Unit,
        onGuardado: () -> Unit
    ) {
        val error = ReglasOperacion.validar(envio, nuevoEstado, transportista)
        if (error != null) {
            onError(error)
            return
        }
        viewModelScope.launch {
            envioRepository.guardarOperacion(envio.id, nuevoEstado, transportista)
            programarSincronizacion()
            onGuardado()
        }
    }

    // ---------- Rutas ----------

    /** Rutas guardadas en Room, para el filtro por ruta de RF07. */
    val rutas: StateFlow<List<Ruta>> = rutaRepository.observarRutas()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** RF10: rutas con la carga que llevan; se recalcula cuando cambia un envío. */
    val rutasConCarga: StateFlow<List<RutaConCarga>> = rutaRepository.observarConCarga()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _soloActivas = MutableStateFlow(true)
    val soloActivas: StateFlow<Boolean> = _soloActivas.asStateFlow()

    val rutasVisibles: StateFlow<List<RutaConCarga>> =
        combine(rutasConCarga, _soloActivas) { lista, soloActivas ->
            if (soloActivas) lista.filter { it.esActiva } else lista
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun mostrarSoloActivas(valor: Boolean) { _soloActivas.value = valor }

    fun obtenerRutaPorNombre(nombre: String): Flow<Ruta?> = rutaRepository.observarPorNombre(nombre)

    companion object {
        fun factory(
            envioRepository: EnvioRepository,
            rutaRepository: RutaRepository,
            programarSincronizacion: () -> Unit
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { EnvioViewModel(envioRepository, rutaRepository, programarSincronizacion) }
            }
    }
}
