package com.example.rutalogadmin.data.repository

import androidx.room.withTransaction
import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.local.entities.EnvioEntity
import com.example.rutalogadmin.data.local.entities.OperacionPendienteEntity
import com.example.rutalogadmin.data.local.entities.TipoOperacion
import com.example.rutalogadmin.data.mapper.aModelo
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Datos de una operación tal como se guardan en el campo payload (JSON).
 * Los valores "anteriores" son los que tenía el envío antes del primer cambio sin enviar:
 * sirven para devolverlo a ese punto si el operador descarta un cambio rechazado.
 */
data class PayloadEnvio(
    val estado: String,
    val transportistaAsignado: String?,
    val estadoAnterior: String,
    val transportistaAnterior: String?
)

/**
 * Envíos del operador. Las pantallas siempre leen de Room; cada escritura se guarda en Room
 * junto con su operación pendiente, en una sola transacción (RF12). El envío a la API lo
 * hace después SyncRepository, haya o no conexión en este momento.
 */
class EnvioRepository(private val db: AppDatabase) {

    private val envioDao = db.envioDao()
    private val operacionDao = db.operacionPendienteDao()
    private val gson = Gson()

    /** RF06 */
    fun observarEnvios(): Flow<List<Envio>> =
        envioDao.obtenerTodos().map { lista -> lista.map { it.aModelo() } }

    /** RF07: un parámetro null significa "todos". */
    fun filtrar(estado: String?, ruta: String?): Flow<List<Envio>> =
        envioDao.filtrar(estado, ruta).map { lista -> lista.map { it.aModelo() } }

    fun observarPorId(id: Int): Flow<Envio?> =
        envioDao.observarPorId(id).map { it?.aModelo() }

    /** RF08 + RF09: guarda el nuevo estado y el transportista, y deja el cambio en la cola. */
    suspend fun guardarOperacion(idLocal: Int, nuevoEstado: EstadoEnvio, transportista: String?) {
        db.withTransaction {
            val actual = envioDao.porId(idLocal) ?: return@withTransaction
            val cambiado = actual.copy(
                estado = nuevoEstado.codigo,
                transportistaAsignado = transportista,
                estadoSync = EstadoSincronizacion.PENDIENTE.name,
                mensajeError = null
            )
            envioDao.actualizar(cambiado)
            encolar(anterior = actual, cambiado = cambiado)
        }
    }

    /**
     * Agrega la operación a la cola. Si ya hay una para el mismo envío que aún no se envió
     * (o que el servidor rechazó), se reutiliza con los datos nuevos: así el servidor recibe
     * una sola operación con el estado final.
     */
    private suspend fun encolar(anterior: EnvioEntity, cambiado: EnvioEntity) {
        val reutilizable = operacionDao.deEntidad(cambiado.id).lastOrNull { operacion ->
            operacion.estado != EstadoSincronizacion.ENVIANDO.name &&
                (operacion.intentos == 0 || operacion.estado == EstadoSincronizacion.ERROR.name)
        }
        if (reutilizable != null) {
            // Se conserva el punto de partida de la operación original, no el del último cambio.
            val original = gson.fromJson(reutilizable.payload, PayloadEnvio::class.java)
            operacionDao.actualizar(
                reutilizable.copy(
                    payload = payload(cambiado, original.estadoAnterior, original.transportistaAnterior),
                    estado = EstadoSincronizacion.PENDIENTE.name,
                    mensajeError = null
                )
            )
        } else {
            operacionDao.insertar(
                OperacionPendienteEntity(
                    uuidOperacion = UUID.randomUUID().toString(),
                    entidad = "envios",
                    idEntidadLocal = cambiado.id,
                    tipoOperacion = TipoOperacion.ACTUALIZAR,
                    payload = payload(cambiado, anterior.estado, anterior.transportistaAsignado),
                    fechaRegistro = System.currentTimeMillis(),
                    estado = EstadoSincronizacion.PENDIENTE.name
                )
            )
        }
    }

    private fun payload(envio: EnvioEntity, estadoAnterior: String, transportistaAnterior: String?): String =
        gson.toJson(PayloadEnvio(envio.estado, envio.transportistaAsignado, estadoAnterior, transportistaAnterior))
}
