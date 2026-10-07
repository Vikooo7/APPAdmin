package com.example.rutalogadmin.data.repository

import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.mapper.aModelo
import com.example.rutalogadmin.model.Ruta
import com.example.rutalogadmin.model.RutaConCarga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Rutas guardadas en Room. Son de solo lectura: el operador las consulta, no las modifica. */
class RutaRepository(db: AppDatabase) {

    private val rutaDao = db.rutaDao()

    /** Para el filtro por ruta de RF07. */
    fun observarRutas(): Flow<List<Ruta>> =
        rutaDao.obtenerTodas().map { lista -> lista.map { it.aModelo() } }

    /** RF10: rutas con su carga; se recalcula sola cuando cambia un envío. */
    fun observarConCarga(): Flow<List<RutaConCarga>> = rutaDao.obtenerConCarga()

    fun observarPorNombre(nombre: String): Flow<Ruta?> =
        rutaDao.observarPorNombre(nombre).map { it?.aModelo() }
}
