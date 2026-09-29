package com.example.rutalogadmin.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsuarioDao {

    /** Registrar un nuevo operador. */
    @Insert
    suspend fun registrar(usuario: Usuario)

    /** Iniciar sesión validando correo y clave. */
    @Query("SELECT * FROM usuarios WHERE correo = :correo AND clave = :clave LIMIT 1")
    suspend fun login(correo: String, clave: String): Usuario?

    /** Evita correos repetidos y permite distinguir los errores del login. */
    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): Usuario?
}
