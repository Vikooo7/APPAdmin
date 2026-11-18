package com.example.rutalogadmin.data.remote.dto

/** GET /api/couriers: courier con su zona, capacidad y paradas activas según el servidor. */
data class CourierDto(
    val id: Int,
    val nombre: String,
    val placa: String?,
    val zona: String?,
    val capacidad: Int,
    val activos: Int?
)

data class CouriersResponse(
    val datos: List<CourierDto>
)

/** POST /api/asignaciones: todo o nada; si un envío no se puede asignar, no se asigna ninguno. */
data class AsignacionRequest(
    val uuidOperacion: String,
    val courierId: Int,
    /** Ids remotos de los envíos. */
    val envioIds: List<Int>
)

data class AsignacionResponse(
    val courierId: Int,
    val asignados: Int,
    val envios: List<EnvioDto>
)

/** POST /api/envios/{id}/reprogramar. Gson omite courierId si es null (se mantiene el courier). */
data class ReprogramarRequest(
    val uuidOperacion: String,
    val version: Int,
    val courierId: Int?
)

/** GET /api/fallidas */
data class IntentoDto(
    val numero: Int,
    val motivo: String,
    val fotoUrl: String?,
    val fecha: String?,
    val courierId: Int?,
    val courierNombre: String?
)

data class FallidaDto(
    val envio: EnvioDto,
    val maxIntentos: Int?,
    val intentos: List<IntentoDto>?
)

data class FallidasResponse(
    val datos: List<FallidaDto>
)
