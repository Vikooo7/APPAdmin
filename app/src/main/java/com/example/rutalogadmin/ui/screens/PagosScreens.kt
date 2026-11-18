package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.remote.dto.LiquidacionDto
import com.example.rutalogadmin.data.remote.dto.PendienteRemitenteDto
import com.example.rutalogadmin.data.remote.dto.SolicitudCreditoDto
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.MensajeError
import com.example.rutalogadmin.ui.components.MensajeInfo
import com.example.rutalogadmin.ui.components.formatoSoles
import com.example.rutalogadmin.viewmodel.CreditoUiState
import com.example.rutalogadmin.viewmodel.LiquidacionUiState

private val VerdeOk = Color(0xFF2E7D32)
private val Ambar = Color(0xFFB26A00)
private val RojoMal = Color(0xFFC62828)

/**
 * RF-O03 / RF-O04: liquidación a remitentes (martes y viernes). El operador calcula el corte; solo el
 * administrador aprueba y entonces Banco Andino transfiere. Solo entran cobros confirmados por el banco (RN-04).
 */
@Composable
fun LiquidacionScreen(
    estado: LiquidacionUiState,
    esAdministrador: Boolean,
    onCargar: () -> Unit,
    onCalcular: () -> Unit,
    onAprobar: (LiquidacionDto) -> Unit,
    onVolver: () -> Unit
) {
    LaunchedEffect(Unit) { onCargar() }
    AppScaffold(titulo = "Liquidaciones", subtitulo = "Contraentrega a remitentes", onVolver = onVolver) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Cabecera("Por liquidar ahora", estado.cargando, onCargar) }
            estado.mensaje?.let { m -> item { if (estado.mensajeEsError) MensajeError(m) else MensajeInfo(m) } }
            if (estado.pendiente.isEmpty() && !estado.cargando) {
                item { Text("No hay cobros confirmados pendientes de liquidar.", style = MaterialTheme.typography.bodySmall) }
            }
            items(estado.pendiente, key = { "p-${it.remitenteId}" }) { FilaPendiente(it) }
            if (estado.pendiente.isNotEmpty()) {
                item {
                    Button(
                        onClick = onCalcular,
                        enabled = !estado.calculando,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text(if (estado.calculando) "Calculando con el banco…" else "Calcular liquidación (corte de hoy)") }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Historial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!esAdministrador) {
                    Text("Las liquidaciones calculadas las aprueba el administrador.", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(estado.liquidaciones, key = { "l-${it.id}" }) { l ->
                FilaLiquidacion(l, esAdministrador, aprobando = estado.aprobando == l.id, onAprobar = { onAprobar(l) })
            }
        }
    }
}

