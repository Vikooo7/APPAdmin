package com.example.rutalogadmin

import com.example.rutalogadmin.data.local.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.ReglasOperacion
import com.example.rutalogadmin.model.Transportistas
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** RF08 + RF09: reglas para actualizar el estado y asignar transportista. */
class ReglasOperacionTest {

    private val transportista = Transportistas.lista.first()

    private fun envio(estado: EstadoEnvio, transportista: String? = null) = Envio(
        id = 1,
        numeroGuia = "RLP-26-100001-2",
        ruta = "Lima → Arequipa",
        pesoKg = 10.0,
        costoEnvio = 29.0,
        estado = estado.codigo,
        transportistaAsignado = transportista
    )

    @Test
    fun pendiente_puedePasarARecogidoSinTransportista() {
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, null))
    }

    @Test
    fun enTransito_exigeTransportista() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.RECOGIDO), EstadoEnvio.EN_TRANSITO, null))
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.RECOGIDO), EstadoEnvio.EN_TRANSITO, transportista))
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
