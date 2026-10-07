package com.example.rutalogadmin.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla "envios": los campos del caso (id, numeroGuia, ruta, pesoKg, costoEnvio, estado,
 * transportistaAsignado) más los que exige la sincronización: identificador remoto, UUID,
 * versión y estado de sincronización.
 *
 * El operador no crea envíos: todos llegan de la API, por eso siempre tienen idRemoto.
 */
@Entity(
    tableName = "envios",
    indices = [
        Index(value = ["idRemoto"], unique = true),
        Index(value = ["uuid"], unique = true),
        Index("estado"),
        Index("ruta")
    ]
)
data class EnvioEntity(
    /** Identificador local. No cambia aunque el envío se vuelva a descargar. */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idRemoto: Int,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    /** Estado del negocio: código de EstadoEnvio. */
    val estado: String,
    val transportistaAsignado: String?,
    /** Versión del servidor; sirve para detectar conflictos. */
    val version: Int = 1,
    /** Nombre de EstadoSincronizacion: PENDIENTE, ENVIANDO, SINCRONIZADO o ERROR. */
    val estadoSync: String,
    /** Motivo del rechazo del servidor, si lo hubo. */
    val mensajeError: String? = null
)
