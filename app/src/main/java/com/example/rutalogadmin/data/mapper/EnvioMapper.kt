package com.example.rutalogadmin.data.mapper

import com.example.rutalogadmin.data.local.entities.EnvioEntity
import com.example.rutalogadmin.data.local.entities.UsuarioEntity
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.UsuarioDto
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.example.rutalogadmin.model.Usuario

/** Room → modelo de la app. */
fun EnvioEntity.aModelo(): Envio = Envio(
    id = id,
    idRemoto = idRemoto,
    uuid = uuid,
    numeroGuia = numeroGuia,
    ruta = ruta,
    pesoKg = pesoKg,
    costoEnvio = costoEnvio,
    estado = estado,
    transportistaAsignado = transportistaAsignado,
    version = version,
    estadoSync = EstadoSincronizacion.desde(estadoSync),
    mensajeError = mensajeError
)

fun UsuarioEntity.aModelo(): Usuario = Usuario(id = id, nombre = nombre, correo = correo, rol = rol)

/**
 * API → Room. Lo que llega del servidor queda como SINCRONIZADO.
 * [idLocal] es 0 para insertar una fila nueva o el id de la fila local que se actualiza.
 */
fun EnvioDto.aEntidad(idLocal: Int = 0): EnvioEntity = EnvioEntity(
    id = idLocal,
    idRemoto = id,
    uuid = uuid,
    numeroGuia = numeroGuia,
    ruta = ruta,
    pesoKg = pesoKg,
    costoEnvio = costoEnvio,
    estado = estado,
    transportistaAsignado = transportistaAsignado,
    version = version,
    estadoSync = EstadoSincronizacion.SINCRONIZADO.name,
    mensajeError = null
)

fun UsuarioDto.aEntidad(): UsuarioEntity = UsuarioEntity(id = id, nombre = nombre, correo = correo, rol = rol)
