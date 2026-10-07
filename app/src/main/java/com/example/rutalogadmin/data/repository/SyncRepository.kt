package com.example.rutalogadmin.data.repository

import androidx.compose.foundation.layout.size
import androidx.room.withTransaction
import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.local.dao.OperacionConGuia
import com.example.rutalogadmin.data.local.entities.EnvioEntity
import com.example.rutalogadmin.data.local.entities.OperacionPendienteEntity
import com.example.rutalogadmin.data.local.entities.SyncMetadataEntity
import com.example.rutalogadmin.data.mapper.aEntidad
import com.example.rutalogadmin.data.remote.ResultadoApi
import com.example.rutalogadmin.data.remote.api.ApiService
import com.example.rutalogadmin.data.remote.dto.ActualizarEnvioRequest
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.RutaDto
import com.example.rutalogadmin.data.remote.llamarApi
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resumen de una sincronización, para mostrarlo en pantalla. */
data class ResultadoSync(
    val enviadas: Int = 0,
    val rechazadas: Int = 0,
    val descargados: Int = 0,
    /** null si todo salió bien; si no, el motivo por el que se detuvo. */
    val error: String? = null
)

/**
 * Sincronización con la API REST (RF11 a RF15):
 * 1. Envía, en orden, las operaciones de la cola local. Cada una lleva su UUID, así el
 *    servidor no la aplica dos veces aunque se reenvíe.
 * 2. Descarga las rutas y los envíos y actualiza Room, sin tocar los envíos que todavía
 *    tienen cambios locales por enviar.
 *
 * Si el servidor rechaza una operación (validación o conflicto), el envío se conserva con
 * estado ERROR y el motivo, para que el operador decida si reintenta o descarta su cambio.
 */
