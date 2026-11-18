package com.example.rutalogadmin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.data.remote.dto.PronosticoResponse
import com.example.rutalogadmin.data.remote.dto.RiesgoEntregaDto
import com.example.rutalogadmin.data.remote.dto.ZonaPronosticoDto
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.ReglasRiesgo
import com.example.rutalogadmin.model.CourierConCarga
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.estadoEnvio
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.EstadoBadge
import com.example.rutalogadmin.ui.components.EstadoVacio
import com.example.rutalogadmin.ui.components.LocalEnLinea
import com.example.rutalogadmin.ui.components.MensajeInfo
import com.example.rutalogadmin.ui.components.SelectorDesplegable
import com.example.rutalogadmin.viewmodel.AsignacionUiState

/**
 * HU-O01 / RF-O01: asignar los envíos del día a los couriers por zona y capacidad
 * (máximo 30 paradas por courier). Se hace en línea: el servidor valida la capacidad en ese momento.
 */
@Composable
fun AsignacionScreen(
    estado: AsignacionUiState,
    envios: List<Envio>,
    couriers: List<CourierConCarga>,
    puedeAsignarse: (Envio) -> Boolean,
    onZona: (String?) -> Unit,
    onSoloSinCourier: (Boolean) -> Unit,
    onAlternar: (Envio) -> Unit,
    onSeleccionarTodos: () -> Unit,
    onLimpiarSeleccion: () -> Unit,
    onCourier: (CourierConCarga?) -> Unit,
    onAsignar: () -> Unit,
    onMensajeMostrado: () -> Unit,
    onVolver: () -> Unit,
    /** ML-01: entregas de riesgo alto por número de guía (RF-O02). */
    riesgoAlto: Map<String, RiesgoEntregaDto> = emptyMap(),
    onVerRiesgo: () -> Unit = {},
    /** ML-03: envíos esperados mañana por zona y couriers a programar. */
    pronostico: PronosticoResponse? = null
) {
    val snackbar = remember { SnackbarHostState() }
    val enLinea = LocalEnLinea.current
    val courier = couriers.firstOrNull { it.courier.id == estado.courierId }
    // Los couriers de la zona filtrada salen primero.
    val opciones = remember(couriers, estado.zona) {
        couriers.sortedWith(compareBy<CourierConCarga>({ it.courier.zona != estado.zona }, { -it.disponibles }))
    }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    AppScaffold(
        titulo = "Asignar envíos",
        subtitulo = "Por zona y capacidad del courier",
        onVolver = onVolver,
        snackbarHostState = snackbar
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = estado.zona == null,
                                onClick = { onZona(null) },
                                label = { Text("Todas") }
                            )
                        }
                        items(CatalogoRutas.zonas) { zona ->
                            FilterChip(
                                selected = estado.zona == zona,
                                onClick = { onZona(zona) },
                                label = { Text(zona) }
                            )
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Solo envíos sin courier",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = estado.soloSinCourier, onCheckedChange = onSoloSinCourier)
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${envios.size} envío(s) · ${estado.seleccion.size} marcado(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (estado.seleccion.isEmpty()) {
                            TextButton(onClick = onSeleccionarTodos, enabled = envios.isNotEmpty()) { Text("Marcar todos") }
                        } else {
                            TextButton(onClick = onLimpiarSeleccion) { Text("Desmarcar") }
                        }
                    }
                }

                pronostico?.let { p ->
                    val zonas = p.zonas.filter { estado.zona == null || it.zona == estado.zona }
                    if (zonas.isNotEmpty()) item { PronosticoManana(p.fecha, zonas) }
                }

                val conRiesgo = envios.count { it.numeroGuia in riesgoAlto }
                if (conRiesgo > 0) {
                    item { AvisoRiesgo(conRiesgo, onVerRiesgo) }
                }

                if (envios.isEmpty()) {
                    item {
                        EstadoVacio(
                            icono = Icons.Default.TaskAlt,
                            titulo = "Nada por asignar",
                            mensaje = if (estado.soloSinCourier) {
                                "Todos los envíos abiertos de esta zona ya tienen courier."
                            } else {
                                "No hay envíos abiertos en esta zona."
                            }
                        )
                    }
                }

                items(envios, key = { it.id }) { envio ->
                    FilaAsignable(
                        envio = envio,
                        marcado = envio.id in estado.seleccion,
                        habilitado = puedeAsignarse(envio),
                        riesgo = riesgoAlto[envio.numeroGuia],
                        onAlternar = { onAlternar(envio) }
                    )
                }
            }

            PanelAsignar(
                estado = estado,
                opciones = opciones,
                courier = courier,
                enLinea = enLinea,
                onCourier = onCourier,
                onAsignar = onAsignar
            )
        }
    }
}

