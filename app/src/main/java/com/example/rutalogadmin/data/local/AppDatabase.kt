package com.example.rutalogadmin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rutalogadmin.data.local.dao.CourierDao
import com.example.rutalogadmin.data.local.dao.EnvioDao
import com.example.rutalogadmin.data.local.dao.OperacionPendienteDao
import com.example.rutalogadmin.data.local.dao.RutaDao
import com.example.rutalogadmin.data.local.dao.UsuarioDao
import com.example.rutalogadmin.data.local.entities.CourierEntity
import com.example.rutalogadmin.data.local.entities.EnvioEntity
import com.example.rutalogadmin.data.local.entities.OperacionPendienteEntity
import com.example.rutalogadmin.data.local.entities.RutaEntity
import com.example.rutalogadmin.data.local.entities.SyncMetadataEntity
import com.example.rutalogadmin.data.local.entities.UsuarioEntity

/**
 * Base de datos local (SQLite mediante Room).
 * Tablas: usuarios, envios, rutas, couriers, operaciones_pendientes y sync_metadata.
 * Los envíos, las rutas y los couriers no se crean de ejemplo: se descargan de la API REST (RF11).
 */
@Database(
    entities = [
        UsuarioEntity::class,
        EnvioEntity::class,
        RutaEntity::class,
        CourierEntity::class,
        OperacionPendienteEntity::class,
        SyncMetadataEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun envioDao(): EnvioDao
    abstract fun rutaDao(): RutaDao
    abstract fun courierDao(): CourierDao
    abstract fun operacionPendienteDao(): OperacionPendienteDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        /**
         * Proyecto final: courier, destinatario, contraentrega e intentos en envios; zona en rutas;
         * tabla couriers. Solo agrega columnas y una tabla: se conservan los envíos y la cola
         * de operaciones pendientes que el operador aún no envió.
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `courierId` INTEGER")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `destinatarioNombre` TEXT")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `destinatarioTelefono` TEXT")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `destinatarioDireccion` TEXT")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `montoContraentrega` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `intentos` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_envios_courierId` ON `envios` (`courierId`)")
                db.execSQL("ALTER TABLE `rutas` ADD COLUMN `zona` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `couriers` (`id` INTEGER NOT NULL, `nombre` TEXT NOT NULL, " +
                        "`placa` TEXT NOT NULL, `zona` TEXT NOT NULL, `capacidad` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        /** Fase de pagos: estado del pago del envío con QR. Los envíos anteriores no lo requieren. */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `envios` ADD COLUMN `pagoEstado` TEXT NOT NULL DEFAULT 'NO_REQUERIDO'")
            }
        }

        fun getDB(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rutalog_admin.db"
                )
                    .addMigrations(MIGRACION_2_3, MIGRACION_3_4)
                    // La versión 1 no tenía los campos de sincronización: se recrea la base.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instancia = it }
            }
    }
}
