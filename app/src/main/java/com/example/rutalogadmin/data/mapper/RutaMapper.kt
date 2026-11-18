package com.example.rutalogadmin.data.mapper

import com.example.rutalogadmin.data.local.entities.RutaEntity
import com.example.rutalogadmin.data.remote.dto.RutaDto
import com.example.rutalogadmin.model.Ruta

/** Room → modelo de la app. */
fun RutaEntity.aModelo(): Ruta = Ruta(id = id, nombre = nombre, tiempoEstimado = tiempoEstimado, zona = zona)

/** API → Room. */
fun RutaDto.aEntidad(): RutaEntity = RutaEntity(id = id, nombre = nombre, tiempoEstimado = tiempoEstimado, zona = zona)
