package com.example.rutalogadmin.data.remote.dto

/** POST /api/auth/login */
data class LoginRequest(
    val correo: String,
    val clave: String
)

data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

/** Ingreso con Google: ID token de Firebase y la app desde la que se entra. */
data class SocialRequest(
    val idToken: String,
    val app: String
)

data class LoginResponse(
    val token: String,
    val usuario: UsuarioDto
)
