package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.remote.dto.RiesgoEntregaDto
import com.example.rutalogadmin.model.AccionConfirmacion
import com.example.rutalogadmin.model.ReglasRiesgo
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.BannerDegradado
import com.example.rutalogadmin.ui.components.EstadoVacio
import com.example.rutalogadmin.ui.components.MensajeError
import com.example.rutalogadmin.ui.components.MensajeInfo
import com.example.rutalogadmin.ui.components.formatoSoles
import com.example.rutalogadmin.viewmodel.RiesgoUiState

private val RojoRiesgo = Color(0xFFC62828)
private val VerdeOk = Color(0xFF2E7D32)
private val Ambar = Color(0xFFB26A00)

/**
 * RF-O02 / HU-O01: entregas con riesgo alto de fallar (ML-01) para avisar antes al destinatario.
 * El operador pide la confirmación del horario por SMS (simulado) y registra la respuesta.
 */
@Composable
fun RiesgoScreen(
    estado: RiesgoUiState,
    onCargar: () -> Unit,
    onSoloAltos: (Boolean) -> Unit,
    onPedir: (RiesgoEntregaDto) -> Unit,
    onConfirmar: (RiesgoEntregaDto) -> Unit,
    onVolver: () -> Unit
) {
    LaunchedEffect(Unit) { onCargar() }
    val r = estado.respuesta
    AppScaffold(titulo = "Riesgo de falla", subtitulo = "ML-01 · aviso previo al destinatario", onVolver = onVolver) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BannerDegradado {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = r?.let { "Entregas para el ${ReglasRiesgo.textoFecha(it.fecha)}" } ?: "Entregas de mañana",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = r?.let { "${it.resumen.riesgoAlto} de ${it.resumen.entregas} con riesgo alto" } ?: "Calculando…",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            r?.let {
                                Text(
                                    text = "Avisados: ${it.resumen.confirmacionPedida} · riesgo alto desde ${ReglasRiesgo.porcentaje(it.modelo.umbralRiesgoAlto)}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (estado.cargando) {
                            CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onCargar) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White) }
                        }
                    }
                }
            }

            estado.mensaje?.let { m -> item { if (estado.mensajeEsError) MensajeError(m) else MensajeInfo(m) } }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Solo riesgo alto", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = estado.soloAltos, onCheckedChange = onSoloAltos)
                }
            }

            if (r != null && estado.visibles.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Default.TaskAlt,
                        titulo = "Sin entregas de riesgo alto",
                        mensaje = "Ninguna entrega abierta supera el umbral del modelo."
                    )
                }
            }

            items(estado.visibles, key = { it.envio.id }) { e ->
                FilaRiesgo(
                    e = e,
                    procesando = estado.procesando == e.envio.id,
                    onPedir = { onPedir(e) },
                    onConfirmar = { onConfirmar(e) }
                )
            }

            item {
                Text(
                    text = "Modelo: regresión logística entrenada con 9 semanas de histórico simulado (notebook RutaLog_ML). " +
                        "Usa franja, zona, intentos previos, contraentrega, monto, historial del destinatario y si confirmó el horario. " +
                        "La franja sale del orden de la parada en la ruta del día (5 paradas por franja de 2 h).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilaRiesgo(e: RiesgoEntregaDto, procesando: Boolean, onPedir: () -> Unit, onConfirmar: () -> Unit) {
    val color = if (e.riesgoAlto) RojoRiesgo else VerdeOk
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.envio.numeroGuia, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = listOfNotNull(e.envio.ruta, e.zona).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(ReglasRiesgo.porcentaje(e.probabilidad), color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { e.probabilidad.toFloat() }, color = color, modifier = Modifier.fillMaxWidth())
            Text(
                text = buildString {
                    append(e.courier ?: "Sin courier")
                    append(" · franja ${ReglasRiesgo.textoFranja(e.franja)}")
                    if ((e.envio.montoContraentrega ?: 0.0) > 0) append(" · cobra ${formatoSoles(e.envio.montoContraentrega ?: 0.0)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (e.factores.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    e.factores.forEach { Etiqueta(it, Ambar) }
                }
            }
            when (ReglasRiesgo.accion(e.confirmacion)) {
                AccionConfirmacion.PEDIR -> Button(onClick = onPedir, enabled = !procesando, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Text(if (procesando) "  Enviando SMS…" else "  Pedir confirmación del horario")
                }
                AccionConfirmacion.REGISTRAR_RESPUESTA -> {
                    Etiqueta("SMS enviado · esperando respuesta", Ambar)
                    OutlinedButton(onClick = onConfirmar, enabled = !procesando, modifier = Modifier.fillMaxWidth()) {
                        Text(if (procesando) "Guardando…" else "Respondió \"1\": confirmó el horario")
                    }
                }
                AccionConfirmacion.NINGUNA -> Etiqueta("Horario confirmado por el destinatario", VerdeOk)
                AccionConfirmacion.SIN_TELEFONO -> Etiqueta("Sin celular del destinatario: no se puede avisar", RojoRiesgo)
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Text(
            texto,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
