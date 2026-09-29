package com.example.rutalogadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.local.Envio
import com.example.rutalogadmin.data.local.Ruta
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.ui.components.AppScaffold
import com.example.rutalogadmin.ui.components.EstadoVacio
import com.example.rutalogadmin.ui.components.ItemCard
import com.example.rutalogadmin.ui.components.PestanaOperador
import com.example.rutalogadmin.ui.components.SelectorDesplegable
import com.example.rutalogadmin.ui.components.color
import com.example.rutalogadmin.ui.components.formatoTiempo
import com.example.rutalogadmin.ui.components.icono
import com.example.rutalogadmin.ui.navigation.AppNavigation
import com.example.rutalogadmin.ui.theme.RutaLogTheme
import com.example.rutalogadmin.viewmodel.AuthViewModel
import com.example.rutalogadmin.viewmodel.EnvioViewModel

/**
 * RutaLog Perú · App Admin. Crea AppDatabase.getDB(this) e inicializa los ViewModel.
 * En este archivo también está [PantallaPrincipal], la lista de envíos.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es azul oscuro: íconos del sistema en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val db = AppDatabase.getDB(this)

        setContent {
            RutaLogTheme {
                val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(db.usuarioDao()))
                val envioViewModel: EnvioViewModel = viewModel(
                    factory = EnvioViewModel.factory(db.envioDao(), db.rutaDao())
                )
                AppNavigation(authViewModel = authViewModel, envioViewModel = envioViewModel)
            }
        }
    }
}

/**
 * Pantalla principal de la App Admin: lista de envíos leídos de Room (RF06) en un
 * LazyColumn, con búsqueda y filtros por estado y por ruta (RF07).
 */
@Composable
fun PantallaPrincipal(
    nombreUsuario: String,
    envios: List<Envio>,
    totalEnvios: Int,
    rutas: List<Ruta>,
    texto: String,
    filtroEstado: EstadoEnvio?,
    filtroRuta: String?,
    onTexto: (String) -> Unit,
    onEstado: (EstadoEnvio?) -> Unit,
    onRuta: (String?) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onEnvio: (Envio) -> Unit,
    onPestana: (PestanaOperador) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val hayFiltros = texto.isNotEmpty() || filtroEstado != null || filtroRuta != null

    AppScaffold(
        titulo = "Envíos",
        subtitulo = "Operador · $nombreUsuario",
        pestana = PestanaOperador.ENVIOS,
        onPestana = onPestana,
        onCerrarSesion = onCerrarSesion
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = texto,
                    onValueChange = onTexto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    placeholder = { Text("Buscar por guía o transportista") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (texto.isNotEmpty()) {
                            IconButton(onClick = { onTexto("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            item {
                SelectorDesplegable(
                    etiqueta = "Ruta",
                    opciones = listOf<Ruta?>(null) + rutas,
                    seleccion = rutas.firstOrNull { it.nombre == filtroRuta },
                    texto = { it?.nombre ?: "Todas las rutas" },
                    textoOpcion = { ruta ->
                        if (ruta == null) "Todas las rutas" else "${ruta.nombre} · ${formatoTiempo(ruta.tiempoEstimado)}"
                    },
                    onSeleccion = { onRuta(it?.nombre) },
                    icono = Icons.Default.Map,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filtroEstado == null,
                            onClick = { onEstado(null) },
                            label = { Text("Todos") }
                        )
                    }
                    items(EstadoEnvio.entries) { estado ->
                        FilterChip(
                            selected = filtroEstado == estado,
                            onClick = { onEstado(if (filtroEstado == estado) null else estado) },
                            label = { Text(estado.etiqueta) },
                            leadingIcon = {
                                Icon(
                                    imageVector = estado.icono,
                                    contentDescription = null,
                                    tint = estado.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (envios.size == totalEnvios) {
                            "$totalEnvios envíos guardados"
                        } else {
                            "${envios.size} de $totalEnvios envíos"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (hayFiltros) {
                        TextButton(onClick = onLimpiarFiltros) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(" Limpiar filtros")
                        }
                    }
                }
            }

            if (envios.isEmpty()) {
                item {
                    if (totalEnvios == 0) {
                        EstadoVacio(
                            icono = Icons.Default.Inventory,
                            titulo = "No hay envíos",
                            mensaje = "Todavía no hay envíos guardados en el teléfono."
                        )
                    } else {
                        EstadoVacio(
                            icono = Icons.Default.Search,
                            titulo = "Sin resultados",
                            mensaje = "Ningún envío coincide con la búsqueda o los filtros.",
                            accion = "Limpiar filtros",
                            onAccion = onLimpiarFiltros
                        )
                    }
                }
            }

            items(envios, key = { it.id }) { envio ->
                ItemCard(
                    envio = envio,
                    onClick = { onEnvio(envio) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .animateItem()
                )
            }
        }
    }
}
