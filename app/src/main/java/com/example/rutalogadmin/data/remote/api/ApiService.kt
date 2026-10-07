package com.example.rutalogadmin.data.remote.api

import com.example.rutalogadmin.data.remote.dto.ActualizarEnvioRequest
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.EnviosResponse
import com.example.rutalogadmin.data.remote.dto.LoginRequest
import com.example.rutalogadmin.data.remote.dto.LoginResponse
import com.example.rutalogadmin.data.remote.dto.RutasResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Endpoints de la API REST de RutaLog (Node.js + Express en Vercel, base de datos en Neon).
 * Solo están los que usa el operador: no registra ni elimina envíos.
 */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): Response<LoginResponse>

    /** Al administrador el servidor le devuelve los envíos de todos los clientes. */
    @GET("api/envios")
    suspend fun obtenerEnvios(): Response<EnviosResponse>

    /** RF08 + RF09: cambia el estado y el transportista de un envío. */
    @PUT("api/envios/{id}")
    suspend fun actualizarEnvio(@Path("id") id: Int, @Body cuerpo: ActualizarEnvioRequest): Response<EnvioDto>

    /** RF10: rutas activas con su tiempo estimado. */
    @GET("api/rutas")
    suspend fun obtenerRutas(): Response<RutasResponse>
}
