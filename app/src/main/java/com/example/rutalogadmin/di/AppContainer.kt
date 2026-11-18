package com.example.rutalogadmin.di

import android.content.Context
import com.example.rutalogadmin.data.local.AppDatabase
import com.example.rutalogadmin.data.local.SesionSegura
import com.example.rutalogadmin.data.remote.RetrofitClient
import com.example.rutalogadmin.data.remote.api.ApiService
import com.example.rutalogadmin.data.repository.AuthRepository
import com.example.rutalogadmin.data.repository.EnvioRepository
import com.example.rutalogadmin.data.repository.OperacionRepository
import com.example.rutalogadmin.data.repository.PagosRepository
import com.example.rutalogadmin.data.repository.RiesgoRepository
import com.example.rutalogadmin.data.repository.RutaRepository
import com.example.rutalogadmin.data.repository.SyncRepository
import com.example.rutalogadmin.util.ConnectivityObserver
import com.example.rutalogadmin.worker.SyncWorker
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Crea una sola vez las piezas que comparten los ViewModel y el SyncWorker:
 * la base de datos Room, el cliente de la API y los repositorios.
 */
class AppContainer(context: Context) {

    private val contextoApp = context.applicationContext

    val db: AppDatabase = AppDatabase.getDB(contextoApp)
    val sesion = SesionSegura(contextoApp)
    val conectividad = ConnectivityObserver(contextoApp)

    /** Prueba de la exposición: mientras esté en true, la API responde con error 503. */
    val simularErrorServidor = AtomicBoolean(false)

    val api: ApiService = RetrofitClient.crear(
        token = { sesion.token },
        simularError = { simularErrorServidor.get() }
    )

    val authRepository = AuthRepository(api, db, sesion)
    val envioRepository = EnvioRepository(db)
    val rutaRepository = RutaRepository(db)
    val syncRepository = SyncRepository(db, api)
    val operacionRepository = OperacionRepository(db, api)
    val pagosRepository = PagosRepository(api)
    val riesgoRepository = RiesgoRepository(api)

    /** RF13: deja programado el envío de la cola para cuando haya conexión, aunque la app se cierre. */
    fun programarSincronizacion() {
        SyncWorker.programar(contextoApp)
    }
}
