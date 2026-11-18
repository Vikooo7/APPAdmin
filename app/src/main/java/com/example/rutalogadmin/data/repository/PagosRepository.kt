package com.example.rutalogadmin.data.repository

import com.example.rutalogadmin.data.remote.ResultadoApi
import com.example.rutalogadmin.data.remote.api.ApiService
import com.example.rutalogadmin.data.remote.dto.AprobarRequest
import com.example.rutalogadmin.data.remote.dto.FacturacionMesDto
import com.example.rutalogadmin.data.remote.dto.LiquidacionDto
import com.example.rutalogadmin.data.remote.dto.LiquidacionesResponse
import com.example.rutalogadmin.data.remote.dto.SolicitudCreditoDto
import com.example.rutalogadmin.data.remote.dto.SolicitudCreditoRequest
import com.example.rutalogadmin.data.remote.llamarApi
import java.util.UUID

/**
 * Liquidaciones a remitentes (RF-O03, RF-O04) y Mi crédito (RF-O06). Son operaciones con dinero:
 * se hacen en línea y las confirma Banco Andino; no se guardan en la cola sin conexión.
 */
class PagosRepository(private val api: ApiService) {

    suspend fun liquidaciones(): Result<LiquidacionesResponse> = aResult(llamarApi { api.obtenerLiquidaciones() })

    suspend fun calcular(): Result<List<LiquidacionDto>> = aResult(llamarApi { api.calcularLiquidaciones() }).map { it.datos }

    /** [uuid] se crea una vez por aprobación: si se reintenta, el banco no transfiere dos veces. */
    suspend fun aprobar(id: Int, uuid: String = UUID.randomUUID().toString()): Result<LiquidacionDto> =
        aResult(llamarApi { api.aprobarLiquidacion(id, AprobarRequest(uuid)) })

    suspend fun facturacion(): Result<List<FacturacionMesDto>> = aResult(llamarApi { api.obtenerFacturacion() }).map { it.datos }

    suspend fun solicitudes(): Result<List<SolicitudCreditoDto>> = aResult(llamarApi { api.obtenerSolicitudesCredito() }).map { it.datos }

    suspend fun solicitar(monto: Double, plazoMeses: Int, proposito: String): Result<SolicitudCreditoDto> =
        aResult(llamarApi { api.solicitarCredito(SolicitudCreditoRequest(monto, plazoMeses, proposito, autorizo = true)) })

    private fun <T> aResult(r: ResultadoApi<T>): Result<T> = when (r) {
        is ResultadoApi.Exito -> Result.success(r.valor)
        is ResultadoApi.Rechazo -> Result.failure(Exception(r.mensaje))
        is ResultadoApi.Fallo -> Result.failure(
            Exception(if (r.sinConexion) "Sin conexión: las operaciones con el banco se hacen en línea." else r.mensaje)
        )
    }
}
