package com.example.rutalogadmin.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla "rutas" de la App Operador.
 *
 * - nombre: por ejemplo "Lima → Arequipa". Es el mismo texto que guarda la columna
 *   "ruta" de la tabla envios, así se relacionan las dos tablas.
 * - tiempoEstimado: horas estimadas de viaje hasta el destino.
 */
@Entity(tableName = "rutas", indices = [Index(value = ["nombre"], unique = true)])
data class Ruta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val tiempoEstimado: Int
)

/**
 * RF10: resultado de la consulta de rutas con la carga que tienen en este momento.
 * Una ruta está "activa" cuando lleva al menos un envío recogido, en tránsito o en reparto.
 */
data class RutaConCarga(
    val id: Int,
    val nombre: String,
    val tiempoEstimado: Int,
    val recogidos: Int,
    val enTransito: Int,
    val enReparto: Int,
    val pendientes: Int,
    val totalEnvios: Int
) {
    val enCurso: Int get() = recogidos + enTransito + enReparto
    val esActiva: Boolean get() = enCurso > 0
}
