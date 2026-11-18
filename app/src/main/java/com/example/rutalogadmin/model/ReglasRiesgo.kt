package com.example.rutalogadmin.model

import com.example.rutalogadmin.data.remote.dto.ConfirmacionDto
import com.example.rutalogadmin.data.remote.dto.RiesgoEntregaDto
import java.util.Locale
import kotlin.math.roundToInt

/** Niveles de riesgo de la ficha (ML-01) y la acción de cada uno. */
enum class NivelRiesgo(val etiqueta: String, val accion: String) {
    ALTO("Riesgo alto", "Pedir confirmación del horario"),
    MEDIO("Riesgo medio", "Aviso por notificación el mismo día"),
    BAJO("Riesgo bajo", "Sin acción")
}

/** Qué puede hacer el operador con una entrega según su nivel y lo que ya se hizo. */
enum class AccionConfirmacion { PEDIR, REGISTRAR_RESPUESTA, NINGUNA, SIN_TELEFONO, AVISO_DIA, AVISO_ENVIADO, SIN_ACCION }

object ReglasRiesgo {

    fun nivel(e: RiesgoEntregaDto): NivelRiesgo = when (e.nivel) {
        "alto" -> NivelRiesgo.ALTO
        "medio" -> NivelRiesgo.MEDIO
        "bajo" -> NivelRiesgo.BAJO
        else -> if (e.riesgoAlto) NivelRiesgo.ALTO else NivelRiesgo.BAJO // API anterior, sin "nivel"
    }

    /** Riesgo alto: sin pedir → se pide; pedida → se registra la respuesta; confirmada → nada; sin celular → no se puede. */
    fun accion(c: ConfirmacionDto): AccionConfirmacion = when {
        c.estado == "CONFIRMADA" -> AccionConfirmacion.NINGUNA
        c.estado == "PEDIDA" -> AccionConfirmacion.REGISTRAR_RESPUESTA
        c.sinTelefono -> AccionConfirmacion.SIN_TELEFONO
        else -> AccionConfirmacion.PEDIR
    }

    /** La acción según el nivel: alto → confirmación del horario · medio → aviso del día · bajo → sin acción. */
    fun accion(e: RiesgoEntregaDto): AccionConfirmacion = when (nivel(e)) {
        NivelRiesgo.ALTO -> accion(e.confirmacion)
        NivelRiesgo.MEDIO -> when {
            e.confirmacion.avisoDiaEn != null -> AccionConfirmacion.AVISO_ENVIADO
            e.confirmacion.sinTelefono -> AccionConfirmacion.SIN_TELEFONO
            else -> AccionConfirmacion.AVISO_DIA
        }
        NivelRiesgo.BAJO -> AccionConfirmacion.SIN_ACCION
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
