package com.example.rutalogadmin.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.rutalogadmin.RutaLogAdminApplication
import java.util.concurrent.TimeUnit

/**
 * RF13: sincroniza en segundo plano con WorkManager. Solo se ejecuta cuando hay red, así que
 * un cambio guardado sin conexión se envía solo al recuperarla, aunque la app esté cerrada.
 */
class SyncWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as RutaLogAdminApplication).contenedor
        if (contenedor.sesion.token == null) return Result.success()

        val resultado = contenedor.syncRepository.sincronizar()
        return if (resultado.error != null && runAttemptCount < MAX_INTENTOS) Result.retry() else Result.success()
    }

    companion object {
        private const val NOMBRE = "sincronizacion_rutalog_admin"
        private const val MAX_INTENTOS = 5

        /** Programa una sincronización para cuando haya conexión. */
        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.REPLACE, trabajo)
        }
    }
}
