package com.example.rutalogadmin.data.remote.dto

/** Ruta activa tal como la devuelve GET /api/rutas. */
data class RutaDto(
    val id: Int,
    val nombre: String,
    /** Horas de viaje. */
    val tiempoEstimado: Int
)

data class RutasResponse(
    val datos: List<RutaDto>
)
