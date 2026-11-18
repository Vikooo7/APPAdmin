package com.example.rutalogadmin.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.CourierConCarga
import com.example.rutalogadmin.model.EntregaFallida
import com.example.rutalogadmin.model.IntentoFallido
import com.example.rutalogadmin.model.MotivoFalla
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.EstadoBadge
import com.example.rutalogadmin.ui.components.EstadoVacio
import com.example.rutalogadmin.ui.components.LocalEnLinea
import com.example.rutalogadmin.ui.components.MensajeError
import com.example.rutalogadmin.ui.components.SelectorDesplegable
import com.example.rutalogadmin.ui.components.formatoFechaHora
import com.example.rutalogadmin.ui.components.formatoSoles
import com.example.rutalogadmin.viewmodel.FallidasUiState

/**
 * Entregas fallidas (RF-C07 visto por el operador): cada intento con su motivo, fecha y courier.
 * El operador reprograma la entrega (y puede cambiar el courier). Al tercer intento el envío
 * vuelve al remitente (RN-05). Se consulta en línea.
 */
@Composable
fun FallidasScreen(
    estado: FallidasUiState,
    couriers: List<CourierConCarga>,
    onCargar: () -> Unit,
    onReprogramar: (EntregaFallida, CourierConCarga?) -> Unit,
    onMensajeMostrado: () -> Unit,
    onVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val enLinea = LocalEnLinea.current
    var aReprogramar by remember { mutableStateOf<EntregaFallida?>(null) }

    LaunchedEffect(Unit) { onCargar() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            onMensajeMostrado()
        }
    }

    val porReprogramar = estado.lista.filter { it.porReprogramar }
    val historial = estado.lista.filterNot { it.porReprogramar }

    AppScaffold(
        titulo = "Entregas fallidas",
        subtitulo = "${porReprogramar.size} por reprogramar",
        onVolver = onVolver,
        snackbarHostState = snackbar
    ) {
        when {
            estado.cargando && !estado.cargada -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            estado.error != null && estado.lista.isEmpty() -> EstadoVacio(
                icono = Icons.Default.CloudOff,
                titulo = "No se pudo cargar",
                mensaje = estado.error,
                accion = "Reintentar",
                onAccion = onCargar
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Al tercer intento fallido el envío vuelve al remitente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onCargar, enabled = !estado.cargando) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(" Actualizar")
                        }
                    }
                }
                estado.error?.let { item { MensajeError(texto = it) } }

                if (porReprogramar.isEmpty()) {
                    item {
                        EstadoVacio(
                            icono = Icons.Default.TaskAlt,
                            titulo = "Nada por reprogramar",
                            mensaje = "No hay entregas fallidas pendientes."
                        )
                    }
                }
                items(porReprogramar, key = { it.idRemoto }) { fallida ->
                    TarjetaFallida(
                        fallida = fallida,
                        procesando = estado.procesando == fallida.idRemoto,
                        enLinea = enLinea,
                        onReprogramar = { aReprogramar = fallida }
                    )
                }

                if (historial.isNotEmpty()) {
                    item {
                        Text(
                            text = "Historial (reprogramadas y devueltas)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(historial, key = { it.idRemoto }) { fallida ->
                        TarjetaFallida(fallida = fallida, procesando = false, enLinea = enLinea, onReprogramar = null)
                    }
                }
            }
        }
    }

    aReprogramar?.let { fallida ->
        DialogoReprogramar(
            fallida = fallida,
            couriers = couriers,
            onConfirmar = { courier ->
                aReprogramar = null
                onReprogramar(fallida, courier)
            },
            onCancelar = { aReprogramar = null }
        )
    }
}

@Composable
private fun TarjetaFallida(
    fallida: EntregaFallida,
    procesando: Boolean,
    enLinea: Boolean,
    onReprogramar: (() -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fallida.numeroGuia,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = fallida.ruta, style = MaterialTheme.typography.bodySmall)
                }
                EstadoBadge(estado = fallida.estado)
            }
            Text(
                text = "Intento ${fallida.intentos.size} de ${fallida.maxIntentos}" +
                    (fallida.destinatario?.let { " · Destinatario: $it" } ?: "") +
                    (if (fallida.montoContraentrega > 0) " · Contraentrega ${formatoSoles(fallida.montoContraentrega)}" else ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            fallida.intentos.forEach { FilaIntento(it) }

            if (onReprogramar != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onReprogramar,
                    enabled = enLinea && !procesando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (procesando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.EventRepeat, contentDescription = null)
                        Text("  Reprogramar entrega")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaIntento(intento: IntentoFallido) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "${intento.numero}.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = MotivoFalla.etiqueta(intento.motivo), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = listOfNotNull(intento.fecha?.let(::formatoFechaHora), intento.courierNombre).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (intento.tieneFoto) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Con foto de la fachada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Confirma la reprogramación y permite pasar la entrega a otro courier. */
@Composable
private fun DialogoReprogramar(
    fallida: EntregaFallida,
    couriers: List<CourierConCarga>,
    onConfirmar: (CourierConCarga?) -> Unit,
    onCancelar: () -> Unit
) {
    // null = mismo courier.
    var courier by remember { mutableStateOf<CourierConCarga?>(null) }
    val zona = CatalogoRutas.porNombre(fallida.ruta)?.zona
    val opciones = remember(couriers, zona, fallida.courierId) {
        listOf<CourierConCarga?>(null) + couriers
            .filter { it.courier.id != fallida.courierId }
            .sortedWith(compareBy<CourierConCarga>({ it.courier.zona != zona }, { -it.disponibles }))
    }
    val sinCupo = courier?.lleno == true

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Default.EventRepeat, contentDescription = null) },
        title = { Text("Reprogramar ${fallida.numeroGuia}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("La entrega vuelve a salir a reparto. Puedes mantener el courier o pasarla a otro.")
                SelectorDesplegable(
                    etiqueta = "Courier",
                    opciones = opciones,
                    seleccion = courier,
                    texto = { elegido -> elegido?.courier?.etiqueta ?: "Mismo courier (${fallida.transportista ?: "sin courier"})" },
                    textoOpcion = { opcion ->
                        opcion?.let { "${it.courier.etiqueta} · ${it.courier.zona} · ${it.activos}/${it.courier.capacidad}" }
                            ?: "Mismo courier"
                    },
                    onSeleccion = { courier = it },
                    icono = Icons.Default.DirectionsCar,
                    modifier = Modifier.fillMaxWidth()
                )
                if (sinCupo) {
                    Text(
                        text = "${courier?.courier?.nombre} no tiene cupo.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(courier) }, enabled = !sinCupo) { Text("Reprogramar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
