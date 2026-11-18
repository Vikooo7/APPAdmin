package com.example.rutalogadmin

import com.example.rutalogadmin.data.remote.dto.IndicadorDto
import com.example.rutalogadmin.model.ReglasTablero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReglasTableroTest {

    private fun ind(unidad: String, mejorSi: String, valor: Double?, variacion: Double?) =
        IndicadorDto("x", "X", unidad, mejorSi, "f", valor, null, variacion)

    @Test
    fun `formato de cada unidad`() {
        assertEquals("91 %", ReglasTablero.valor(ind("%", "sube", 91.0, null)))
        assertEquals("6.4 %", ReglasTablero.valor(ind("%", "baja", 6.4, null)))
        assertEquals("2.1 km", ReglasTablero.valor(ind("km", "baja", 2.1, null)))
        assertEquals("S/ 8,700.00", ReglasTablero.valor(ind("S/", "sube", 8700.0, null)))
        assertEquals("—", ReglasTablero.valor(ind("días", "baja", null, null)))
    }

    @Test
    fun `la variacion de un porcentaje va en puntos`() {
        assertEquals("▲ 7 pp", ReglasTablero.variacion(ind("%", "sube", 91.0, 7.0)))
        assertEquals("▼ 4.5 pp", ReglasTablero.variacion(ind("%", "baja", 6.4, -4.5)))
        assertEquals("▼ S/ 120.50", ReglasTablero.variacion(ind("S/", "sube", 1.0, -120.5)))
        assertNull(ReglasTablero.variacion(ind("%", "sube", 91.0, null)))
    }

    @Test
    fun `mejora segun hacia donde conviene`() {
        assertEquals(true, ReglasTablero.esMejora(ind("%", "sube", 91.0, 7.0)))
        assertEquals("menos fallidas es mejor", true, ReglasTablero.esMejora(ind("%", "baja", 6.4, -4.0)))
        assertEquals(false, ReglasTablero.esMejora(ind("km", "baja", 3.0, 0.5)))
        assertNull(ReglasTablero.esMejora(ind("%", "sube", 91.0, 0.0)))
    }

    @Test
    fun `semanas`() {
        assertEquals("16 – 22 nov", ReglasTablero.textoSemana("2026-11-16", "2026-11-22"))
        assertEquals("26 oct – 1 nov", ReglasTablero.textoSemana("2026-10-26", "2026-11-01"))
        assertEquals("2026-11-09", ReglasTablero.moverSemana("2026-11-16", -1))
    }
}
