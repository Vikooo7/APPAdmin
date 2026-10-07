package com.example.rutalogadmin

import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.example.rutalogadmin.model.ReglasOperacion
import com.example.rutalogadmin.model.Transportistas
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** RF08 + RF09: reglas para actualizar el estado y asignar transportista (las mismas de la API). */
class ReglasOperacionTest {

    private val transportista = Transportistas.lista.first()

    private fun envio(estado: EstadoEnvio, transportista: String? = null) = Envio(
        id = 1,
        idRemoto = 1,
        uuid = "00000000-0000-4000-8000-000000000001",
        numeroGuia = "RLP-26-100001-2",
        ruta = "Lima → Arequipa",
        pesoKg = 10.0,
        costoEnvio = 29.0,
        estado = estado.codigo,
        transportistaAsignado = transportista,
        version = 1,
        estadoSync = EstadoSincronizacion.SINCRONIZADO,
        mensajeError = null
    )

    @Test
    fun cualquierEstadoDistintoDePendiente_exigeTransportista() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, null))
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.EN_TRANSITO, null))
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, transportista))
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.RECOGIDO, transportista), EstadoEnvio.EN_TRANSITO, transportista))
    }

    @Test
    fun asignarTransportista_sinCambiarEstado_esValido() {
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.PENDIENTE, transportista))
    }

    @Test
    fun elEstadoNoRetrocede() {
        val enReparto = envio(EstadoEnvio.EN_REPARTO, transportista)
        assertNotNull(ReglasOperacion.validar(enReparto, EstadoEnvio.EN_TRANSITO, transportista))
        assertNotNull(ReglasOperacion.validar(enReparto, EstadoEnvio.PENDIENTE, transportista))
    }

    @Test
    fun entregado_quedaCerrado() {
        val entregado = envio(EstadoEnvio.ENTREGADO, transportista)
        assertNotNull(ReglasOperacion.validar(entregado, EstadoEnvio.ENTREGADO, Transportistas.lista.last()))
    }

    @Test
    fun sinCambios_noSeGuarda() {
        val enTransito = envio(EstadoEnvio.EN_TRANSITO, transportista)
        assertNotNull(ReglasOperacion.validar(enTransito, EstadoEnvio.EN_TRANSITO, transportista))
    }

    @Test
    fun transportistaFueraDeLaLista_esRechazado() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, "Desconocido"))
    }
}
