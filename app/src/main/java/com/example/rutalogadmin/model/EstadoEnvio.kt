package com.example.rutalogadmin.model

/**
 * Estados de un envío. En Room se guarda el [codigo]. La App Cliente registra
 * los envíos como PENDIENTE (el REGISTRADO de la ficha del proyecto final); la App Admin
 * los pasa por el flujo normal. FALLIDO lo registra el courier, REPROGRAMADO el operador y
 * DEVUELTO llega solo al tercer intento fallido (RN-05).
 *
 * [paso] ordena el avance: el estado nunca retrocede a un paso menor.
 */
enum class EstadoEnvio(
    val codigo: String,
    val etiqueta: String,
    val paso: Int,
    val descripcion: String
) {
    PENDIENTE("pendiente", "Pendiente de recojo", 0, "Guía emitida. Esperando que recojan la carga."),
    RECOGIDO("recogido", "Recogido", 1, "La carga fue recogida y verificada en almacén."),
    EN_TRANSITO("en_transito", "En tránsito", 2, "La unidad va en camino a la ciudad de destino."),
    EN_REPARTO("en_reparto", "En reparto", 3, "La carga salió a reparto con el destinatario."),
    ENTREGADO("entregado", "Entregado", 4, "Entregado y firmado por el destinatario."),
    FALLIDO("fallido", "Entrega fallida", 3, "El courier no pudo entregar. Hay que reprogramar la entrega."),
    // Vuelve a salir a reparto: cuenta como "en tránsito" para que el estado no retroceda.
    REPROGRAMADO("reprogramado", "Reprogramado", 2, "La entrega fallida se reprogramó y saldrá de nuevo a reparto."),
    DEVUELTO("devuelto", "Devuelto", 4, "Tres intentos fallidos: la carga vuelve al remitente.");

    /** La carga está en manos de RutaLog: recogida y todavía sin entregar ni devolver. */
    val enCurso: Boolean
        get() = this == RECOGIDO || this == EN_TRANSITO || this == EN_REPARTO || this == FALLIDO || this == REPROGRAMADO

    companion object {
        /** RF08: estados a los que el operador puede pasar un envío. */
        val delOperador = listOf(RECOGIDO, EN_TRANSITO, EN_REPARTO, ENTREGADO)

        /** RN-05: al tercer intento fallido el envío vuelve al remitente. */
        const val MAX_INTENTOS = 3

        fun desde(codigo: String): EstadoEnvio = entries.firstOrNull { it.codigo == codigo } ?: PENDIENTE
    }
}

val Envio.estadoEnvio: EstadoEnvio
    get() = EstadoEnvio.desde(estado)

/** Entregado o devuelto: la operación quedó cerrada y ya no se cambia su estado ni su courier. */
val Envio.estaCerrado: Boolean
    get() = estadoEnvio == EstadoEnvio.ENTREGADO || estadoEnvio == EstadoEnvio.DEVUELTO

/** Envío al que todavía no se le asignó un courier (ni un transportista del parcial). */
val Envio.sinCourier: Boolean
    get() = courierId == null && transportistaAsignado == null && !estaCerrado
