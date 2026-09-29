package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.local.RutaConCarga
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.BannerDegradado
import com.example.rutalogadmin.ui.components.EstadoVacio
import com.example.rutalogadmin.ui.components.PestanaOperador
import com.example.rutalogadmin.ui.components.color
import com.example.rutalogadmin.ui.components.formatoTiempo
import com.example.rutalogadmin.ui.components.icono

/** RF10: rutas guardadas en Room con su carga en curso y el tiempo estimado de llegada. */
@Composable
fun RutasScreen(
    nombreUsuario: String,
    rutas: List<RutaConCarga>,
    totalRutas: Int,
    rutasActivas: Int,
    enviosEnCurso: Int,
    soloActivas: Boolean,
    onSoloActivas: (Boolean) -> Unit,
    onRuta: (RutaConCarga) -> Unit,
    onPestana: (PestanaOperador) -> Unit,
    onCerrarSesion: () -> Unit
) {
    AppScaffold(
        titulo = "Rutas activas",
        subtitulo = "Operador · $nombreUsuario",
        pestana = PestanaOperador.RUTAS,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BannerDegradado {
                    Text(
                        text = "Operación en curso",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$rutasActivas de $totalRutas rutas con carga",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Dato(rutasActivas.toString(), "Activas", Modifier.weight(1f))
                        Dato(enviosEnCurso.toString(), "Envíos en curso", Modifier.weight(1f))
                        Dato((totalRutas - rutasActivas).toString(), "Sin carga", Modifier.weight(1f))
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = soloActivas,
                        onClick = { onSoloActivas(true) },
                        label = { Text("Activas ($rutasActivas)") }
                    )
                    FilterChip(
                        selected = !soloActivas,
                        onClick = { onSoloActivas(false) },
                        label = { Text("Todas ($totalRutas)") }
                    )
                }
            }

            if (rutas.isEmpty()) {
                item {
                    EstadoVacio(
                        icono = Icons.Default.Map,
                        titulo = "Sin rutas activas",
                        mensaje = "Ninguna ruta lleva carga recogida, en tránsito o en reparto."
                    )
                }
            }

            items(rutas, key = { it.id }) { ruta ->
                RutaCard(
                    ruta = ruta,
                    onClick = { onRuta(ruta) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RutaCard(
    ruta: RutaConCarga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catalogo = CatalogoRutas.porNombre(ruta.nombre)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ruta.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (catalogo != null) {
                        Text(
                            text = "${catalogo.region} · ${catalogo.zona} · ${catalogo.distanciaKm} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                EtiquetaActiva(activa = ruta.esActiva)
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tiempo estimado de llegada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "~${formatoTiempo(ruta.tiempoEstimado)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConteoEstado(EstadoEnvio.RECOGIDO, ruta.recogidos)
                ConteoEstado(EstadoEnvio.EN_TRANSITO, ruta.enTransito)
                ConteoEstado(EstadoEnvio.EN_REPARTO, ruta.enReparto)
                ConteoEstado(EstadoEnvio.PENDIENTE, ruta.pendientes)
            }

            Text(
                text = if (ruta.totalEnvios == 0) {
                    "Sin envíos registrados en esta ruta"
                } else {
                    "${ruta.totalEnvios} envíos en total · toca para verlos"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EtiquetaActiva(activa: Boolean) {
    val color = if (activa) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
    Surface(color = color.copy(alpha = 0.14f), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (activa) "Activa" else "Sin carga",
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConteoEstado(estado: EstadoEnvio, cantidad: Int) {
    val alfa = if (cantidad > 0) 1f else 0.45f
    Surface(
        color = estado.color.copy(alpha = 0.12f * alfa),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = estado.icono,
                contentDescription = null,
                tint = estado.color.copy(alpha = alfa),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$cantidad ${estado.etiqueta.lowercase()}",
                color = estado.color.copy(alpha = alfa),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun Dato(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valor,
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
