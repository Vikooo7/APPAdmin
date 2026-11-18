package com.example.rutalogadmin.model

import com.example.rutalogadmin.data.remote.dto.IndicadorDto
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

/** Cómo se lee cada indicador del tablero (RF-O05). */
object ReglasTablero {

    private val peru = Locale("es", "PE")

    /** 91.0 % → "91 %"; 2.1 km → "2.1 km"; 8700 S/ → "S/ 8,700.00"; sin datos → "—". */
    fun valor(i: IndicadorDto): String = formatear(i.valor, i.unidad)

    fun formatear(v: Double?, unidad: String): String = when {
        v == null -> "—"
        unidad == "S/" -> "S/ " + String.format(Locale.US, "%,.2f", v)
        unidad == "%" -> "${numero(v)} %"
        else -> "${numero(v)} $unidad"
    }

    /** "▲ 7 pp" / "▼ 18 km" / "= igual"; null si no hay con qué comparar. */
    fun variacion(i: IndicadorDto): String? {
        val d = i.variacion ?: return null
        if (d == 0.0) return "= igual que la semana anterior"
        val flecha = if (d > 0) "▲" else "▼"
        val unidad = when (i.unidad) {
            "%" -> "pp"
            "S/" -> return "$flecha S/ " + String.format(Locale.US, "%,.2f", abs(d))
            else -> i.unidad
        }
        return "$flecha ${numero(abs(d))} $unidad"
    }

    /** true si cambió hacia donde conviene (más entregas a tiempo, menos fallidas...); null si no cambió. */
    fun esMejora(i: IndicadorDto): Boolean? {
        val d = i.variacion ?: return null
        if (d == 0.0) return null
        return if (i.mejorSi == "baja") d < 0 else d > 0
    }

    /** "2026-11-16".."2026-11-22" → "16 – 22 nov". */
    fun textoSemana(inicio: String, fin: String): String = runCatching {
        val a = LocalDate.parse(inicio)
        val b = LocalDate.parse(fin)
        val mes = { d: LocalDate -> d.month.getDisplayName(java.time.format.TextStyle.SHORT, peru).removeSuffix(".") }
        if (a.month == b.month) "${a.dayOfMonth} – ${b.dayOfMonth} ${mes(b)}" else "${a.dayOfMonth} ${mes(a)} – ${b.dayOfMonth} ${mes(b)}"
    }.getOrDefault("$inicio – $fin")

    /** Fecha (AAAA-MM-DD) de la semana anterior o siguiente a la que empieza en [inicio]. */
    fun moverSemana(inicio: String, semanas: Long): String = LocalDate.parse(inicio).plusWeeks(semanas).toString()

    private fun numero(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)
}
