package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.model.EstadoSincronizacion
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.BannerDegradado
import com.example.rutalogadmin.ui.components.MensajeError
import com.example.rutalogadmin.ui.components.MensajeInfo
import com.example.rutalogadmin.ui.components.PestanaOperador
import com.example.rutalogadmin.ui.components.SyncStatusIndicator
import com.example.rutalogadmin.ui.components.formatoFechaHora
import com.example.rutalogadmin.viewmodel.OperacionUi
import com.example.rutalogadmin.viewmodel.SyncUiState

/**
 * RF13 y RF14: sincronización manual, operaciones pendientes, errores y fecha de la última
 * sincronización exitosa. Aquí también se resuelven los conflictos que rechaza el servidor (RF15).
 */
@Composable
fun SyncScreen(
    estado: SyncUiState,
    nombreUsuario: String,
    onSincronizar: () -> Unit,
    onReintentar: (Int) -> Unit,
    onDescartar: (Int) -> Unit,
    onSimularError: (Boolean) -> Unit,
    onPestana: (PestanaOperador) -> Unit,
    onCerrarSesion: () -> Unit
) {
    AppScaffold(
        titulo = "Sincronización",
        subtitulo = "Operador · $nombreUsuario",
        pestana = PestanaOperador.SINCRONIZAR,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BannerDegradado {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (estado.enLinea) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (estado.enLinea) "En línea" else "Sin conexión",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = estado.ultimaSincronizacion
                                    ?.let { "Última sincronización: ${formatoFechaHora(it)}" }
                                    ?: "Aún no se ha sincronizado en este teléfono",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Contador(estado.pendientes, "Pendientes", Modifier.weight(1f))
                        Contador(estado.errores, "Rechazadas", Modifier.weight(1f))
                    }
                }
            }

            item {
                Button(
                    onClick = onSincronizar,
                    enabled = !estado.sincronizando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (estado.sincronizando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("  Sincronizando…")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Text("  Sincronizar ahora")
                    }
                }
            }

            estado.mensaje?.let { mensaje ->
                item {
                    if (estado.mensajeEsError) MensajeError(texto = mensaje) else MensajeInfo(texto = mensaje)
                }
            }

            item {
                Text(
                    text = "Operaciones en cola (${estado.operaciones.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (estado.operaciones.isEmpty()) {
                item {
                    Text(
                        text = "No hay operaciones por enviar. Todos tus cambios ya están en el servidor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(estado.operaciones, key = { it.id }) { operacion ->
                OperacionCard(
                    operacion = operacion,
                    onReintentar = { onReintentar(operacion.id) },
                    onDescartar = { onDescartar(operacion.id) }
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simular error del servidor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Solo para la prueba: la API responde con error 503 y las operaciones se conservan en la cola.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = estado.simularErrorServidor, onCheckedChange = onSimularError)
                    }
                }
            }
        }
    }
}

@Composable
private fun Contador(valor: Int, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valor.toString(),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = etiqueta,
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun OperacionCard(
    operacion: OperacionUi,
    onReintentar: () -> Unit,
    onDescartar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = operacion.descripcion,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = operacion.numeroGuia,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                SyncStatusIndicator(estado = operacion.estado)
            }

            Text(
                text = "Registrada el ${formatoFechaHora(operacion.fechaRegistro)} · intentos: ${operacion.intentos}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            operacion.mensajeError?.let { MensajeError(texto = it) }

            // Conflicto o validación rechazada: el operador decide qué hacer con su cambio.
            if (operacion.estado == EstadoSincronizacion.ERROR) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReintentar, modifier = Modifier.weight(1f)) {
                        Text("Reintentar")
                    }
                    TextButton(onClick = onDescartar, modifier = Modifier.weight(1f)) {
                        Text("Descartar mi cambio")
                    }
                }
            }
        }
    }
}
