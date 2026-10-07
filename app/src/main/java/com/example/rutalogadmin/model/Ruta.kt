package com.example.rutalogadmin.model

/** Ruta del caso: id, nombre y tiempo estimado de viaje en horas. */
data class Ruta(
    val id: Int,
    val nombre: String,
    val tiempoEstimado: Int
)

/**
 * RF10: una ruta con la carga que tiene en este momento (resultado de RutaDao.obtenerConCarga).
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