@Composable
private fun FilaPendiente(p: PendienteRemitenteDto) {
    Tarjeta {
        Text(p.remitente, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("${p.cobros} cobro(s) confirmado(s)", style = MaterialTheme.typography.bodySmall)
        Fila("Recaudado", formatoSoles(p.recaudado))
        Fila("Costo de envíos sin pagar", "− ${formatoSoles(p.costoEnvios)}")
        HorizontalDivider()
        Fila("Neto a transferir", formatoSoles(p.neto), negrita = true)
        if (p.cuentaBanco == null) {
            Text("Sin cuenta Banco Andino registrada: no se podrá transferir.", color = RojoMal, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Cuenta ${p.cuentaBanco}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FilaLiquidacion(l: LiquidacionDto, esAdministrador: Boolean, aprobando: Boolean, onAprobar: () -> Unit) {
    val transferida = l.estado == "TRANSFERIDA"
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.remitente.orEmpty(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Corte ${l.periodo} · ${l.cobros ?: 0} cobro(s)", style = MaterialTheme.typography.bodySmall)
            }
            Chip(if (transferida) "TRANSFERIDA" else "CALCULADA", if (transferida) VerdeOk else Ambar)
        }
        Fila("Recaudado − envíos", "${formatoSoles(l.recaudado)} − ${formatoSoles(l.costoEnvios)}")
        Fila("Neto", formatoSoles(l.neto), negrita = true)
        if (transferida) {
            Text("Operación ${l.nroOperacion} → ${l.cuentaDestino}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        } else if (esAdministrador) {
            Button(onClick = onAprobar, enabled = !aprobando, modifier = Modifier.fillMaxWidth()) {
                Text(if (aprobando) "Transfiriendo con Banco Andino…" else "Aprobar y transferir ${formatoSoles(l.neto)}")
            }
        }
    }
}

/**
 * RF-O06: Mi crédito (solo administrador). Se comparte con el banco únicamente la facturación por mes y el número
 * de envíos (RN-07); el comité decide en la web del banco (RN-08) y, si aprueba, el banco desembolsa.
 */
@Composable
fun CreditoScreen(
    estado: CreditoUiState,
    onCargar: () -> Unit,
    onSolicitar: (monto: String, plazo: String, proposito: String, autorizo: Boolean) -> Unit,
    onVolver: () -> Unit
) {
    LaunchedEffect(Unit) { onCargar() }
    var monto by rememberSaveable { mutableStateOf("70000") }
    var plazo by rememberSaveable { mutableStateOf("48") }
    var proposito by rememberSaveable { mutableStateOf("Furgoneta para reparto") }
    var autorizo by rememberSaveable { mutableStateOf(false) }

    AppScaffold(titulo = "Mi crédito", subtitulo = "Banco Andino", onVolver = onVolver) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Cabecera("Facturación que se compartiría", estado.cargando, onCargar) }
            item {
                Tarjeta {
                    if (estado.facturacion.isEmpty()) Text("Sin datos de facturación todavía.", style = MaterialTheme.typography.bodySmall)
                    estado.facturacion.forEach { Fila(it.mes, "${formatoSoles(it.facturacion)} · ${it.envios} envíos") }
                    if (estado.facturacion.isNotEmpty()) {
                        HorizontalDivider()
                        Fila("Promedio mensual", formatoSoles(estado.facturacion.map { it.facturacion }.average()), negrita = true)
                    }
                }
            }
            item {
                Tarjeta {
                    Text("Nueva solicitud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monto (S/)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = plazo,
                        onValueChange = { plazo = it.filter(Char::isDigit) },
                        label = { Text("Plazo (6 a 84 meses)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = proposito, onValueChange = { proposito = it }, label = { Text("Propósito") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = autorizo, onCheckedChange = { autorizo = it })
                        Text(
                            "Autorizo compartir con Banco Andino solo la facturación por mes y el número de envíos. " +
                                "No se envían datos de remitentes ni destinatarios.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { onSolicitar(monto, plazo, proposito, autorizo) },
                        enabled = !estado.enviando,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (estado.enviando) "Enviando al banco…" else "Solicitar crédito") }
                }
            }
            estado.mensaje?.let { m -> item { if (estado.mensajeEsError) MensajeError(m) else MensajeInfo(m) } }
            item { Text("Mis solicitudes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(estado.solicitudes, key = { it.id }) { FilaSolicitud(it) }
        }
    }
}

@Composable
private fun FilaSolicitud(s: SolicitudCreditoDto) {
    val (texto, color) = when (s.estado) {
        "DESEMBOLSADO" -> "DESEMBOLSADO" to VerdeOk
        "RECHAZADO" -> "RECHAZADO" to RojoMal
        else -> "EN EVALUACIÓN" to Ambar
    }
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${s.codigoBanco} · ${s.proposito}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${formatoSoles(s.monto)} a ${s.plazoMeses} meses", style = MaterialTheme.typography.bodySmall)
            }
            Chip(texto, color)
        }
        if (s.estado == "EN_EVALUACION") {
            Text("Lo decide el comité de crédito en la web del banco.", style = MaterialTheme.typography.bodySmall)
        }
        s.comentario?.let { Text("Comité: $it", style = MaterialTheme.typography.bodySmall) }
        s.cuota?.let {
            Fila("Cuota mensual", formatoSoles(it), negrita = true)
            s.tasaAnual?.let { t -> Fila("Tasa efectiva anual", "%.1f %%".format(t * 100)) }
            s.nroDesembolso?.let { n -> Text("Desembolso: operación $n", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary) }
            val cronograma = s.cronograma.orEmpty()
            if (cronograma.isNotEmpty()) {
                Text("Primeras cuotas", style = MaterialTheme.typography.labelMedium)
                cronograma.take(3).forEach { c -> Fila("${c.numero}. ${c.fecha}", "${formatoSoles(c.cuota)} (interés ${formatoSoles(c.interes)})") }
                Text("… ${cronograma.size} cuotas en total", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Cabecera(titulo: String, cargando: Boolean, onCargar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (cargando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onCargar) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar") }
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun Chip(texto: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(texto, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { contenido() }
    }
}
