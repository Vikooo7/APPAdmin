package com.example.rutalogadmin.model

import com.example.rutalogadmin.data.local.Envio

/** Reglas de RF08 (actualizar estado) y RF09 (asignar transportista). */
object ReglasOperacion {

    /** Devuelve el mensaje de error, o null si el cambio es válido. */
    fun validar(envio: Envio, nuevoEstado: EstadoEnvio, transportista: String?): String? = when {
        envio.estaCerrado ->
            "El envío ya fue entregado: su estado y transportista no se pueden cambiar."
        nuevoEstado == EstadoEnvio.PENDIENTE && envio.estadoEnvio != EstadoEnvio.PENDIENTE ->
            "Un envío recogido no puede volver a pendiente."
        nuevoEstado.paso < envio.estadoEnvio.paso ->
            "El estado solo puede avanzar: el envío ya está ${envio.estadoEnvio.etiqueta.lowercase()}."
        nuevoEstado.paso >= EstadoEnvio.EN_TRANSITO.paso && transportista == null ->
            "Asigna un transportista antes de pasar el envío a ${nuevoEstado.etiqueta.lowercase()}."
        transportista != null && transportista !in Transportistas.lista ->
            "Elige un transportista de la lista."
        nuevoEstado == envio.estadoEnvio && transportista == envio.transportistaAsignado ->
            "No hay cambios que guardar."
        else -> null
    }
}
