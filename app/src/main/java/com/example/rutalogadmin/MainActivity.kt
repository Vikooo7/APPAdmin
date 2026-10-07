package com.example.rutalogadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rutalogadmin.ui.navigation.AppNavigation
import com.example.rutalogadmin.ui.theme.RutaLogTheme
import com.example.rutalogadmin.viewmodel.AuthViewModel
import com.example.rutalogadmin.viewmodel.EnvioViewModel
import com.example.rutalogadmin.viewmodel.SyncViewModel

/**
 * RutaLog Perú · App Admin.
 * Toma las dependencias del contenedor (Room, API y repositorios) e inicializa los ViewModel.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La barra superior siempre es azul oscuro: íconos del sistema en blanco.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))

        val contenedor = (application as RutaLogAdminApplication).contenedor

        setContent {
            RutaLogTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.factory(contenedor.authRepository)
                )
                val envioViewModel: EnvioViewModel = viewModel(
                    factory = EnvioViewModel.factory(
                        envioRepository = contenedor.envioRepository,
                        rutaRepository = contenedor.rutaRepository,
                        programarSincronizacion = contenedor::programarSincronizacion
                    )
                )
                val syncViewModel: SyncViewModel = viewModel(
                    factory = SyncViewModel.factory(
                        repositorio = contenedor.syncRepository,
                        conectividad = contenedor.conectividad,
                        simularError = contenedor.simularErrorServidor
                    )
                )
                AppNavigation(
                    authViewModel = authViewModel,
                    envioViewModel = envioViewModel,
                    syncViewModel = syncViewModel
                )
            }
        }
    }
}