@Composable
private fun FilaAsignable(envio: Envio, marcado: Boolean, habilitado: Boolean, riesgo: RiesgoEntregaDto?, onAlternar: () -> Unit) {
    Surface(
        onClick = onAlternar,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        color = if (marcado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = marcado, onCheckedChange = { onAlternar() }, enabled = habilitado)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = envio.numeroGuia,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = envio.ruta + (CatalogoRutas.porNombre(envio.ruta)?.let { " · ${it.zona}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        envio.pagoEstado == "POR_PAGAR" -> "Por pagar: el remitente aún no paga el envío con QR."
                        !habilitado -> "Tiene cambios sin enviar: sincroniza primero."
                        envio.transportistaAsignado != null -> "Courier: ${envio.transportistaAsignado}"
                        else -> "Sin courier"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (habilitado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (riesgo != null) {
                    Text(
                        text = "Riesgo alto de falla ${ReglasRiesgo.porcentaje(riesgo.probabilidad)}" +
                            when (riesgo.confirmacion.estado) {
                                "CONFIRMADA" -> " · horario confirmado"
                                "PEDIDA" -> " · SMS enviado"
                                else -> " · avisar antes"
                            },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            EstadoBadge(estado = envio.estadoEnvio)
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

/** ML-03 (uso en la ficha: "Cuántos couriers programar"). Avisa si faltan couriers en una zona. */
@Composable
private fun PronosticoManana(fecha: String, zonas: List<ZonaPronosticoDto>) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Pronóstico para el ${ReglasRiesgo.textoFecha(fecha)} (ML-03)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            zonas.forEach { z ->
                val faltan = z.couriersSugeridos > z.couriersRegistrados
                Text(
                    text = "${z.zona}: ~${z.envios} envíos → ${z.couriersSugeridos} courier(s)" +
                        if (faltan) " · solo hay ${z.couriersRegistrados}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (faltan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = if (faltan) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** HU-O01: "Las entregas con riesgo alto de fallar se marcan para avisar antes al destinatario." */
@Composable
private fun AvisoRiesgo(cantidad: Int, onVerRiesgo: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                text = "  $cantidad con riesgo alto de fallar (ML-01): avisa antes al destinatario.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onVerRiesgo) { Text("Ver") }
        }
    }
}

@Composable
private fun PanelAsignar(
    estado: AsignacionUiState,
    opciones: List<CourierConCarga>,
    courier: CourierConCarga?,
    enLinea: Boolean,
    onCourier: (CourierConCarga?) -> Unit,
    onAsignar: () -> Unit
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SelectorDesplegable<CourierConCarga?>(
                etiqueta = "Courier que recibe los envíos",
                opciones = opciones,
                seleccion = courier,
                texto = { it?.courier?.etiqueta ?: "Elige un courier" },
                textoOpcion = { opcion ->
                    opcion?.let { "${it.courier.etiqueta} · ${it.courier.zona} · ${it.activos}/${it.courier.capacidad}" }
                        .orEmpty()
                },
                onSeleccion = onCourier,
                icono = Icons.Default.DirectionsCar,
                modifier = Modifier.fillMaxWidth()
            )

            if (courier != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${courier.courier.zona} · ${courier.activos} de ${courier.courier.capacidad} paradas · " +
                            "${courier.disponibles} libre(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (courier.lleno) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { courier.activos / courier.courier.capacidad.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!enLinea) {
                MensajeInfo("Sin conexión: la asignación por lote necesita internet, porque el servidor valida la capacidad.")
            }

            Button(
                onClick = onAsignar,
                enabled = enLinea && !estado.enviando && estado.seleccion.isNotEmpty() && courier != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (estado.enviando) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AssignmentInd, contentDescription = null)
                    Text("  Asignar ${estado.seleccion.size} envío(s)")
                }
            }
        }
    }
}
