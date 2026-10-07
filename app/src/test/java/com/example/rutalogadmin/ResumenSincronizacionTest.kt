package com.example.rutalogadmin

import com.example.rutalogadmin.data.repository.ResultadoSync
import com.example.rutalogadmin.viewmodel.describirSincronizacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** RF14: el aviso de sincronización no debe decir "Sincronizado" cuando hubo un rechazo o un fallo. */
class ResumenSincronizacionTest {

    @Test
    fun sinCambios_todoAlDia() {
        assertEquals("Todo al día. 40 envíos descargados.", describirSincronizacion(ResultadoSync(descargados = 40)))
    }

    @Test
    fun cambiosEnviados_sincronizado() {
        assertEquals(
            "Sincronizado: 2 cambio(s) enviado(s). 40 envíos descargados.",
            describirSincronizacion(ResultadoSync(enviadas = 2, descargados = 40))
        )
    }

    @Test
    fun rechazo_noDiceSincronizado() {
        val texto = describirSincronizacion(ResultadoSync(enviadas = 1, rechazadas = 1, descargados = 43))

        assertEquals(
            "Sincronización terminada con 1 cambio(s) rechazado(s) por el servidor. 1 cambio(s) enviado(s). 43 envíos descargados.",
            texto
        )
        assertFalse(texto.startsWith("Sincronizado"))
    }

    @Test
    fun falloDeRed_indicaQueNoSeCompleto() {
        val texto = describirSincronizacion(ResultadoSync(enviadas = 1, error = "No se pudo conectar con el servidor."))

        assertEquals("No se completó: No se pudo conectar con el servidor. (1 cambio(s) enviado(s))", texto)
    }
}
