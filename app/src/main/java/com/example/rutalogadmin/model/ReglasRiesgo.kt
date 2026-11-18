package com.example.rutalogadmin.model

import com.example.rutalogadmin.data.remote.dto.ConfirmacionDto
import java.util.Locale
import kotlin.math.roundToInt

/** Qué puede hacer el operador con una entrega según su confirmación (aviso previo de ML-01). */
enum class AccionConfirmacion { PEDIR, REGISTRAR_RESPUESTA, NINGUNA, SIN_TELEFONO }

object ReglasRiesgo {

    /** Sin pedir → se pide; pedida → se registra la respuesta; confirmada → nada; sin celular → no se puede avisar. */
    fun accion(c: ConfirmacionDto): AccionConfirmacion = when {
        c.estado == "CONFIRMADA" -> AccionConfirmacion.NINGUNA
        c.estado == "PEDIDA" -> AccionConfirmacion.REGISTRAR_RESPUESTA
        c.sinTelefono -> AccionConfirmacion.SIN_TELEFONO
        else -> AccionConfirmacion.PEDIR
    }

    /** 0.874 → "87 %". */
    fun porcentaje(probabilidad: Double): String = "${(probabilidad * 100).roundToInt()} %"

    /** Franja de 2 h: 8 → "8–10 h"; sin ruta todavía → "sin ruta". */
    fun textoFranja(franja: Int?): String = franja?.let { "$it–${it + 2} h" } ?: "sin ruta"

    /** "2026-11-19" → "jue 19/11". */
    fun textoFecha(fecha: String): String = runCatching {
        val d = java.time.LocalDate.parse(fecha)
        val dia = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale("es", "PE")).removeSuffix(".")
        "%s %02d/%02d".format(dia, d.dayOfMonth, d.monthValue)
    }.getOrDefault(fecha)
}
