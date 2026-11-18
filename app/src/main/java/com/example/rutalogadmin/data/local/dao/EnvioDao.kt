package com.example.rutalogadmin.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.rutalogadmin.data.local.entities.EnvioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvioDao {

    /** Envío descargado de la API que todavía no estaba en el teléfono. */
    @Insert
    suspend fun insertar(envio: EnvioEntity): Long

    /** RF08 + RF09: guarda el nuevo estado y el transportista del envío. */
    @Update
    suspend fun actualizar(envio: EnvioEntity)

    /** Envío que el servidor ya no devuelve (por ejemplo, el cliente lo eliminó). */
    @Delete
    suspend fun eliminar(envio: EnvioEntity)

    /** RF06: todos los envíos guardados en Room. */
    @Query("SELECT * FROM envios ORDER BY idRemoto DESC")
    fun obtenerTodos(): Flow<List<EnvioEntity>>

    /** RF07: filtra por estado y por ruta. Un parámetro null significa "todos". */
    @Query(
        "SELECT * FROM envios " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:ruta IS NULL OR ruta = :ruta) " +
            "ORDER BY idRemoto DESC"
    )
    fun filtrar(estado: String?, ruta: String?): Flow<List<EnvioEntity>>

    @Query("SELECT * FROM envios WHERE id = :id")
    fun observarPorId(id: Int): Flow<EnvioEntity?>

    @Query("SELECT * FROM envios WHERE id = :id")
    suspend fun porId(id: Int): EnvioEntity?

    @Query("SELECT * FROM envios WHERE idRemoto = :idRemoto")
    suspend fun porIdRemoto(idRemoto: Int): EnvioEntity?

    @Query("SELECT * FROM envios")
    suspend fun todos(): List<EnvioEntity>

    /** Si la app se cerró a mitad de un envío, esos registros vuelven a quedar pendientes. */
    @Query("UPDATE envios SET estadoSync = 'PENDIENTE' WHERE estadoSync = 'ENVIANDO'")
    suspend fun recuperarInterrumpidos()

    @Query("DELETE FROM envios")
    suspend fun borrarTodo()
}
