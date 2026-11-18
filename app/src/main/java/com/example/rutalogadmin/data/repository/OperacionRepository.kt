package com.example.rutalogadmin.data.repository

import androidx.room.withTransaction
import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.mapper.aEntidad
import com.example.rutalogadmin.data.mapper.aModelo
import com.example.rutalogadmin.data.remote.ResultadoApi
import com.example.rutalogadmin.data.remote.api.ApiService
import com.example.rutalogadmin.data.remote.dto.AsignacionRequest
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.ReprogramarRequest
import com.example.rutalogadmin.data.remote.llamarApi
import com.example.rutalogadmin.model.Courier
import com.example.rutalogadmin.model.EntregaFallida
import com.example.rutalogadmin.model.EstadoSincronizacion
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Resultado de una acción que se hace en línea (asignar o reprogramar). */
sealed interface ResultadoAccion {
    data class Exito(val mensaje: String) : ResultadoAccion
    data class Error(val mensaje: String) : ResultadoAccion
}

/**
 * Operación del día en la App Admin: couriers, asignación por lote y entregas fallidas.
 *
 * Los couriers se leen de Room (se descargan al sincronizar) para elegirlos aunque no haya red.
 * Asignar por lote y reprogramar se hacen en línea: el servidor valida en ese momento la
 * capacidad de cada courier y el estado real del envío, y la respuesta se guarda en Room.
 */
class OperacionRepository(
    private val db: AppDatabase,
    private val api: ApiService
) {

    private val envioDao = db.envioDao()
    private val operacionDao = db.operacionPendienteDao()

    fun observarCouriers(): Flow<List<Courier>> =
        db.courierDao().observarTodos().map { lista -> lista.map { it.aModelo() } }

    /** HU-O01: asigna los envíos (ids remotos) al courier, todo o nada. */
    suspend fun asignar(courier: Courier, idsRemotos: List<Int>): ResultadoAccion {
        val pedido = AsignacionRequest(
            uuidOperacion = UUID.randomUUID().toString(),
            courierId = courier.id,
            envioIds = idsRemotos
        )
        return when (val resultado = llamarApi { api.asignar(pedido) }) {
            is ResultadoApi.Exito -> {
                guardarEnRoom(resultado.valor.envios)
                ResultadoAccion.Exito(
                    "${resultado.valor.envios.size} envío(s) asignado(s) a ${courier.nombre}."
                )
            }
            is ResultadoApi.Rechazo -> ResultadoAccion.Error(resultado.mensaje)
            is ResultadoApi.Fallo -> ResultadoAccion.Error(sinConexion(resultado))
        }
    }

    /** Entregas fallidas con su historial; se consultan en línea cada vez que se abre la pantalla. */
    suspend fun cargarFallidas(): Result<List<EntregaFallida>> =
        when (val resultado = llamarApi { api.obtenerFallidas() }) {
            is ResultadoApi.Exito -> Result.success(resultado.valor.datos.map { it.aModelo() })
            is ResultadoApi.Rechazo -> Result.failure(Exception(resultado.mensaje))
            is ResultadoApi.Fallo -> Result.failure(Exception(sinConexion(resultado)))
        }

    /** La entrega fallida vuelve a salir a reparto. [courier] null mantiene el mismo courier. */
    suspend fun reprogramar(fallida: EntregaFallida, courier: Courier?): ResultadoAccion {
        val pedido = ReprogramarRequest(
            uuidOperacion = UUID.randomUUID().toString(),
            version = fallida.version,
            courierId = courier?.id
        )
        return when (val resultado = llamarApi { api.reprogramar(fallida.idRemoto, pedido) }) {
            is ResultadoApi.Exito -> {
                guardarEnRoom(listOf(resultado.valor))
                ResultadoAccion.Exito(
                    if (courier == null) {
                        "Guía ${fallida.numeroGuia} reprogramada."
                    } else {
                        "Guía ${fallida.numeroGuia} reprogramada con ${courier.nombre}."
                    }
                )
            }
            is ResultadoApi.Rechazo -> ResultadoAccion.Error(resultado.mensaje)
            is ResultadoApi.Fallo -> ResultadoAccion.Error(sinConexion(resultado))
        }
    }

    /** Guarda lo que devolvió el servidor, sin pisar envíos que tienen cambios locales por enviar. */
    private suspend fun guardarEnRoom(remotos: List<EnvioDto>) {
        db.withTransaction {
            val conCambiosLocales = operacionDao.todas().map { it.idEntidadLocal }.toSet()
            remotos.forEach { remoto ->
                val local = envioDao.porIdRemoto(remoto.id)
                when {
                    local == null -> envioDao.insertar(remoto.aEntidad())
                    local.id in conCambiosLocales || local.estadoSync != EstadoSincronizacion.SINCRONIZADO.name -> Unit
                    else -> envioDao.actualizar(remoto.aEntidad(local.id))
                }
            }
        }
    }

    private fun sinConexion(fallo: ResultadoApi.Fallo): String =
        if (fallo.sinConexion) "Sin conexión: esta acción necesita internet." else fallo.mensaje
}
