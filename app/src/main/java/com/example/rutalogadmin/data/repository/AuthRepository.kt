package com.example.rutalogadmin.data.repository

import androidx.room.withTransaction
import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.local.SesionSegura
import com.example.rutalogadmin.data.mapper.aEntidad
import com.example.rutalogadmin.data.mapper.aModelo
import com.example.rutalogadmin.data.remote.ResultadoApi
import com.example.rutalogadmin.data.remote.api.ApiService
import com.example.rutalogadmin.data.remote.dto.LoginRequest
import com.example.rutalogadmin.data.remote.dto.LoginResponse
import com.example.rutalogadmin.data.remote.dto.SocialRequest
import com.example.rutalogadmin.data.remote.llamarApi
import com.example.rutalogadmin.model.RolUsuario
import com.example.rutalogadmin.model.Usuario

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/**
 * Autenticación mediante la API REST.
 * - El token se guarda cifrado (SesionSegura) y el perfil en la tabla usuarios.
 * - Acceso sin conexión: quien ya inició sesión conserva su sesión y entra aunque no haya red.
 *   Un inicio de sesión nuevo siempre necesita conexión, porque la clave no se guarda en el teléfono.
 */
class AuthRepository(
    private val api: ApiService,
    private val db: AppDatabase,
    private val sesion: SesionSegura
) {

    /** Operador con sesión activa, o null si hay que iniciar sesión. */
    suspend fun usuarioActual(): Usuario? =
        if (sesion.token == null) null else db.usuarioDao().actual()?.aModelo()

    suspend fun login(correo: String, clave: String): ResultadoLogin =
        abrirSesion(llamarApi { api.login(LoginRequest(correo, clave)) })

    /** Google (vía Firebase): solo entra si la empresa ya registró ese correo. */
    suspend fun loginConGoogle(idTokenFirebase: String): ResultadoLogin =
        abrirSesion(llamarApi { api.loginSocial(SocialRequest(idTokenFirebase, "admin")) })

    private suspend fun abrirSesion(resultado: ResultadoApi<LoginResponse>): ResultadoLogin =
        when (resultado) {
            is ResultadoApi.Exito -> {
                val perfil = resultado.valor.usuario
                val rol = RolUsuario.desde(perfil.rol)
                if (rol !in RolUsuario.deLaAppAdmin) {
                    ResultadoLogin.Error(
                        if (rol == RolUsuario.COURIER) {
                            "Esta cuenta es de courier. Usa la App Courier."
                        } else {
                            "Esta cuenta no es de operador. Usa la App Cliente."
                        }
                    )
                } else {
                    db.withTransaction {
                        val anterior = db.usuarioDao().actual()
                        // Si entra otro operador en este teléfono, no hereda los cambios sin enviar del anterior.
                        if (anterior != null && anterior.id != perfil.id) {
                            db.envioDao().borrarTodo()
                            db.rutaDao().borrarTodo()
                            db.courierDao().borrarTodo()
                            db.operacionPendienteDao().borrarTodo()
                            db.operacionPendienteDao().borrarMetadata()
                        }
                        db.usuarioDao().borrarTodo()
                        db.usuarioDao().guardar(perfil.aEntidad())
                    }
                    sesion.token = resultado.valor.token
                    ResultadoLogin.Exito(perfil.aEntidad().aModelo())
                }
            }

            is ResultadoApi.Rechazo -> ResultadoLogin.Error(resultado.mensaje)

            is ResultadoApi.Fallo -> ResultadoLogin.Error(
                if (resultado.sinConexion) {
                    "Sin conexión. Necesitas internet para iniciar sesión."
                } else {
                    resultado.mensaje
                }
            )
        }

    /** Cierra la sesión. Los datos locales se conservan por si el mismo operador vuelve a entrar. */
    fun cerrarSesion() {
        sesion.cerrar()
    }
}
