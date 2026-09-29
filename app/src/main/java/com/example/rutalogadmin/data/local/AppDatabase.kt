package com.example.rutalogadmin.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.layout.size
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.rutalogadmin.model.CatalogoRutas
import com.example.rutalogadmin.model.EstadoEnvio
import com.example.rutalogadmin.model.NumeroGuia
import com.example.rutalogadmin.model.RolUsuario
import com.example.rutalogadmin.model.Transportistas

@Database(entities = [Usuario::class, Envio::class, Ruta::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun envioDao(): EnvioDao
    abstract fun rutaDao(): RutaDao

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
                    .addCallback(DatosIniciales)
                    .build()
                    .also { instancia = it }
            }
    }
}

/** RF06: cantidad de envíos de ejemplo (el mínimo pedido es 30). */
private const val ENVIOS_INICIALES = 40

/**
 * Rutas con más movimiento: los envíos de ejemplo se reparten entre ellas.
 * Son 15 (no 16) para que los envíos de una misma ruta no caigan siempre en el
 * mismo punto del patrón de 8 estados.
 */
private val rutasFrecuentes = listOf(
    "Lima → Arequipa", "Lima → Trujillo", "Lima → Cusco", "Lima → Iquitos",
    "Lima → Piura", "Lima → Huancayo", "Lima → Tacna", "Lima → Chiclayo",
    "Lima → Puno", "Lima → Ica", "Lima → Tarapoto", "Lima → Pucallpa",
    "Lima → Cajamarca", "Lima → Ayacucho", "Arequipa → Puno"
)

/** Se repite cada 8 envíos: 10 pendientes, 5 recogidos, 10 en tránsito, 5 en reparto y 10 entregados. */
private val patronEstados = listOf(
    EstadoEnvio.EN_TRANSITO, EstadoEnvio.PENDIENTE, EstadoEnvio.RECOGIDO, EstadoEnvio.ENTREGADO,
    EstadoEnvio.EN_REPARTO, EstadoEnvio.EN_TRANSITO, EstadoEnvio.ENTREGADO, EstadoEnvio.PENDIENTE
)

/**
 * Datos de ejemplo que se insertan solo la primera vez que se crea la base de datos:
 * la cuenta del operador, las 27 rutas del catálogo y 40 envíos en distintos estados.
 */
private object DatosIniciales : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        db.insert(
            "usuarios",
            SQLiteDatabase.CONFLICT_NONE,
            ContentValues().apply {
                put("nombre", "Operador RutaLog")
                put("correo", "operador@rutalog.pe")
                put("clave", "1234")
                put("rol", RolUsuario.ADMINISTRADOR.valor)
            }
        )

        CatalogoRutas.rutas.forEach { ruta ->
            db.insert(
                "rutas",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("nombre", ruta.nombre)
                    put("tiempoEstimado", ruta.horasEstimadas)
                }
            )
        }

        for (id in 1..ENVIOS_INICIALES) {
            val ruta = rutasFrecuentes[(id * 7) % rutasFrecuentes.size]
            val estado = patronEstados[(id - 1) % patronEstados.size]
            val peso = 5.0 + (id * 37) % 480 + (id % 4) * 0.25
            val tarifa = CatalogoRutas.porNombre(ruta)?.tarifaPorKg ?: 0.0
            // Los pendientes aún no tienen transportista; algunos recogidos tampoco.
            val sinTransportista = estado == EstadoEnvio.PENDIENTE ||
                (estado == EstadoEnvio.RECOGIDO && id % 2 == 1)
            db.insert(
                "envios",
                SQLiteDatabase.CONFLICT_NONE,
                ContentValues().apply {
                    put("id", id)
                    put("numeroGuia", NumeroGuia.generar(id))
                    put("ruta", ruta)
                    put("pesoKg", peso)
                    put("costoEnvio", CatalogoRutas.calcularCosto(peso, tarifa))
                    put("estado", estado.codigo)
                    if (sinTransportista) {
                        putNull("transportistaAsignado")
                    } else {
                        put("transportistaAsignado", Transportistas.lista[(id * 3) % Transportistas.lista.size])
                    }
                }
            )
        }
    }
}
