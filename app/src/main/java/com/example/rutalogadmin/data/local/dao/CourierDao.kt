package com.example.rutalogadmin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.rutalogadmin.data.local.entities.CourierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourierDao {

    @Query("SELECT * FROM couriers ORDER BY zona, nombre")
    fun observarTodos(): Flow<List<CourierEntity>>

    @Insert
    suspend fun insertarTodos(couriers: List<CourierEntity>)

    @Query("DELETE FROM couriers")
    suspend fun borrarTodo()

    /** Los couriers son de solo lectura: al descargarlos se reemplaza la copia local completa. */
    @Transaction
    suspend fun reemplazarTodos(couriers: List<CourierEntity>) {
        borrarTodo()
        insertarTodos(couriers)
    }
}
