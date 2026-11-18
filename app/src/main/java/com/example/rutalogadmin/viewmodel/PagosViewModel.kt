package com.example.rutalogadmin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogadmin.data.remote.dto.FacturacionMesDto
import com.example.rutalogadmin.data.remote.dto.LiquidacionDto
import com.example.rutalogadmin.data.remote.dto.PendienteRemitenteDto
import com.example.rutalogadmin.data.remote.dto.SolicitudCreditoDto
import com.example.rutalogadmin.data.repository.PagosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiquidacionUiState(
    val cargando: Boolean = false,
    val pendiente: List<PendienteRemitenteDto> = emptyList(),
    val liquidaciones: List<LiquidacionDto> = emptyList(),
    /** Id de la liquidación que se está aprobando (para deshabilitar su botón). */
    val aprobando: Int? = null,
    val calculando: Boolean = false,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false
)

data class CreditoUiState(
    val cargando: Boolean = false,
    val facturacion: List<FacturacionMesDto> = emptyList(),
    val solicitudes: List<SolicitudCreditoDto> = emptyList(),
    val enviando: Boolean = false,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false
)

class PagosViewModel(private val repositorio: PagosRepository) : ViewModel() {

    private val _liquidacion = MutableStateFlow(LiquidacionUiState())
    val liquidacion: StateFlow<LiquidacionUiState> = _liquidacion.asStateFlow()

    private val _credito = MutableStateFlow(CreditoUiState())
    val credito: StateFlow<CreditoUiState> = _credito.asStateFlow()

    fun cargarLiquidaciones() {
        viewModelScope.launch {
            _liquidacion.update { it.copy(cargando = true) }
            repositorio.liquidaciones()
                .onSuccess { r -> _liquidacion.update { it.copy(cargando = false, pendiente = r.pendiente.orEmpty(), liquidaciones = r.datos) } }
                .onFailure { e -> _liquidacion.update { it.copy(cargando = false, mensaje = e.message, mensajeEsError = true) } }
        }
    }

    /** RF-O03: corte de liquidación con lo confirmado por el banco hasta ahora. */
    fun calcular() {
        viewModelScope.launch {
            _liquidacion.update { it.copy(calculando = true, mensaje = null) }
            repositorio.calcular()
                .onSuccess { creadas ->
                    _liquidacion.update {
                        it.copy(
                            calculando = false,
                            mensaje = if (creadas.isEmpty()) "No hay cobros confirmados pendientes de liquidar." else "${creadas.size} liquidación(es) calculada(s).",
                            mensajeEsError = false
                        )
                    }
                    cargarLiquidaciones()
                }
                .onFailure { e -> _liquidacion.update { it.copy(calculando = false, mensaje = e.message, mensajeEsError = true) } }
        }
    }

    /** RF-O04: solo el administrador. El banco transfiere el neto a la cuenta del remitente. */
    fun aprobar(liquidacion: LiquidacionDto) {
        viewModelScope.launch {
            _liquidacion.update { it.copy(aprobando = liquidacion.id, mensaje = null) }
            repositorio.aprobar(liquidacion.id)
                .onSuccess { l ->
                    _liquidacion.update {
                        it.copy(aprobando = null, mensaje = "Transferido a ${l.remitente}: operación ${l.nroOperacion}.", mensajeEsError = false)
                    }
                    cargarLiquidaciones()
                }
                .onFailure { e -> _liquidacion.update { it.copy(aprobando = null, mensaje = e.message, mensajeEsError = true) } }
        }
    }

    fun cargarCredito() {
        viewModelScope.launch {
            _credito.update { it.copy(cargando = true) }
            val facturacion = repositorio.facturacion()
            val solicitudes = repositorio.solicitudes()
            _credito.update {
                it.copy(
                    cargando = false,
                    facturacion = facturacion.getOrNull() ?: it.facturacion,
                    solicitudes = solicitudes.getOrNull() ?: it.solicitudes,
                    mensaje = (facturacion.exceptionOrNull() ?: solicitudes.exceptionOrNull())?.message ?: it.mensaje,
                    mensajeEsError = facturacion.isFailure || solicitudes.isFailure
                )
            }
        }
    }

    /** RF-O06: se envía solo con autorización expresa de compartir la facturación por mes (RN-07). */
    fun solicitar(montoTexto: String, plazoTexto: String, proposito: String, autorizo: Boolean) {
        val error = validarSolicitud(montoTexto, plazoTexto, autorizo)
        if (error != null) {
            _credito.update { it.copy(mensaje = error, mensajeEsError = true) }
            return
        }
        viewModelScope.launch {
            _credito.update { it.copy(enviando = true, mensaje = null) }
            repositorio.solicitar(montoTexto.replace(",", "").toDouble(), plazoTexto.toInt(), proposito.ifBlank { "Furgoneta para reparto" })
                .onSuccess { s ->
                    _credito.update { it.copy(enviando = false, mensaje = "Solicitud ${s.codigoBanco} enviada: la decide el comité del banco.", mensajeEsError = false) }
                    cargarCredito()
                }
                .onFailure { e -> _credito.update { it.copy(enviando = false, mensaje = e.message, mensajeEsError = true) } }
        }
    }

    fun mensajeMostrado() {
        _liquidacion.update { it.copy(mensaje = null) }
        _credito.update { it.copy(mensaje = null) }
    }

    companion object {
        /** Mismas reglas que la API y el banco: monto > 0, plazo de 6 a 84 meses y autorización. */
        fun validarSolicitud(montoTexto: String, plazoTexto: String, autorizo: Boolean): String? {
            val monto = montoTexto.replace(",", "").toDoubleOrNull()
            val plazo = plazoTexto.toIntOrNull()
            return when {
                monto == null || monto <= 0 -> "Escribe un monto mayor que 0."
                monto > 1_000_000 -> "El monto supera el máximo permitido."
                plazo == null || plazo !in 6..84 -> "El plazo va de 6 a 84 meses."
                !autorizo -> "Autoriza compartir solo la facturación por mes y el número de envíos."
                else -> null
            }
        }

        fun factory(repositorio: PagosRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { PagosViewModel(repositorio) }
        }
    }
}
