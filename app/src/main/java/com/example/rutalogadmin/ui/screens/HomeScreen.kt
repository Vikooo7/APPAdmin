package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.local.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.estadoEnvio
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.BannerDegradado
import com.example.rutalogadmin.ui.components.EstadoVacio
import com.example.rutalogadmin.ui.components.ItemCard
import com.example.rutalogadmin.ui.components.PestanaOperador
import com.example.rutalogadmin.ui.components.color
import com.example.rutalogadmin.ui.components.icono

/** Inicio del operador (panel de operaciones): resumen de los envíos guardados en Room y accesos a Envíos y Rutas. */
@Composable
fun HomeScreen(
    nombreUsuario: String,
    envios: List<Envio>,
    rutasActivas: Int,
    onVerEstado: (EstadoEnvio?) -> Unit,
    onVerRutas: () -> Unit,
    onEnvio: (Envio) -> Unit,
    onPestana: (PestanaOperador) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val porEstado = envios.groupingBy { it.estadoEnvio }.eachCount()
    val enCurso = envios.count { it.estadoEnvio.enCurso }
    val porAsignar = envios.filter { it.transportistaAsignado == null && it.estadoEnvio != EstadoEnvio.ENTREGADO }

    AppScaffold(
        titulo = "Panel de operaciones",
        subtitulo = "Operador · $nombreUsuario",
        pestana = PestanaOperador.INICIO,
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
                    Text(
                        text = "Hola,",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = nombreUsuario,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Estadistica(envios.size, "Envíos", Modifier.weight(1f))
                        Estadistica(enCurso, "En curso", Modifier.weight(1f))
                        Estadistica(porAsignar.size, "Por asignar", Modifier.weight(1f))
                        Estadistica(porEstado[EstadoEnvio.ENTREGADO] ?: 0, "Entregados", Modifier.weight(1f))
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccionRapida(
                        titulo = "Envíos",
                        subtitulo = "Filtra y actualiza estados",
                        icono = Icons.Default.Inventory,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onVerEstado(null) },
                        modifier = Modifier.weight(1f)
                    )
                    AccionRapida(
                        titulo = "Rutas activas",
                        subtitulo = "$rutasActivas con carga en curso",
                        icono = Icons.Default.Map,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = onVerRutas,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Envíos por estado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        EstadoEnvio.entries.forEach { estado ->
                            FilaEstado(
                                estado = estado,
                                cantidad = porEstado[estado] ?: 0,
                                total = envios.size,
                                onClick = { onVerEstado(estado) }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Por asignar transportista",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (porAsignar.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Default.TaskAlt,
                        titulo = "Todo asignado",
                        mensaje = "Todos los envíos en curso tienen un transportista."
                    )
                }
            }

            items(porAsignar.take(5), key = { it.id }) { envio ->
                ItemCard(envio = envio, onClick = { onEnvio(envio) })
            }
        }
    }
}

@Composable
private fun FilaEstado(
    estado: EstadoEnvio,
    cantidad: Int,
    total: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(estado.color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(estado.icono, contentDescription = null, tint = estado.color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = estado.etiqueta,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = cantidad.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else cantidad / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = estado.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Ver ${estado.etiqueta.lowercase()}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Estadistica(valor: Int, etiqueta: String, modifier: Modifier = Modifier) {
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
private fun AccionRapida(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
