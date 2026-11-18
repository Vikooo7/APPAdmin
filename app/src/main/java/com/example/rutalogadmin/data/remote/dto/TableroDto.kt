package com.example.rutalogadmin.data.remote.dto

/** RF-O05 / HU-O03: tablero de la operación con comparación semanal. */

data class SemanaDto(val inicio: String, val fin: String)

data class IndicadorDto(
    val clave: String,
    val nombre: String,
    /** %, km, S/ o días. */
    val unidad: String,
    /** "sube" o "baja": hacia dónde es mejor. */
    val mejorSi: String,
    val formula: String,
    val valor: Double?,
    val anterior: Double?,
    /** valor − anterior (en puntos porcentuales si la unidad es %). */
    val variacion: Double?
)

data class RecaudoPendienteDto(val monto: Double, val cobros: Int)
data class CourierSemanaDto(val courier: String, val entregas: Int, val fallidas: Int)
data class SemanaTendenciaDto(val semana: String, val entregas: Int, val fallidas: Int, val fallidasPct: Double?)

data class TableroResponse(
    val semana: SemanaDto,
    val semanaAnterior: SemanaDto,
    val entregadas: Int,
    val intentosFallidos: Int,
    val indicadores: List<IndicadorDto>,
    /** ML-04: % menos km que en el orden de registro. */
    val ahorroRutaPct: Double?,
    val recaudoPendiente: RecaudoPendienteDto,
    val porCourier: List<CourierSemanaDto>,
    val tendencia: List<SemanaTendenciaDto>
)
