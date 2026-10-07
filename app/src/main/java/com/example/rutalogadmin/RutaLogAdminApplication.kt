package com.example.rutalogadmin

import android.app.Application
import com.example.rutalogadmin.di.AppContainer

/** Clase Application: crea el contenedor de dependencias al iniciar la app. */
class RutaLogAdminApplication : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
    }
}
