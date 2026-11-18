package com.example.rutalogadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.CourierConCarga
import com.example.rutalogadmin.model.Envio
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.estaCerrado
import com.example.rutalogadmin.model.estadoEnvio
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.MensajeError
import com.example.rutalogadmin.ui.components.MensajeInfo
import com.example.rutalogadmin.ui.components.SelectorDesplegable
import com.example.rutalogadmin.ui.components.TarjetaSeccion
import com.example.rutalogadmin.ui.components.color
import com.example.rutalogadmin.ui.components.icono

/**
 * Formulario para editar el envío: RF08 (actualizar estado) y RF09 (asignar courier).
 * Funciona sin conexión: el cambio queda en la cola. Al guardar, vuelve al detalle.
 */
@Composable
fun FormScreen(
    envio: Envio?,
    couriers: List<CourierConCarga>,
    onVolver: () -> Unit,
    onVerFallidas: () -> Unit,
    onGuardar: (
        envio: Envio,
        estado: EstadoEnvio,
        courier: CourierConCarga?,
        onError: (String) -> Unit,
        onGuardado: () -> Unit
    ) -> Unit
) {
    AppScaffold(titulo = "Actualizar envío", subtitulo = envio?.numeroGuia, onVolver = onVolver) {
        if (envio == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@AppScaffold
        }

        val estadoActual = envio.estadoEnvio
        var estadoElegido by remember(envio.estado) { mutableStateOf(estadoActual) }
        // null = se mantiene el courier (o transportista) que ya tiene el envío.
        var courierElegido by remember(envio.courierId) { mutableStateOf<CourierConCarga?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var guardando by remember { mutableStateOf(false) }

        val zonaEnvio = CatalogoRutas.porNombre(envio.ruta)?.zona
        // Primero los couriers de la zona del envío; dentro de cada grupo, los que tienen más cupo.
        val opciones = remember(couriers, zonaEnvio) {
            couriers.sortedWith(compareBy<CourierConCarga>({ it.courier.zona != zonaEnvio }, { -it.disponibles }))
        }
        val hayCambios = estadoElegido != estadoActual ||
            (courierElegido != null && courierElegido?.courier?.id != envio.courierId)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "${envio.ruta} · estado actual: ${estadoActual.etiqueta.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (envio.estaCerrado) {
                MensajeInfo(
                    if (estadoActual == EstadoEnvio.DEVUELTO) {
                        "Envío devuelto al remitente tras ${EstadoEnvio.MAX_INTENTOS} intentos fallidos: ya no se puede modificar."
                    } else {
                        "Envío entregado: la operación está cerrada y ya no se puede modificar."
                    }
                )
                return@Column
            }
            if (estadoActual == EstadoEnvio.FALLIDO) {
                MensajeInfo(
                    "La entrega falló (intento ${envio.intentos} de ${EstadoEnvio.MAX_INTENTOS}). " +
                        "Reprográmala antes de cambiar el estado."
                )
                Button(onClick = onVerFallidas, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EventRepeat, contentDescription = null)
                    Text("  Ir a Entregas fallidas")
                }
                return@Column
            }

            // RF08
            TarjetaSeccion(titulo = "Estado del envío", icono = Icons.Default.SwapHoriz) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoEnvio.delOperador.forEach { opcion ->
                        OpcionEstado(
                            estado = opcion,
                            seleccionado = estadoElegido == opcion,
                            esActual = estadoActual == opcion,
                            habilitado = opcion.paso >= estadoActual.paso,
                            onClick = {
                                estadoElegido = opcion
                                error = null
                            }
                        )
                    }
                }
            }

            // RF09
            TarjetaSeccion(titulo = "Courier", icono = Icons.Default.DirectionsCar) {
                SelectorDesplegable<CourierConCarga?>(
                    etiqueta = "Courier asignado",
                    opciones = opciones,
                    seleccion = courierElegido,
                    texto = { elegido ->
                        elegido?.courier?.etiqueta ?: envio.transportistaAsignado ?: "Selecciona un courier"
                    },
                    textoOpcion = { opcion ->
                        opcion?.let { "${it.courier.etiqueta} · ${it.courier.zona} · ${it.activos}/${it.courier.capacidad}" }
                            .orEmpty()
                    },
                    onSeleccion = {
                        courierElegido = it
                        error = null
                    },
                    icono = Icons.Default.DirectionsCar,
                    modifier = Modifier.fillMaxWidth()
                )
                val mostrado = courierElegido ?: couriers.firstOrNull { it.courier.id == envio.courierId }
                Text(
                    text = when {
                        couriers.isEmpty() -> "Sincroniza para descargar la lista de couriers."
                        mostrado != null ->
                            "Zona ${mostrado.courier.zona} · ${mostrado.activos} de ${mostrado.courier.capacidad} paradas" +
                                if (mostrado.lleno) " (sin cupo)" else ""
                        envio.transportistaAsignado != null -> "Transportista del parcial: elige un courier para reasignarlo."
                        else -> "Obligatorio para cualquier estado distinto de pendiente de recojo."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mostrado?.lleno == true) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            AnimatedVisibility(visible = error != null) {
                MensajeError(texto = error ?: "")
            }

            Button(
                onClick = {
                    error = null
                    guardando = true
                    onGuardar(
                        envio,
                        estadoElegido,
                        courierElegido,
                        {
                            error = it
                            guardando = false
                        },
                        {
                            guardando = false
                            onVolver()
                        }
                    )
                },
                enabled = hayCambios && !guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Text("  Guardar cambios")
            }
        }
    }
}

@Composable
private fun OpcionEstado(
    estado: EstadoEnvio,
    seleccionado: Boolean,
    esActual: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit
) {
    val alfa = if (habilitado) 1f else 0.4f
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionado) estado.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) estado.color else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(estado.color.copy(alpha = 0.14f * alfa)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = estado.icono,
                    contentDescription = null,
                    tint = estado.color.copy(alpha = alfa),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (esActual) "${estado.etiqueta} (actual)" else estado.etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alfa)
                )
                Text(
                    text = estado.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alfa)
                )
            }
            Icon(
                imageVector = if (seleccionado) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (seleccionado) estado.color else MaterialTheme.colorScheme.outline.copy(alpha = alfa)
            )
        }
    }
}
