package com.example.rutalogadmin.data.remote.dto

/** ML-01 · riesgo de entrega fallida (RF-O02). Lo calcula la API con el modelo entrenado en el notebook. */

data class ModeloRiesgoDto(
    val nombre: String,
    /** Probabilidad desde la que una entrega es "riesgo alto" (≈ 0.11). */
    val umbralRiesgoAlto: Double
)

data class ResumenRiesgoDto(val entregas: Int, val riesgoAlto: Int, val confirmacionPedida: Int)

data class ConfirmacionDto(
    /** null (no se pidió), PEDIDA o CONFIRMADA. */
    val estado: String?,
    val pedidaEn: String?,
    val confirmadaEn: String?,
    val sinTelefono: Boolean
)

data class RiesgoEntregaDto(
    val envio: EnvioDto,
    val zona: String?,
    val courier: String?,
    /** Franja estimada por el orden en la ruta del día (8, 10, ... 18); null si aún no tiene ruta. */
    val franja: Int?,
    val probabilidad: Double,
    val riesgoAlto: Boolean,
    /** Hasta 3 razones que más suben el riesgo, en palabras. */
    val factores: List<String>,
    val confirmacion: ConfirmacionDto
)

data class RiesgoResponse(
    val fecha: String,
    val modelo: ModeloRiesgoDto,
    val resumen: ResumenRiesgoDto,
    val datos: List<RiesgoEntregaDto>
)

/** UUID de la operación: si se reintenta, el servidor no envía dos SMS (RF15). */
data class ConfirmacionRequest(val uuidOperacion: String)

data class AvisoSmsDto(val canal: String, val destino: String, val mensaje: String)

data class ConfirmacionResponse(
    val envioId: Int,
    val numeroGuia: String,
    val estado: String,
    val aviso: AvisoSmsDto? = null
)
