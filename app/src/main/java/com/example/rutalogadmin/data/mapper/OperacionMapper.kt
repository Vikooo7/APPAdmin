package com.example.rutalogadmin.data.mapper

import com.example.rutalogadmin.data.local.entities.CourierEntity
import com.example.rutalogadmin.data.remote.dto.CourierDto
import com.example.rutalogadmin.data.remote.dto.FallidaDto
import com.example.rutalogadmin.model.Courier
import com.example.rutalogadmin.model.EntregaFallida
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.IntentoFallido
import java.time.Instant

/** Room → modelo de la app. */
fun CourierEntity.aModelo(): Courier =
    Courier(id = id, nombre = nombre, placa = placa, zona = zona, capacidad = capacidad)

/** API → Room. */
fun CourierDto.aEntidad(): CourierEntity = CourierEntity(
    id = id,
    nombre = nombre,
    placa = placa ?: "—",
    zona = zona ?: SIN_ZONA,
    capacidad = capacidad
)

/** API → modelo. Las entregas fallidas se consultan en línea: no se guardan en Room. */
fun FallidaDto.aModelo(): EntregaFallida = EntregaFallida(
    idRemoto = envio.id,
    numeroGuia = envio.numeroGuia,
    ruta = envio.ruta,
    estado = EstadoEnvio.desde(envio.estado),
    version = envio.version,
    courierId = envio.courierId,
    transportista = envio.transportistaAsignado,
    destinatario = envio.destinatarioNombre,
    montoContraentrega = envio.montoContraentrega ?: 0.0,
    intentos = intentos.orEmpty().map { intento ->
        IntentoFallido(
            numero = intento.numero,
            motivo = intento.motivo,
            fecha = intento.fecha?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            courierNombre = intento.courierNombre,
            tieneFoto = !intento.fotoUrl.isNullOrBlank()
        )
    },
    maxIntentos = maxIntentos ?: EstadoEnvio.MAX_INTENTOS
)

const val SIN_ZONA = "Sin zona"
