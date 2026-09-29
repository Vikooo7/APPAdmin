package com.example.rutalogadmin.ui.components

import java.util.Locale

private val localePeru: Locale = Locale.forLanguageTag("es-PE")

fun formatoSoles(monto: Double): String = String.format(localePeru, "S/ %,.2f", monto)

fun formatoPeso(kg: Double): String = String.format(localePeru, "%,.2f kg", kg)

/** Horas de viaje legibles: 16 → "16 h", 96 → "4 d", 28 → "1 d 4 h". */
fun formatoTiempo(horas: Int): String {
    if (horas < 24) return "$horas h"
    val dias = horas / 24
    val resto = horas % 24
    return if (resto == 0) "$dias d" else "$dias d $resto h"
}
