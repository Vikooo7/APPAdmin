package com.example.rutalogadmin.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rutalogadmin.PantallaPrincipal
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.ui.components.PestanaOperador
import com.example.rutalogadmin.ui.screens.DetailScreen
import com.example.rutalogadmin.ui.screens.FormScreen
import com.example.rutalogadmin.ui.screens.HomeScreen
import com.example.rutalogadmin.ui.screens.LoginScreen
import com.example.rutalogadmin.ui.screens.RegistroScreen
import com.example.rutalogadmin.ui.screens.RutasScreen
import com.example.rutalogadmin.ui.screens.SplashScreen
import com.example.rutalogadmin.viewmodel.AuthViewModel
import com.example.rutalogadmin.viewmodel.EnvioViewModel
import kotlinx.coroutines.flow.flowOf

object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
    const val LISTA = "lista"
    const val RUTAS = "rutas"
    const val DETALLE = "detalle/{envioId}"
    const val FORMULARIO = "formulario/{envioId}"

    fun detalle(id: Int) = "detalle/$id"
    fun formulario(id: Int) = "formulario/$id"
}

/** Splash → Login / Registro → Inicio del operador → Envíos → Detalle → Formulario · Rutas activas. */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    envioViewModel: EnvioViewModel,
    navController: NavHostController = rememberNavController()
) {
    val auth by authViewModel.estado.collectAsState()
    val nombreUsuario = auth.usuario?.nombre.orEmpty()

    val irAPestana: (PestanaOperador) -> Unit = { pestana ->
        val destino = when (pestana) {
            PestanaOperador.INICIO -> Rutas.HOME
            PestanaOperador.ENVIOS -> Rutas.LISTA
            PestanaOperador.RUTAS -> Rutas.RUTAS
        }
        navController.navigate(destino) {
            popUpTo(Rutas.HOME)
            launchSingleTop = true
        }
    }

    /** Abre Envíos con un filtro ya aplicado (desde el Inicio o desde una ruta). */
    val verEnvios: (EstadoEnvio?, String?) -> Unit = { estado, ruta ->
        envioViewModel.mostrarSolo(estado = estado, ruta = ruta)
        irAPestana(PestanaOperador.ENVIOS)
    }

    val cerrarSesion: () -> Unit = {
        authViewModel.cerrarSesion()
        envioViewModel.limpiarFiltros()
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.SPLASH,
        enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 10 } },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(150)) }
    ) {
        composable(Rutas.SPLASH) {
            SplashScreen(
                onFinish = {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.LOGIN) {
            LoginScreen(
                estado = auth,
                onLogin = { correo, clave ->
                    authViewModel.login(correo, clave) {
                        navController.navigate(Rutas.HOME) {
                            popUpTo(Rutas.LOGIN) { inclusive = true }
                        }
                    }
                },
                onIrARegistro = {
                    authViewModel.limpiarMensajes()
                    navController.navigate(Rutas.REGISTRO)
                },
                onLimpiarMensajes = authViewModel::limpiarMensajes
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(
                estado = auth,
                onRegistrar = { nombre, correo, clave, confirmacion ->
                    authViewModel.registrar(nombre, correo, clave, confirmacion) {
                        navController.popBackStack()
                    }
                },
                onVolver = {
                    authViewModel.limpiarMensajes()
                    navController.popBackStack()
                },
                onLimpiarMensajes = authViewModel::limpiarMensajes
            )
        }

        composable(Rutas.HOME) {
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
            val envios by envioViewModel.envios.collectAsState()
            val rutas by envioViewModel.rutasConCarga.collectAsState()
            HomeScreen(
                nombreUsuario = nombreUsuario,
                envios = envios,
                rutasActivas = rutas.count { it.esActiva },
                onVerEstado = { estado -> verEnvios(estado, null) },
                onVerRutas = { irAPestana(PestanaOperador.RUTAS) },
                onEnvio = { navController.navigate(Rutas.detalle(it.id)) },
                onPestana = irAPestana,
                onCerrarSesion = cerrarSesion
            )
        }

        composable(Rutas.LISTA) {
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
            val todos by envioViewModel.envios.collectAsState()
            val filtrados by envioViewModel.enviosFiltrados.collectAsState()
            val texto by envioViewModel.texto.collectAsState()
            val filtroEstado by envioViewModel.filtroEstado.collectAsState()
            val filtroRuta by envioViewModel.filtroRuta.collectAsState()
            val rutas by envioViewModel.rutas.collectAsState()
            PantallaPrincipal(
                nombreUsuario = nombreUsuario,
                envios = filtrados,
                totalEnvios = todos.size,
                rutas = rutas,
                texto = texto,
                filtroEstado = filtroEstado,
                filtroRuta = filtroRuta,
                onTexto = envioViewModel::buscarTexto,
                onEstado = envioViewModel::filtrarPorEstado,
                onRuta = envioViewModel::filtrarPorRuta,
                onLimpiarFiltros = envioViewModel::limpiarFiltros,
                onEnvio = { navController.navigate(Rutas.detalle(it.id)) },
                onPestana = irAPestana,
                onCerrarSesion = cerrarSesion
            )
        }

        composable(Rutas.RUTAS) {
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
            val todas by envioViewModel.rutasConCarga.collectAsState()
            val visibles by envioViewModel.rutasVisibles.collectAsState()
            val soloActivas by envioViewModel.soloActivas.collectAsState()
            RutasScreen(
                nombreUsuario = nombreUsuario,
                rutas = visibles,
                totalRutas = todas.size,
                rutasActivas = todas.count { it.esActiva },
                enviosEnCurso = todas.sumOf { it.enCurso },
                soloActivas = soloActivas,
                onSoloActivas = envioViewModel::mostrarSoloActivas,
                onRuta = { ruta -> verEnvios(null, ruta.nombre) },
                onPestana = irAPestana,
                onCerrarSesion = cerrarSesion
            )
        }

        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument("envioId") { type = NavType.IntType })
        ) { entrada ->
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
            val id = entrada.arguments?.getInt("envioId") ?: 0
            val flujoEnvio = remember(id) { envioViewModel.obtenerPorId(id) }
            val envio by flujoEnvio.collectAsState(initial = null)
            val nombreRuta = envio?.ruta
            val flujoRuta = remember(nombreRuta) {
                if (nombreRuta == null) flowOf(null) else envioViewModel.obtenerRutaPorNombre(nombreRuta)
            }
            val ruta by flujoRuta.collectAsState(initial = null)
            DetailScreen(
                envio = envio,
                ruta = ruta,
                onVolver = { navController.popBackStack() },
                onActualizar = { navController.navigate(Rutas.formulario(it.id)) }
            )
        }

        composable(
            route = Rutas.FORMULARIO,
            arguments = listOf(navArgument("envioId") { type = NavType.IntType })
        ) { entrada ->
            SesionRequerida(hayUsuario = auth.usuario != null, onSinSesion = cerrarSesion)
            val id = entrada.arguments?.getInt("envioId") ?: 0
            val flujo = remember(id) { envioViewModel.obtenerPorId(id) }
            val envio by flujo.collectAsState(initial = null)
            FormScreen(
                envio = envio,
                onVolver = { navController.popBackStack() },
                onGuardar = envioViewModel::guardarOperacion
            )
        }
    }
}

/** Si Android cerró la app y se perdió la sesión, vuelve al Login en vez de mostrar datos sin usuario. */
@Composable
private fun SesionRequerida(hayUsuario: Boolean, onSinSesion: () -> Unit) {
    LaunchedEffect(hayUsuario) {
        if (!hayUsuario) onSinSesion()
    }
}
