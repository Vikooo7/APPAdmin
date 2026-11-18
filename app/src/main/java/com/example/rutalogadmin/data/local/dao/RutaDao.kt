package com.example.rutalogadmin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.rutalogadmin.data.local.entities.RutaEntity
import com.example.rutalogadmin.model.RutaConCarga
import kotlinx.coroutines.flow.Flow

@Dao
interface RutaDao {

    @Query("SELECT * FROM rutas ORDER BY nombre")
    fun obtenerTodas(): Flow<List<RutaEntity>>

    @Query("SELECT * FROM rutas WHERE nombre = :nombre LIMIT 1")
    fun observarPorNombre(nombre: String): Flow<RutaEntity?>

    /**
     * RF10: cada ruta con cuántos envíos lleva en cada estado. Las rutas activas
     * (con carga en curso) salen primero y, entre ellas, las de más envíos.
     */
    @Query(
        """
        SELECT r.id, r.nombre, r.tiempoEstimado,
            SUM(CASE WHEN e.estado = 'recogido' THEN 1 ELSE 0 END) AS recogidos,
            SUM(CASE WHEN e.estado = 'en_transito' THEN 1 ELSE 0 END) AS enTransito,
            SUM(CASE WHEN e.estado IN ('en_reparto', 'fallido', 'reprogramado') THEN 1 ELSE 0 END) AS enReparto,
            SUM(CASE WHEN e.estado = 'pendiente' THEN 1 ELSE 0 END) AS pendientes,
            COUNT(e.id) AS totalEnvios
        FROM rutas r
        LEFT JOIN envios e ON e.ruta = r.nombre
        GROUP BY r.id
        ORDER BY (recogidos + enTransito + enReparto) > 0 DESC,
            (recogidos + enTransito + enReparto) DESC,
            r.tiempoEstimado
        """
    )
    fun obtenerConCarga(): Flow<List<RutaConCarga>>

    @Insert
    suspend fun insertarTodas(rutas: List<RutaEntity>)

    @Query("DELETE FROM rutas")
    suspend fun borrarTodo()

    /** Las rutas son de solo lectura: al descargarlas se reemplaza la copia local completa. */
    @Transaction
    suspend fun reemplazarTodas(rutas: List<RutaEntity>) {
        borrarTodo()
        insertarTodas(rutas)
    }
}
