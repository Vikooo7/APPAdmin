package com.example.rutalogadmin

import com.example.rutalogadmin.data.remote.dto.ConfirmacionDto
import com.example.rutalogadmin.data.remote.dto.ConfirmacionResponse
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.ModeloRiesgoDto
import com.example.rutalogadmin.data.remote.dto.ResumenRiesgoDto
import com.example.rutalogadmin.data.remote.dto.RiesgoEntregaDto
import com.example.rutalogadmin.data.remote.dto.RiesgoResponse
import com.example.rutalogadmin.model.AccionConfirmacion
import com.example.rutalogadmin.model.NivelRiesgo
import com.example.rutalogadmin.model.ReglasRiesgo
import com.example.rutalogadmin.viewmodel.RiesgoUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ReglasRiesgoTest {

    private fun conf(estado: String?, sinTelefono: Boolean = false) = ConfirmacionDto(estado, null, null, sinTelefono)

    private fun entrega(guia: String, p: Double, alto: Boolean) = RiesgoEntregaDto(
        envio = EnvioDto(1, "u", guia, "Lima → Callao", 1.0, 10.0, "en_reparto", null, 1),
        zona = "Lima y Callao", courier = null, franja = 8, probabilidad = p, riesgoAlto = alto,
        factores = listOf("Franja de 8 h"), confirmacion = conf(null)
    )

    @Test
    fun `la accion depende de la confirmacion`() {
        assertEquals(AccionConfirmacion.PEDIR, ReglasRiesgo.accion(conf(null)))
        assertEquals(AccionConfirmacion.REGISTRAR_RESPUESTA, ReglasRiesgo.accion(conf("PEDIDA")))
        assertEquals(AccionConfirmacion.NINGUNA, ReglasRiesgo.accion(conf("CONFIRMADA")))
        assertEquals(AccionConfirmacion.SIN_TELEFONO, ReglasRiesgo.accion(conf(null, sinTelefono = true)))
    }

    @Test
    fun `niveles de la ficha y su accion`() {
        val medio = entrega("RLP-3", 0.08, false).copy(nivel = "medio")
        assertEquals(NivelRiesgo.MEDIO, ReglasRiesgo.nivel(medio))
        assertEquals(AccionConfirmacion.AVISO_DIA, ReglasRiesgo.accion(medio))
        assertEquals(
            AccionConfirmacion.AVISO_ENVIADO,
            ReglasRiesgo.accion(medio.copy(confirmacion = conf(null).copy(avisoDiaEn = "2026-11-19T13:00:00Z")))
        )
        assertEquals(AccionConfirmacion.SIN_ACCION, ReglasRiesgo.accion(entrega("RLP-4", 0.02, false).copy(nivel = "bajo")))
        // Alto: el flujo de confirmación del horario.
        assertEquals(AccionConfirmacion.PEDIR, ReglasRiesgo.accion(entrega("RLP-5", 0.6, true).copy(nivel = "alto")))
        // Sin "nivel" (API anterior) se deduce de riesgoAlto.
        assertEquals(NivelRiesgo.ALTO, ReglasRiesgo.nivel(entrega("RLP-6", 0.6, true)))
        // "Solo alto y medio" deja fuera solo los bajos.
        val estado = RiesgoUiState(
            respuesta = RiesgoResponse("2026-11-19", ModeloRiesgoDto("ML-01", 0.11, 0.055), ResumenRiesgoDto(2, 0, 0, 1),
                listOf(medio, entrega("RLP-4", 0.02, false).copy(nivel = "bajo")))
        )
        assertEquals(listOf("RLP-3"), estado.visibles.map { it.envio.numeroGuia })
    }

    @Test
    fun `textos de probabilidad, franja y fecha`() {
        assertEquals("87 %", ReglasRiesgo.porcentaje(0.874))
        assertEquals("11 %", ReglasRiesgo.porcentaje(0.11))
        assertEquals("8–10 h", ReglasRiesgo.textoFranja(8))
        assertEquals("sin ruta", ReglasRiesgo.textoFranja(null))
        assertEquals("19/11", ReglasRiesgo.textoFecha("2026-11-19").takeLast(5))
        assertEquals("no-es-fecha", ReglasRiesgo.textoFecha("no-es-fecha"))
    }

    @Test
    fun `solo riesgo alto filtra y marca por guia para Asignacion`() {
        val r = RiesgoResponse(
            fecha = "2026-11-19",
            modelo = ModeloRiesgoDto("ML-01", 0.11),
            resumen = ResumenRiesgoDto(2, 1, 0),
            datos = listOf(entrega("RLP-1", 0.8, true), entrega("RLP-2", 0.05, false))
        )
        val estado = RiesgoUiState(respuesta = r)
        assertEquals(listOf("RLP-1"), estado.visibles.map { it.envio.numeroGuia })
        assertEquals(2, estado.copy(soloAltos = false).visibles.size)
        assertEquals(setOf("RLP-1"), estado.altosPorGuia.keys)
    }

    @Test
    fun `la respuesta de confirmacion acepta que no venga el aviso`() {
        assertEquals(null, ConfirmacionResponse(1, "RLP-1", "CONFIRMADA").aviso)
    }
}
