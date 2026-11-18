package com.example.rutalogadmin

import com.example.rutalogadmin.model.Courier
import com.example.rutalogadmin.model.CourierConCarga
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.example.rutalogadmin.model.ReglasOperacion
import com.example.rutalogadmin.model.calcularCarga
import com.example.rutalogadmin.viewmodel.OperacionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** RF08 + RF09: reglas para actualizar el estado y asignar courier (las mismas de la API). */
class ReglasOperacionTest {

    private val carlos = Courier(id = 7, nombre = "Carlos Quispe Mamani", placa = "AQP-482", zona = "Sur", capacidad = 30)
    private val rosa = Courier(id = 3, nombre = "Rosa Huamán Torres", placa = "BFK-209", zona = "Lima y Callao", capacidad = 30)
    private val conCupo = CourierConCarga(carlos, activos = 12)
    private val otroConCupo = CourierConCarga(rosa, activos = 5)
    private val lleno = CourierConCarga(rosa, activos = 30)

    private fun envio(
        estado: EstadoEnvio,
        courier: Courier? = null,
        id: Int = 1,
        sync: EstadoSincronizacion = EstadoSincronizacion.SINCRONIZADO
    ) = Envio(
        id = id,
        idRemoto = id,
        uuid = "00000000-0000-4000-8000-00000000000$id",
        numeroGuia = "RLP-26-10000$id-2",
        ruta = "Lima → Arequipa",
        pesoKg = 10.0,
        costoEnvio = 29.0,
        estado = estado.codigo,
        transportistaAsignado = courier?.etiqueta,
        version = 1,
        estadoSync = sync,
        mensajeError = null,
        courierId = courier?.id
    )

    @Test
    fun cualquierEstadoDistintoDePendiente_exigeCourier() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, null))
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.EN_TRANSITO, null))
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, conCupo))
        // Si ya tiene courier, no hace falta elegirlo otra vez.
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.RECOGIDO, carlos), EstadoEnvio.EN_TRANSITO, null))
    }

    @Test
    fun asignarCourier_sinCambiarEstado_esValido() {
        assertNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.PENDIENTE, conCupo))
    }

    @Test
    fun elEstadoNoRetrocede() {
        val enReparto = envio(EstadoEnvio.EN_REPARTO, carlos)
        assertNotNull(ReglasOperacion.validar(enReparto, EstadoEnvio.EN_TRANSITO, null))
        assertNotNull(ReglasOperacion.validar(enReparto, EstadoEnvio.PENDIENTE, null))
    }

    @Test
    fun entregadoYDevuelto_quedanCerrados() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.ENTREGADO, carlos), EstadoEnvio.ENTREGADO, otroConCupo))
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.DEVUELTO, carlos), EstadoEnvio.ENTREGADO, otroConCupo))
    }

    @Test
    fun sinCambios_noSeGuarda() {
        val enTransito = envio(EstadoEnvio.EN_TRANSITO, carlos)
        assertNotNull(ReglasOperacion.validar(enTransito, EstadoEnvio.EN_TRANSITO, null))
        assertNotNull(ReglasOperacion.validar(enTransito, EstadoEnvio.EN_TRANSITO, conCupo))
    }

    @Test
    fun courierSinCupo_esRechazado() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.PENDIENTE), EstadoEnvio.RECOGIDO, lleno))
    }

    @Test
    fun entregaFallida_seReprogramaAntesDeCambiarElEstado() {
        assertNotNull(ReglasOperacion.validar(envio(EstadoEnvio.FALLIDO, carlos), EstadoEnvio.ENTREGADO, null))
    }

    @Test
    fun reprogramado_vuelveAReparto_peroNoARecogido() {
        val reprogramado = envio(EstadoEnvio.REPROGRAMADO, carlos)
        assertNull(ReglasOperacion.validar(reprogramado, EstadoEnvio.EN_REPARTO, null))
        assertNotNull(ReglasOperacion.validar(reprogramado, EstadoEnvio.RECOGIDO, null))
    }

    @Test
    fun laCargaCuentaSoloEnviosAbiertos() {
        val envios = listOf(
            envio(EstadoEnvio.EN_REPARTO, carlos, id = 1),
            envio(EstadoEnvio.REPROGRAMADO, carlos, id = 2),
            envio(EstadoEnvio.ENTREGADO, carlos, id = 3),
            envio(EstadoEnvio.DEVUELTO, carlos, id = 4),
            envio(EstadoEnvio.PENDIENTE, rosa, id = 5)
        )
        val carga = calcularCarga(listOf(carlos, rosa), envios).associate { it.courier.id to it.activos }
        assertEquals(2, carga[carlos.id])
        assertEquals(1, carga[rosa.id])
    }

    @Test
    fun asignacionPorLote_respetaLaCapacidad() {
        val tres = (1..3).map { envio(EstadoEnvio.PENDIENTE, id = it) }
        val casiLleno = CourierConCarga(carlos, activos = 28)
        assertNotNull(OperacionViewModel.validarAsignacion(tres, casiLleno))
        assertNull(OperacionViewModel.validarAsignacion(tres.take(2), casiLleno))
        assertNotNull(OperacionViewModel.validarAsignacion(emptyList(), conCupo))
        assertNotNull(OperacionViewModel.validarAsignacion(tres, null))
        val conCambioLocal = listOf(envio(EstadoEnvio.PENDIENTE, sync = EstadoSincronizacion.PENDIENTE))
        assertNotNull(OperacionViewModel.validarAsignacion(conCambioLocal, conCupo))
    }
}
