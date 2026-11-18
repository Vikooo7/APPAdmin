package com.example.rutalogadmin.data.remote.dto

/** Fase de pagos con Banco Andino. La app nunca habla con el banco: todo pasa por la API de RutaLog. */

/** Lo que se le debe a cada remitente ahora: cobros confirmados sin liquidar (RN-04). */
data class PendienteRemitenteDto(
    val remitenteId: Int,
    val remitente: String,
    val cuentaBanco: String?,
    val cobros: Int,
    val recaudado: Double,
    val costoEnvios: Double,
    val neto: Double
)

data class LiquidacionDto(
    val id: Int,
    val remitenteId: Int,
    val remitente: String?,
    val periodo: String,
    val recaudado: Double,
    val costoEnvios: Double,
    val neto: Double,
    val cobros: Int?,
    /** CALCULADA o TRANSFERIDA. */
    val estado: String,
    val cuentaDestino: String?,
    val nroOperacion: String?,
    val creadoEn: String?,
    val transferidaEn: String?
)

data class LiquidacionesResponse(val pendiente: List<PendienteRemitenteDto>?, val datos: List<LiquidacionDto>)
data class LiquidacionesCalculadasResponse(val datos: List<LiquidacionDto>)
data class AprobarRequest(val uuidOperacion: String)

/** RN-07: lo único que se comparte con el banco: facturación y n.º de envíos por mes. */
data class FacturacionMesDto(val mes: String, val facturacion: Double, val envios: Int)
data class FacturacionResponse(val datos: List<FacturacionMesDto>)

data class SolicitudCreditoRequest(val monto: Double, val plazoMeses: Int, val proposito: String, val autorizo: Boolean)

data class CuotaDto(val numero: Int, val fecha: String, val cuota: Double, val capital: Double, val interes: Double, val saldo: Double)

data class SolicitudCreditoDto(
    val id: Int,
    val codigoBanco: String?,
    val monto: Double,
    val plazoMeses: Int,
    val proposito: String,
    /** EN_EVALUACION, RECHAZADO o DESEMBOLSADO (lo decide el comité en la web, RN-08). */
    val estado: String,
    val cuota: Double?,
    val tasaAnual: Double?,
    val comentario: String?,
    val nroDesembolso: String?,
    val cronograma: List<CuotaDto>?,
    val creadoEn: String?
)

data class SolicitudesResponse(val datos: List<SolicitudCreditoDto>)
