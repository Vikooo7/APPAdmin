@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.rutalogadmin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rutalogadmin.ui.theme.AzulRuta

enum class PestanaOperador(val etiqueta: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Default.Dashboard),
    ENVIOS("Envíos", Icons.Default.Inventory),
    RUTAS("Rutas", Icons.Default.Map)
}

/**
 * Estructura común de las pantallas: barra superior azul y, si [pestana] no es null,
 * la barra inferior con Inicio / Envíos / Rutas.
 */
@Composable
fun AppScaffold(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    pestana: PestanaOperador? = null,
    onPestana: (PestanaOperador) -> Unit = {},
    onVolver: (() -> Unit)? = null,
    onCerrarSesion: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var confirmarSalida by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titulo,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitulo != null) {
                            Text(
                                text = subtitulo,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onVolver != null) {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    } else {
                        AppEmblema(modifier = Modifier.padding(start = 12.dp, end = 4.dp))
                    }
                },
                actions = {
                    if (onCerrarSesion != null) {
                        IconButton(onClick = { confirmarSalida = true }) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Cerrar sesión")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulRuta,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (pestana != null) {
                NavigationBar {
                    PestanaOperador.entries.forEach { opcion ->
                        NavigationBarItem(
                            selected = opcion == pestana,
                            onClick = { if (opcion != pestana) onPestana(opcion) },
                            icon = { Icon(opcion.icono, contentDescription = opcion.etiqueta) },
                            label = { Text(opcion.etiqueta) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            content(PaddingValues(0.dp))
        }
    }

    if (confirmarSalida && onCerrarSesion != null) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Los envíos y rutas quedan guardados en el teléfono.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarSalida = false
                        onCerrarSesion()
                    }
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}
