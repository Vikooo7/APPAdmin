package com.example.rutalogadmin.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvioDao {

    @Insert
    suspend fun insertar(envio: Envio): Long

    /** RF08 + RF09: guarda el nuevo estado y el transportista del envío. */
    @Update
    suspend fun actualizar(envio: Envio)

    @Delete
    suspend fun eliminar(envio: Envio)

    /** RF06: todos los envíos guardados en Room. */
    @Query("SELECT * FROM envios ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<Envio>>

    /** RF07: filtra por estado y por ruta. Un parámetro null significa "todos". */
    @Query(
        "SELECT * FROM envios " +
            "WHERE (:estado IS NULL OR estado = :estado) " +
            "AND (:ruta IS NULL OR ruta = :ruta) " +
            "ORDER BY id DESC"
    )
    fun filtrar(estado: String?, ruta: String?): Flow<List<Envio>>

    @Query("SELECT * FROM envios WHERE id = :id")
    fun obtenerPorId(id: Int): Flow<Envio?>
}
