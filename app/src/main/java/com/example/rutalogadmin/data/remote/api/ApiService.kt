package com.example.rutalogadmin.data.remote.api

import com.example.rutalogadmin.data.remote.dto.ActualizarEnvioRequest
import com.example.rutalogadmin.data.remote.dto.AprobarRequest
import com.example.rutalogadmin.data.remote.dto.FacturacionResponse
import com.example.rutalogadmin.data.remote.dto.LiquidacionDto
import com.example.rutalogadmin.data.remote.dto.LiquidacionesCalculadasResponse
import com.example.rutalogadmin.data.remote.dto.LiquidacionesResponse
import com.example.rutalogadmin.data.remote.dto.SolicitudCreditoDto
import com.example.rutalogadmin.data.remote.dto.SolicitudCreditoRequest
import com.example.rutalogadmin.data.remote.dto.SolicitudesResponse
import com.example.rutalogadmin.data.remote.dto.AsignacionRequest
import com.example.rutalogadmin.data.remote.dto.AsignacionResponse
import com.example.rutalogadmin.data.remote.dto.CouriersResponse
import com.example.rutalogadmin.data.remote.dto.EnvioDto
import com.example.rutalogadmin.data.remote.dto.EnviosResponse
import com.example.rutalogadmin.data.remote.dto.FallidasResponse
import com.example.rutalogadmin.data.remote.dto.LoginRequest
import com.example.rutalogadmin.data.remote.dto.LoginResponse
import com.example.rutalogadmin.data.remote.dto.SocialRequest
import com.example.rutalogadmin.data.remote.dto.ReprogramarRequest
import com.example.rutalogadmin.data.remote.dto.RutasResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import com.example.rutalogadmin.data.remote.dto.ConfirmacionRequest
import com.example.rutalogadmin.data.remote.dto.ConfirmacionResponse
import com.example.rutalogadmin.data.remote.dto.RiesgoResponse
import com.example.rutalogadmin.data.remote.dto.PronosticoResponse
import com.example.rutalogadmin.data.remote.dto.TableroResponse

/**
 * Endpoints de la API REST de RutaLog (Node.js + Express en Vercel, base de datos en Neon).
 * Solo están los que usa el operador: no registra ni elimina envíos.
 */
interface ApiService {

    // ---------- Liquidaciones (RF-O03, RF-O04) y Mi crédito (RF-O06) con Banco Andino ----------

    /** Pendiente por remitente (solo cobros confirmados) e historial de liquidaciones. */
    @GET("api/liquidaciones")
    suspend fun obtenerLiquidaciones(): Response<LiquidacionesResponse>

    /** Operador o administrador: crea una liquidación CALCULADA por remitente. */
    @POST("api/liquidaciones/calcular")
    suspend fun calcularLiquidaciones(): Response<LiquidacionesCalculadasResponse>

    /** Solo administrador: Banco Andino transfiere el neto a la cuenta del remitente. */
    @POST("api/liquidaciones/{id}/aprobar")
    suspend fun aprobarLiquidacion(@Path("id") id: Int, @Body cuerpo: AprobarRequest): Response<LiquidacionDto>

    @GET("api/credito/facturacion")
    suspend fun obtenerFacturacion(): Response<FacturacionResponse>

    @GET("api/credito/solicitudes")
    suspend fun obtenerSolicitudesCredito(): Response<SolicitudesResponse>

    @POST("api/credito/solicitudes")
    suspend fun solicitarCredito(@Body cuerpo: SolicitudCreditoRequest): Response<SolicitudCreditoDto>

    /** RF-O05: indicadores de la semana que contiene [semana] (AAAA-MM-DD) frente a la anterior. */
    @GET("api/tablero")
    suspend fun obtenerTablero(@Query("semana") semana: String? = null): Response<TableroResponse>

    // ---------- ML-01: riesgo de entrega fallida y aviso previo (RF-O02, HU-O01) ----------

    /** [fecha] AAAA-MM-DD; sin fecha, la API usa mañana. */
    @GET("api/ml/riesgo-fallida")
    suspend fun obtenerRiesgo(@Query("fecha") fecha: String? = null): Response<RiesgoResponse>

    /** ML-03: envíos esperados por zona (por defecto mañana) y couriers a programar. */
    @GET("api/ml/pronostico-volumen")
    suspend fun obtenerPronostico(@Query("fecha") fecha: String? = null): Response<PronosticoResponse>

    /** Envía al destinatario el SMS (simulado) que pide confirmar el horario. */
    @POST("api/envios/{id}/confirmacion")
    suspend fun pedirConfirmacion(@Path("id") id: Int, @Body cuerpo: ConfirmacionRequest): Response<ConfirmacionResponse>

    /** Riesgo medio: aviso por notificación el mismo día (SMS simulado). */
    @POST("api/envios/{id}/aviso-dia")
    suspend fun enviarAvisoDia(@Path("id") id: Int, @Body cuerpo: ConfirmacionRequest): Response<ConfirmacionResponse>

    /** El destinatario respondió "1": habrá alguien para recibir. */
    @POST("api/envios/{id}/confirmacion/confirmar")
    suspend fun registrarConfirmacion(@Path("id") id: Int, @Body cuerpo: ConfirmacionRequest): Response<ConfirmacionResponse>

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): Response<LoginResponse>

    /** La API valida el token de Firebase (Google) y entrega su propia sesión. */
    @POST("api/auth/social")
    suspend fun loginSocial(@Body cuerpo: SocialRequest): Response<LoginResponse>

    /** Al operador el servidor le devuelve los envíos de todos los clientes. */
    @GET("api/envios")
    suspend fun obtenerEnvios(): Response<EnviosResponse>

    /** RF08 + RF09: cambia el estado y el courier de un envío. */
    @PUT("api/envios/{id}")
    suspend fun actualizarEnvio(@Path("id") id: Int, @Body cuerpo: ActualizarEnvioRequest): Response<EnvioDto>

    /** RF10: rutas activas con su tiempo estimado y su zona. */
    @GET("api/rutas")
    suspend fun obtenerRutas(): Response<RutasResponse>

    /** RF-O01: couriers con su zona y capacidad. */
    @GET("api/couriers")
    suspend fun obtenerCouriers(): Response<CouriersResponse>

    /** HU-O01: asigna varios envíos a un courier en una sola operación. */
    @POST("api/asignaciones")
    suspend fun asignar(@Body cuerpo: AsignacionRequest): Response<AsignacionResponse>

    /** Entregas fallidas con su historial de intentos. */
    @GET("api/fallidas")
    suspend fun obtenerFallidas(): Response<FallidasResponse>

    /** Una entrega fallida vuelve a salir a reparto (estado REPROGRAMADO). */
    @POST("api/envios/{id}/reprogramar")
    suspend fun reprogramar(@Path("id") id: Int, @Body cuerpo: ReprogramarRequest): Response<EnvioDto>
}
