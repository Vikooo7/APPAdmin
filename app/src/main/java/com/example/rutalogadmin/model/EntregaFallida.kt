package com.example.rutalogadmin.model

/** Motivos de una entrega fallida (los mismos que acepta la API). */
enum class MotivoFalla(val codigo: String, val etiqueta: String) {
    AUSENTE("ausente", "Destinatario ausente"),
    DIRECCION_ERRADA("direccion_errada", "Dirección errada"),
    RECHAZADO("rechazado", "Rechazado por el destinatario"),
    ZONA_INACCESIBLE("zona_inaccesible", "Zona inaccesible");

    companion object {
        fun etiqueta(codigo: String): String = entries.firstOrNull { it.codigo == codigo }?.etiqueta ?: codigo
    }
}

data class IntentoFallido(
    val numero: Int,
    val motivo: String,
    /** Hora real del intento, en milisegundos. */
    val fecha: Long?,
    val courierNombre: String?,
    val tieneFoto: Boolean
)

/** Una entrega fallida con su historial de intentos, tal como la devuelve GET /api/fallidas. */
data class EntregaFallida(
    val idRemoto: Int,
    val numeroGuia: String,
    val ruta: String,
    val estado: EstadoEnvio,
    val version: Int,
    val courierId: Int?,
    val transportista: String?,
    val destinatario: String?,
    val montoContraentrega: Double,
    val intentos: List<IntentoFallido>,
    val maxIntentos: Int
) {
    val porReprogramar: Boolean get() = estado == EstadoEnvio.FALLIDO
}
