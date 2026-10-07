package com.example.rutalogadmin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.rutalogadmin.data.local.dao.EnvioDao
import com.example.rutalogadmin.data.local.dao.OperacionPendienteDao
import com.example.rutalogadmin.data.local.dao.RutaDao
import com.example.rutalogadmin.data.local.dao.UsuarioDao
import com.example.rutalogadmin.data.local.entities.EnvioEntity
import com.example.rutalogadmin.data.local.entities.OperacionPendienteEntity
import com.example.rutalogadmin.data.local.entities.RutaEntity
import com.example.rutalogadmin.data.local.entities.SyncMetadataEntity
import com.example.rutalogadmin.data.local.entities.UsuarioEntity

/**
 * Base de datos local (SQLite mediante Room).
 * Tablas: usuarios, envios, rutas, operaciones_pendientes y sync_metadata.
 * Los envíos y las rutas ya no se crean de ejemplo: se descargan de la API REST (RF11).
 */
@Database(
    entities = [
        UsuarioEntity::class,
        EnvioEntity::class,
        RutaEntity::class,
        OperacionPendienteEntity::class,
        SyncMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun envioDao(): EnvioDao
    abstract fun rutaDao(): RutaDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun getDB(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rutalog_admin.db"
                )
                    // La versión 1 no tenía los campos de sincronización: se recrea la base.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
