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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.local.Envio
import com.example.rutalogadmin.data.local.Ruta
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.estaCerrado
import com.example.rutalogadmin.model.estadoEnvio
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.BannerDegradado
import com.example.rutalogadmin.ui.components.EstadoBadge
import com.example.rutalogadmin.ui.components.LineaDeTiempo
import com.example.rutalogadmin.ui.components.MensajeInfo
import com.example.rutalogadmin.ui.components.TarjetaSeccion
import com.example.rutalogadmin.ui.components.formatoPeso
import com.example.rutalogadmin.ui.components.formatoSoles
import com.example.rutalogadmin.ui.components.formatoTiempo

/**
 * Detalle del envío: estado, línea de tiempo y datos. Desde aquí el operador abre
 * el formulario para actualizar el estado (RF08) y el transportista (RF09).
 * [ruta] es la fila de la tabla rutas que corresponde al envío (tiempo estimado).
 */
@Composable
fun DetailScreen(
    envio: Envio?,
    ruta: Ruta?,
    onVolver: () -> Unit,
    onActualizar: (Envio) -> Unit
) {
    AppScaffold(titulo = "Detalle del envío", subtitulo = envio?.numeroGuia, onVolver = onVolver) {
        if (envio == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AppScaffold
        }

        val estado = envio.estadoEnvio
        val catalogo = CatalogoRutas.porNombre(envio.ruta)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BannerDegradado {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "N° de guía",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = envio.numeroGuia,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        EstadoBadge(estado = estado, sobreFondoOscuro = true)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = envio.ruta,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val detalleRuta = listOfNotNull(
                        catalogo?.let { "${it.region} (${it.zona})" },
                        ruta?.let { "llegada estimada ~${formatoTiempo(it.tiempoEstimado)}" },
                        catalogo?.modalidad
                    )
                    if (detalleRuta.isNotEmpty()) {
                        Text(
                            text = detalleRuta.joinToString(" · "),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                if (envio.estaCerrado) {
                    MensajeInfo("Envío entregado: la operación está cerrada y ya no se puede modificar.")
                } else {
                    Button(
                        onClick = { onActualizar(envio) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Text("  Actualizar estado y transportista")
                    }
                }
            }

            item {
                TarjetaSeccion(titulo = "Línea de tiempo", icono = Icons.Default.Timeline) {
                    LineaDeTiempo(estado = estado)
                }
            }

            item {
                TarjetaSeccion(titulo = "Datos del envío", icono = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilaDato(Icons.Default.Map, "Ruta", envio.ruta)
                        if (catalogo != null) {
                            FilaDato(Icons.Default.Place, "Región de destino", "${catalogo.region} · zona ${catalogo.zona}")
                        }
                        if (ruta != null) {
                            FilaDato(
                                Icons.Default.Schedule,
                                "Tiempo estimado de llegada",
                                "~${formatoTiempo(ruta.tiempoEstimado)} (${ruta.tiempoEstimado} h de viaje)"
                            )
                        }
                        FilaDato(Icons.Default.Scale, "Peso", formatoPeso(envio.pesoKg))
                        FilaDato(Icons.Default.Payments, "Costo del envío", formatoSoles(envio.costoEnvio))
                        FilaDato(
                            Icons.Default.DirectionsCar,
                            "Transportista asignado",
                            envio.transportistaAsignado ?: "Por asignar"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaDato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = valor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
