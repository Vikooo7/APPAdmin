package com.example.rutalogadmin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla "couriers": usuarios con rol courier, descargados de GET /api/couriers.
 * Es de solo lectura y el id es el del servidor. La carga de cada uno se calcula con los envíos.
 */
@Entity(tableName = "couriers")
data class CourierEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val placa: String,
    val zona: String,
    val capacidad: Int
)
