package com.example.rutalogadmin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Perfil del operador autorizado por la API. El id es el del servidor.
 * No se guarda la contraseña: la sesión es un token cifrado.
 */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)
