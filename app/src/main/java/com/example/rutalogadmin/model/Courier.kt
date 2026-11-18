package com.example.rutalogadmin.model

/**
 * Courier: usuario con rol courier en el servidor (los transportistas del parcial).
 * Se descarga de GET /api/couriers y se guarda en Room para elegirlo aunque no haya conexión.
 */
data class Courier(
    val id: Int,
    val nombre: String,
    val placa: String,
    val zona: String,
    /** RF-O01: máximo de paradas activas (30 por defecto). */
    val capacidad: Int
) {
    /** El mismo texto "Nombre · Placa" que guarda transportistaAsignado desde el parcial. */
    val etiqueta: String get() = "$nombre · $placa"
}

/** Courier con las paradas que tiene en curso según los envíos guardados en Room. */
data class CourierConCarga(
    val courier: Courier,
    val activos: Int
) {
    val disponibles: Int get() = (courier.capacidad - activos).coerceAtLeast(0)
    val lleno: Boolean get() = disponibles == 0
}

/** Calcula la carga de cada courier: envíos asignados que no están entregados ni devueltos. */
fun calcularCarga(couriers: List<Courier>, envios: List<Envio>): List<CourierConCarga> {
    val activosPorCourier = envios
        .filter { it.courierId != null && !it.estaCerrado }
        .groupingBy { it.courierId }
        .eachCount()
    return couriers.map { CourierConCarga(it, activosPorCourier[it.id] ?: 0) }
}
