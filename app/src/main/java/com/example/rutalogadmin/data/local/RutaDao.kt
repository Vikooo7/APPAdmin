package com.example.rutalogadmin.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RutaDao {

    @Query("SELECT * FROM rutas ORDER BY nombre")
    fun obtenerTodas(): Flow<List<Ruta>>

    @Query("SELECT * FROM rutas WHERE nombre = :nombre LIMIT 1")
    fun obtenerPorNombre(nombre: String): Flow<Ruta?>

    /**
     * RF10: cada ruta con cuántos envíos lleva en cada estado. Las rutas activas
     * (con carga en curso) salen primero y, entre ellas, las de más envíos.
     */
    @Query(
        """
        SELECT r.id, r.nombre, r.tiempoEstimado,
            SUM(CASE WHEN e.estado = 'recogido' THEN 1 ELSE 0 END) AS recogidos,
            SUM(CASE WHEN e.estado = 'en_transito' THEN 1 ELSE 0 END) AS enTransito,
            SUM(CASE WHEN e.estado = 'en_reparto' THEN 1 ELSE 0 END) AS enReparto,
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
}