class SyncRepository(
    private val db: AppDatabase,
    private val api: ApiService
) {

    private val envioDao = db.envioDao()
    private val rutaDao = db.rutaDao()
    private val operacionDao = db.operacionPendienteDao()
    private val gson = Gson()

    /** Evita que el botón manual y el SyncWorker sincronicen al mismo tiempo. */
    private val candado = Mutex()

    fun observarOperaciones(): Flow<List<OperacionConGuia>> = operacionDao.observarConGuia()

    fun observarUltimaSincronizacion(): Flow<Long?> =
        operacionDao.observarMetadata(RECURSO_ENVIOS).map { it?.ultimaSincronizacionExitosa }

    suspend fun sincronizar(): ResultadoSync = candado.withLock {
        // Si la app se cerró a mitad de un envío, esas operaciones vuelven a la cola.
        operacionDao.recuperarInterrumpidas()
        envioDao.recuperarInterrumpidos()

        var enviadas = 0
        var rechazadas = 0

        for (operacion in operacionDao.porEnviar()) {
            val envio = envioDao.porId(operacion.idEntidadLocal)
            if (envio == null) {
                operacionDao.eliminar(operacion)
                continue
            }

            marcar(operacion, EstadoSincronizacion.ENVIANDO)

            when (val resultado = enviar(operacion, envio)) {
                is ResultadoApi.Exito -> {
                    aplicarExito(operacion, resultado.valor)
                    enviadas++
                }

                is ResultadoApi.Rechazo -> {
                    if (resultado.http == 401) {
                        // Sesión vencida: no es un rechazo del cambio, que sigue en la cola.
                        marcar(operacion, EstadoSincronizacion.PENDIENTE)
                        return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje)
                    }
                    aplicarRechazo(operacion, resultado)
                    rechazadas++
                }

                is ResultadoApi.Fallo -> {
                    // Sin conexión o servidor caído: la operación sigue en la cola para otro intento.
                    db.withTransaction {
                        operacionDao.actualizar(
                            operacion.copy(
                                estado = EstadoSincronizacion.PENDIENTE.name,
                                intentos = operacion.intentos + 1,
                                mensajeError = resultado.mensaje
                            )
                        )
                        envioDao.porId(envio.id)?.let {
                            envioDao.actualizar(it.copy(estadoSync = EstadoSincronizacion.PENDIENTE.name))
                        }
                    }
                    return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje)
                }
            }
        }

        descargar(enviadas, rechazadas)
    }

    /** El operador quiere volver a intentarlo: la operación regresa a la cola. */
    suspend fun reintentar(idOperacion: Int) {
        db.withTransaction {
            val operacion = operacionDao.porId(idOperacion) ?: return@withTransaction
            operacionDao.actualizar(
                operacion.copy(estado = EstadoSincronizacion.PENDIENTE.name, mensajeError = null)
            )
            envioDao.porId(operacion.idEntidadLocal)?.let {
                envioDao.actualizar(it.copy(estadoSync = EstadoSincronizacion.PENDIENTE.name, mensajeError = null))
            }
        }
    }

    /**
     * El operador renuncia a su cambio: el envío vuelve a como estaba antes de modificarlo.
     * La siguiente descarga lo deja igual que en el servidor.
     */
    suspend fun descartar(idOperacion: Int) {
        db.withTransaction {
            val operacion = operacionDao.porId(idOperacion) ?: return@withTransaction
            val datos = leerPayload(operacion)
            operacionDao.eliminarDeEntidad(operacion.idEntidadLocal)
            val envio = envioDao.porId(operacion.idEntidadLocal) ?: return@withTransaction
            envioDao.actualizar(
                envio.copy(
                    estado = datos?.estadoAnterior ?: envio.estado,
                    transportistaAsignado = if (datos != null) datos.transportistaAnterior else envio.transportistaAsignado,
                    estadoSync = EstadoSincronizacion.SINCRONIZADO.name,
                    mensajeError = null
                )
            )
        }
    }

    private suspend fun marcar(operacion: OperacionPendienteEntity, estado: EstadoSincronizacion) {
        db.withTransaction {
            operacionDao.actualizar(operacion.copy(estado = estado.name))
            envioDao.porId(operacion.idEntidadLocal)?.let {
                envioDao.actualizar(it.copy(estadoSync = estado.name))
            }
        }
    }

    /** RF08 + RF09: envía el cambio de estado y transportista con la versión que conocía el teléfono. */
    private suspend fun enviar(operacion: OperacionPendienteEntity, envio: EnvioEntity): ResultadoApi<EnvioDto> {
        val datos = leerPayload(operacion)
        return llamarApi {
            api.actualizarEnvio(
                envio.idRemoto,
                ActualizarEnvioRequest(
                    uuidOperacion = operacion.uuidOperacion,
                    estado = datos?.estado ?: envio.estado,
                    transportistaAsignado = if (datos != null) datos.transportistaAsignado else envio.transportistaAsignado,
                    version = envio.version
                )
            )
        }
    }

    private suspend fun aplicarExito(operacion: OperacionPendienteEntity, remoto: EnvioDto) {
        db.withTransaction {
            operacionDao.eliminar(operacion)
            val actual = envioDao.porId(operacion.idEntidadLocal) ?: return@withTransaction
            if (operacionDao.deEntidad(actual.id).isNotEmpty()) {
                // El operador volvió a cambiar el envío mientras se enviaba: se guarda la
                // versión del servidor, pero el envío sigue pendiente con su último cambio.
                envioDao.actualizar(
                    actual.copy(
                        version = remoto.version,
                        estadoSync = EstadoSincronizacion.PENDIENTE.name,
                        mensajeError = null
                    )
                )
            } else {
                envioDao.actualizar(remoto.aEntidad(actual.id))
            }
        }
    }

    /**
     * RF15: el servidor rechazó el cambio (validación o conflicto). No se pierde nada:
     * la operación y el envío quedan en ERROR con el motivo. Si el servidor informó cómo
     * está el envío, se guarda su versión para que un reintento parta del dato actual.
     */
    private suspend fun aplicarRechazo(operacion: OperacionPendienteEntity, rechazo: ResultadoApi.Rechazo) {
        val enServidor = rechazo.envio
        val motivo = if (enServidor == null) {
            rechazo.mensaje
        } else {
            val estado = EstadoEnvio.desde(enServidor.estado).etiqueta.lowercase()
            val transportista = enServidor.transportistaAsignado ?: "sin transportista"
            "${rechazo.mensaje} En el servidor: $estado, $transportista."
        }
        db.withTransaction {
            operacionDao.actualizar(
                operacion.copy(
                    estado = EstadoSincronizacion.ERROR.name,
                    intentos = operacion.intentos + 1,
                    mensajeError = motivo
                )
            )
            envioDao.porId(operacion.idEntidadLocal)?.let {
                envioDao.actualizar(
                    it.copy(
                        version = enServidor?.version ?: it.version,
                        estadoSync = EstadoSincronizacion.ERROR.name,
                        mensajeError = motivo
                    )
                )
            }
        }
    }

    /** RF11: descarga rutas y envíos. Si algo falla, lo ya guardado en Room no se toca. */
    private suspend fun descargar(enviadas: Int, rechazadas: Int): ResultadoSync {
        val rutas = when (val respuesta = llamarApi { api.obtenerRutas() }) {
            is ResultadoApi.Exito -> respuesta.valor.datos
            is ResultadoApi.Rechazo -> return ResultadoSync(enviadas, rechazadas, error = respuesta.mensaje)
            is ResultadoApi.Fallo -> return ResultadoSync(enviadas, rechazadas, error = respuesta.mensaje)
        }
        return when (val respuesta = llamarApi { api.obtenerEnvios() }) {
            is ResultadoApi.Exito -> {
                guardarDescarga(rutas, respuesta.valor.datos, respuesta.valor.servidorAhora)
                ResultadoSync(enviadas, rechazadas, descargados = respuesta.valor.datos.size)
            }

            is ResultadoApi.Rechazo -> ResultadoSync(enviadas, rechazadas, error = respuesta.mensaje)
            is ResultadoApi.Fallo -> ResultadoSync(enviadas, rechazadas, error = respuesta.mensaje)
        }
    }

    /** Actualiza Room con lo descargado, sin sobrescribir cambios locales pendientes. */
    private suspend fun guardarDescarga(rutas: List<RutaDto>, remotos: List<EnvioDto>, horaServidor: String?) {
        db.withTransaction {
            // Las rutas son de solo lectura: se reemplaza la copia local completa.
            rutaDao.reemplazarTodas(rutas.map { it.aEntidad() })

            val locales = envioDao.todos()
            val porIdRemoto = locales.associateBy { it.idRemoto }
            val conCambiosLocales = operacionDao.todas().map { it.idEntidadLocal }.toSet()
            val sincronizado = EstadoSincronizacion.SINCRONIZADO.name

            remotos.forEach { remoto ->
                val local = porIdRemoto[remoto.id]
                when {
                    local == null -> envioDao.insertar(remoto.aEntidad())
                    local.id in conCambiosLocales || local.estadoSync != sincronizado -> Unit
                    else -> envioDao.actualizar(remoto.aEntidad(local.id))
                }
            }

            // Lo que el servidor ya no envía (el cliente lo eliminó) se quita del teléfono.
            val idsRemotos = remotos.map { it.id }.toSet()
            locales
                .filter { it.idRemoto !in idsRemotos && it.id !in conCambiosLocales && it.estadoSync == sincronizado }
                .forEach { envioDao.eliminar(it) }

            val ahora = System.currentTimeMillis()
            operacionDao.guardarMetadata(SyncMetadataEntity(RECURSO_RUTAS, ahora, horaServidor))
            operacionDao.guardarMetadata(SyncMetadataEntity(RECURSO_ENVIOS, ahora, horaServidor))
        }
    }

    private fun leerPayload(operacion: OperacionPendienteEntity): PayloadEnvio? = try {
        gson.fromJson(operacion.payload, PayloadEnvio::class.java)
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val RECURSO_ENVIOS = "envios"
        const val RECURSO_RUTAS = "rutas"
    }
}
