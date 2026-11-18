package com.example.rutalogadmin.data.repository

import com.example.rutalogadmin.data.remote.ResultadoApi
import com.example.rutalogadmin.data.remote.api.ApiService
import com.example.rutalogadmin.data.remote.dto.ConfirmacionRequest
import com.example.rutalogadmin.data.remote.dto.ConfirmacionResponse
import com.example.rutalogadmin.data.remote.dto.RiesgoResponse
import com.example.rutalogadmin.data.remote.llamarApi
import java.util.UUID

/**
 * ML-01 (RF-O02): el riesgo se calcula en el servidor con el modelo del notebook y el aviso al destinatario
 * sale desde allí; por eso se hace en línea y no pasa por la cola sin conexión.
 */
class RiesgoRepository(private val api: ApiService) {

    suspend fun riesgo(fecha: String? = null): Result<RiesgoResponse> = aResult(llamarApi { api.obtenerRiesgo(fecha) })

    /** [uuid] uno por pedido: si la red falla y se reintenta, el destinatario no recibe dos SMS. */
    suspend fun pedirConfirmacion(envioId: Int, uuid: String = UUID.randomUUID().toString()): Result<ConfirmacionResponse> =
        aResult(llamarApi { api.pedirConfirmacion(envioId, ConfirmacionRequest(uuid)) })

    suspend fun registrarConfirmacion(envioId: Int, uuid: String = UUID.randomUUID().toString()): Result<ConfirmacionResponse> =
        aResult(llamarApi { api.registrarConfirmacion(envioId, ConfirmacionRequest(uuid)) })

    private fun <T> aResult(r: ResultadoApi<T>): Result<T> = when (r) {
        is ResultadoApi.Exito -> Result.success(r.valor)
        is ResultadoApi.Rechazo -> Result.failure(Exception(r.mensaje))
        is ResultadoApi.Fallo -> Result.failure(
            Exception(if (r.sinConexion) "Sin conexión: el riesgo se calcula en el servidor." else r.mensaje)
        )
    }
}
