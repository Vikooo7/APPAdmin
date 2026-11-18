package com.example.rutalogadmin.data.remote.dto

/** Formato JSON de la API REST. Los nombres coinciden con los que envía y recibe el servidor. */

data class EnvioDto(
    val id: Int,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    val estado: String,
    val transportistaAsignado: String?,
    val version: Int,
    // Campos del proyecto final. Son anulables: Gson deja null lo que el servidor no envíe.
    val courierId: Int? = null,
    val destinatarioNombre: String? = null,
    val destinatarioTelefono: String? = null,
    val destinatarioDireccion: String? = null,
    val montoContraentrega: Double? = null,
    val intentos: Int? = null,
    /** NO_REQUERIDO, POR_PAGAR o PAGADO (fase de pagos). */
    val pagoEstado: String? = null
)

data class EnviosResponse(
    val datos: List<EnvioDto>,
    val servidorAhora: String?
)

/**
 * PUT /api/envios/{id} como operador: cambia el estado y el courier (RF08 y RF09).
 * Se envía [courierId]; [transportistaAsignado] (texto "Nombre · Placa") solo lo usan los
 * cambios que quedaron en la cola antes de actualizar la app. Gson omite los campos null.
 */
data class ActualizarEnvioRequest(
    /** UUID de la operación: si llega repetido, el servidor no aplica el cambio dos veces (RF15). */
    val uuidOperacion: String,
    val estado: String,
    val courierId: Int?,
    val transportistaAsignado: String?,
    /** Versión que tenía el envío en el teléfono; si el servidor tiene otra, hay conflicto. */
    val version: Int
)

/** Cuerpo de las respuestas de error. En un conflicto (409) incluye el envío real del servidor. */
data class ErrorDto(
    val codigo: String?,
    val mensaje: String?,
    val envio: EnvioDto?
)
