package com.example.rutalogadmin.model

/**
 * Reglas de RF08 (actualizar estado) y RF09 (asignar courier).
 * Son las mismas que valida la API: así un cambio guardado sin conexión no se rechaza después.
 */
object ReglasOperacion {

    /**
     * Devuelve el mensaje de error, o null si el cambio es válido.
     * [courier] es el courier elegido; null significa que se mantiene el que ya tiene el envío.
     */
    fun validar(envio: Envio, nuevoEstado: EstadoEnvio, courier: CourierConCarga?): String? {
        val actual = envio.estadoEnvio
        val cambiaCourier = courier != null && courier.courier.id != envio.courierId
        val tieneCourier = courier != null || envio.courierId != null || envio.transportistaAsignado != null
        return when {
            actual == EstadoEnvio.ENTREGADO ->
                "El envío ya fue entregado: su estado y courier no se pueden cambiar."
            actual == EstadoEnvio.DEVUELTO ->
                "El envío fue devuelto al remitente tras ${EstadoEnvio.MAX_INTENTOS} intentos fallidos: ya no se puede cambiar."
            actual == EstadoEnvio.FALLIDO ->
                "La entrega falló: reprográmala desde Entregas fallidas antes de cambiar el estado."
            nuevoEstado.paso < actual.paso ->
                "El estado solo puede avanzar: el envío ya está ${actual.etiqueta.lowercase()}."
            nuevoEstado != EstadoEnvio.PENDIENTE && !tieneCourier ->
                "Asigna un courier antes de pasar el envío a ${nuevoEstado.etiqueta.lowercase()}."
            cambiaCourier && courier!!.lleno ->
                "${courier.courier.nombre} ya tiene ${courier.activos} de ${courier.courier.capacidad} paradas."
            nuevoEstado == actual && !cambiaCourier ->
                "No hay cambios que guardar."
            else -> null
        }
    }
}
