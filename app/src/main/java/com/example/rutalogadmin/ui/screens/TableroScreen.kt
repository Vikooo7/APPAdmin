package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.remote.dto.IndicadorDto
import com.example.rutalogadmin.data.remote.dto.TableroResponse
import com.example.rutalogadmin.model.ReglasTablero
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.BannerDegradado
import com.example.rutalogadmin.ui.components.MensajeError
import com.example.rutalogadmin.ui.components.formatoSoles
import com.example.rutalogadmin.viewmodel.TableroUiState
import java.time.LocalDate

private val VerdeMejor = Color(0xFF2E7D32)
private val RojoPeor = Color(0xFFC62828)
private val AzulBarra = Color(0xFF1565C0)

/** RF-O05 / HU-O03: indicadores de la operación de la semana frente a la semana anterior. */
@Composable
fun TableroScreen(
    estado: TableroUiState,
    onCargar: (semana: String?) -> Unit,
    onVolver: () -> Unit
) {
    LaunchedEffect(Unit) { onCargar(null) }
    val t = estado.tablero
    AppScaffold(titulo = "Tablero", subtitulo = "Indicadores de la operación", onVolver = onVolver) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CabeceraSemana(t, estado.cargando, onCargar) }
            estado.error?.let { item { MensajeError(it) } }
            if (t != null) {
                // Dos tarjetas por fila.
                t.indicadores.chunked(2).forEach { par ->
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            par.forEach { TarjetaIndicador(it, Modifier.weight(1f)) }
                            if (par.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item { Resumen(t) }
                item { TendenciaFallidas(t) }
                if (t.porCourier.isNotEmpty()) item { EntregasPorCourier(t) }
            }
        }
    }
}

@Composable
private fun CabeceraSemana(t: TableroResponse?, cargando: Boolean, onCargar: (String?) -> Unit) {
    val esActual = t == null || !LocalDate.parse(t.semana.fin).isBefore(LocalDate.now())
    BannerDegradado {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { t?.let { onCargar(ReglasTablero.moverSemana(it.semana.inicio, -1)) } }, enabled = t != null && !cargando) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior", tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (esActual) "Esta semana" else "Semana", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                Text(
                    text = t?.let { ReglasTablero.textoSemana(it.semana.inicio, it.semana.fin) } ?: "Cargando…",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                t?.let {
                    Text(
                        "vs ${ReglasTablero.textoSemana(it.semanaAnterior.inicio, it.semanaAnterior.fin)}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (cargando) {
                CircularProgressIndicator(Modifier.size(24.dp).padding(2.dp), color = Color.White, strokeWidth = 2.dp)
            } else if (esActual) {
                IconButton(onClick = { onCargar(t?.semana?.inicio) }) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White) }
            } else {
                IconButton(onClick = { t?.let { onCargar(ReglasTablero.moverSemana(it.semana.inicio, 1)) } }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Semana siguiente", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TarjetaIndicador(i: IndicadorDto, modifier: Modifier) {
    val mejora = ReglasTablero.esMejora(i)
    val color = when (mejora) {
        true -> VerdeMejor
        false -> RojoPeor
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(i.nombre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ReglasTablero.valor(i), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, maxLines = 1)
            Text(
                ReglasTablero.variacion(i) ?: "sin dato de la semana anterior",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(i.formula, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Resumen(t: TableroResponse) {
    Tarjeta {
        Text("Resumen de la semana", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Linea("Entregas", "${t.entregadas}")
        Linea("Intentos fallidos", "${t.intentosFallidos}")
        Linea("Recaudo pendiente de liquidar", "${formatoSoles(t.recaudoPendiente.monto)} (${t.recaudoPendiente.cobros} cobros)")
        t.ahorroRutaPct?.let { Linea("Ruta ordenada (ML-04)", "${ReglasTablero.formatear(it, "%")} menos km") }
    }
}

/** Tasa de fallidas de las últimas 8 semanas: muestra el efecto del aviso previo (ML-01). */
@Composable
private fun TendenciaFallidas(t: TableroResponse) {
    val maximo = (t.tendencia.mapNotNull { it.fallidasPct }.maxOrNull() ?: 0.0).coerceAtLeast(10.0)
    Tarjeta {
        Text("Tasa de fallidas por semana", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("% de intentos que fallaron · últimas 8 semanas", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            t.tendencia.forEach { s ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(s.fallidasPct?.let { "${it.toInt()}" } ?: "–", style = MaterialTheme.typography.labelSmall)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(((s.fallidasPct ?: 0.0) / maximo).toFloat().coerceIn(0.02f, 0.8f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (s.semana == t.semana.inicio) RojoPeor else RojoPeor.copy(alpha = 0.45f))
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            t.tendencia.forEach { s ->
                Text(
                    s.semana.takeLast(5).split("-").reversed().joinToString("/"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EntregasPorCourier(t: TableroResponse) {
    val maximo = t.porCourier.maxOf { it.entregas + it.fallidas }.coerceAtLeast(1)
    Tarjeta {
        Text("Entregas por courier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Azul: entregadas · rojo: intentos fallidos", style = MaterialTheme.typography.bodySmall)
        t.porCourier.forEach { c ->
            Column(Modifier.padding(top = 6.dp)) {
                Row {
                    Text(c.courier, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${c.entregas} · ${c.fallidas}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                    if (c.entregas > 0) Box(Modifier.weight(c.entregas.toFloat()).fillMaxHeight().background(AzulBarra))
                    if (c.fallidas > 0) Box(Modifier.weight(c.fallidas.toFloat()).fillMaxHeight().background(RojoPeor))
                    val resto = maximo - c.entregas - c.fallidas
                    if (resto > 0) Box(Modifier.weight(resto.toFloat()).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun Linea(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { contenido() }
    }
}
