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
    val version: Int
)

data class EnviosResponse(
    val datos: List<EnvioDto>,
    val servidorAhora: String?
)

/** PUT /api/envios/{id} como administrador: cambia el estado y el transportista (RF08 y RF09). */
data class ActualizarEnvioRequest(
    /** UUID de la operación: si llega repetido, el servidor no aplica el cambio dos veces (RF15). */
    val uuidOperacion: String,
    val estado: String,
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
