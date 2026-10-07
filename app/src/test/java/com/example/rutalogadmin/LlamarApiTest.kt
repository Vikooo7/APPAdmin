package com.example.rutalogadmin

import com.example.rutalogadmin.data.mapper.aEntidad
import com.example.rutalogadmin.data.mapper.aModelo
import com.example.rutalogadmin.data.remote.ResultadoApi
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.RutaDto
import com.example.rutalogadmin.data.remote.dto.RutasResponse
import com.example.rutalogadmin.data.remote.llamarApi
import com.example.rutalogadmin.model.EstadoSincronizacion
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Cómo clasifica la app las respuestas de la API: éxito, rechazo del servidor o fallo reintentable. */
class LlamarApiTest {

    private fun cuerpoError(json: String) = ResponseBody.create(MediaType.parse("application/json"), json)

    @Test
    fun respuestaCorrecta_esExito() = runBlocking<Unit> {
        val resultado = llamarApi { Response.success(RutasResponse(listOf(RutaDto(1, "Lima → Ica", 4)))) }

        assertTrue(resultado is ResultadoApi.Exito)
        assertEquals(4, (resultado as ResultadoApi.Exito).valor.datos.first().tiempoEstimado)
    }

    @Test
    fun conflicto409_esRechazoConElEnvioDelServidor() = runBlocking<Unit> {
        val json = """
            {"codigo":"ESTADO_NO_RETROCEDE","mensaje":"El estado solo puede avanzar: el envío ya está en reparto.",
             "envio":{"id":7,"uuid":"00000000-0000-4000-8000-000000000007","numeroGuia":"RLP-26-100007-8",
                      "ruta":"Lima → Ica","pesoKg":18,"costoEnvio":25.2,"estado":"en_reparto",
                      "transportistaAsignado":"Rosa Huamán Torres · BFK-209","version":3}}
        """.trimIndent()

        val resultado = llamarApi<EnvioDto> { Response.error(409, cuerpoError(json)) }

        assertTrue(resultado is ResultadoApi.Rechazo)
        val rechazo = resultado as ResultadoApi.Rechazo
        assertEquals(409, rechazo.http)
        assertEquals("ESTADO_NO_RETROCEDE", rechazo.codigo)
        assertEquals("en_reparto", rechazo.envio?.estado)
        assertEquals(3, rechazo.envio?.version)
    }

    @Test
    fun error503_esFalloQueSePuedeReintentar() = runBlocking<Unit> {
        val json = """{"codigo":"SERVIDOR_NO_DISPONIBLE","mensaje":"Error del servidor simulado para la prueba."}"""

        val resultado = llamarApi<EnvioDto> { Response.error(503, cuerpoError(json)) }

        assertTrue(resultado is ResultadoApi.Fallo)
        assertFalse((resultado as ResultadoApi.Fallo).sinConexion)
    }

    @Test
    fun sinRed_esFalloSinConexion() = runBlocking<Unit> {
        val resultado = llamarApi<EnvioDto> { throw IOException("sin red") }

        assertTrue(resultado is ResultadoApi.Fallo)
        assertTrue((resultado as ResultadoApi.Fallo).sinConexion)
    }

    @Test
    fun envioDelServidor_quedaSincronizadoYConservaElIdLocal() {
        val dto = EnvioDto(
            id = 42,
            uuid = "00000000-0000-4000-8000-000000000042",
            numeroGuia = "RLP-26-300012-6",
            ruta = "Lima → Cajamarca",
            pesoKg = 140.0,
            costoEnvio = 364.0,
            estado = "pendiente",
            transportistaAsignado = null,
            version = 2
        )

        val entidad = dto.aEntidad(idLocal = 5)

        assertEquals(5, entidad.id)
        assertEquals(42, entidad.idRemoto)
        assertEquals(2, entidad.version)
        assertEquals(EstadoSincronizacion.SINCRONIZADO, entidad.aModelo().estadoSync)
    }
}
