package com.example.rutalogadmin.model

/**
 * Envío tal como lo usan las pantallas y los ViewModel.
 * La tabla de Room es EnvioEntity y el formato de la API es EnvioDto (ver data/mapper).
 */
data class Envio(
    /** Identificador local (Room). */
    val id: Int,
    /** Identificador en el servidor. */
    val idRemoto: Int,
    /** Identificador único del envío; es el mismo en el teléfono y en el servidor. */
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    val estado: String,
    /** Texto "Nombre · Placa" del courier (o el transportista escrito en el parcial). */
    val transportistaAsignado: String?,
    /** Versión del servidor; sirve para detectar conflictos. */
    val version: Int,
    val estadoSync: EstadoSincronizacion,
    /** Motivo del rechazo del servidor, si lo hubo. */
    val mensajeError: String?,
    /** Courier asignado, o null si no tiene (o si es un transportista del parcial que no es courier). */
    val courierId: Int? = null,
    val destinatarioNombre: String? = null,
    val destinatarioTelefono: String? = null,
    val destinatarioDireccion: String? = null,
    /** Monto que el courier cobra al destinatario; 0 si el envío es prepagado. */
    val montoContraentrega: Double = 0.0,
    /** Intentos de entrega fallidos (RN-05: al tercero, el envío vuelve al remitente). */
    val intentos: Int = 0,
    /** RF-R02: el remitente paga el envío con QR; sin pago no se asigna al courier. */
    val pagoEstado: String = "NO_REQUERIDO"
)
