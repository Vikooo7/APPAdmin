package com.example.rutalogadmin.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla "rutas" del caso: id, nombre y tiempoEstimado (horas de viaje).
 * Es de solo lectura: se descarga de la API y el id es el del servidor.
 * "nombre" es el mismo texto que guarda la columna "ruta" de la tabla envios.
 */
@Entity(tableName = "rutas", indices = [Index(value = ["nombre"], unique = true)])
data class RutaEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val tiempoEstimado: Int
)
