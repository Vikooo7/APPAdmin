# RutaLog Admin (App Operador logístico / Administrador)

**Curso:** Desarrollo de Aplicaciones Móviles · Evaluación Parcial, Semana 8
**Caso:** 14. RutaLog Perú · Victor Manuel Serrano Pure
**Tecnología:** Kotlin + Jetpack Compose + Material 3 · MVVM · Room · Retrofit · WorkManager
**Paquete:** `com.example.rutalogadmin` · se instala junto a la App Cliente (`com.example.rutalogcliente`)

```
Compose → ViewModel → Repository → Room / Retrofit → API REST → BD remota (Neon)
                                  ↓
                         Cola local → SyncWorker
```

La app funciona con conexión y sin ella. Las pantallas siempre leen de Room; la API REST solo actualiza esa copia local. La app nunca se conecta directo a la base remota.

El detalle de arquitectura, tablas, endpoints, permisos y sincronización está en [docs/Documento-Semana8.md](../../../../../../../docs/Documento-Semana8.md).

---

## 1. Historia de usuario

> Como **operador logístico**, quiero **actualizar el estado de los envíos y consultar rutas activas guardadas en Room**, para **gestionar la operación de transporte entre sesiones**. Debo poder **consultar los datos locales y conservar las operaciones autorizadas sin conexión**, para **sincronizarlas con la API REST cuando el servidor esté disponible**.

---

## 2. Requisitos funcionales

| RF | Descripción | Dónde está |
|---|---|---|
| RF06 | Consultar mínimo 30 envíos leídos desde Room (el servidor tiene 40 de ejemplo) | `ListScreen`, `EnvioDao.obtenerTodos()` |
| RF07 | Filtrar por estado y ruta | `ListScreen`, `EnvioDao.filtrar(estado, ruta)` |
| RF08 | Actualizar el estado: recogido, en tránsito, en reparto o entregado | `FormScreen`, `EnvioRepository.guardarOperacion()` |
| RF09 | Asignar un transportista a un envío | `FormScreen`, `model/Transportistas.kt` |
| RF10 | Consultar rutas activas y tiempo estimado de llegada | `RutasScreen`, `RutaDao.obtenerConCarga()` |
| RF11 | Actualizar Room con los datos descargados de la API | `SyncRepository.descargar()` |
| RF12 | Guardar cada escritura y su operación pendiente en una transacción local | `EnvioRepository.guardarOperacion()` (`db.withTransaction`) |
| RF13 | Sincronizar con WorkManager y con botón manual | `worker/SyncWorker.kt`, `SyncScreen` ("Sincronizar ahora") |
| RF14 | Mostrar pendientes, errores y última sincronización | `SyncScreen`, `SyncViewModel` |
| RF15 | Evitar duplicados con UUID y mostrar los conflictos rechazados | `OperacionPendienteEntity.uuidOperacion`, `SyncRepository.aplicarRechazo()` |

**Reglas de RF08 y RF09** (`model/ReglasOperacion.kt`). La API valida las mismas:
- El estado solo avanza; no vuelve a un paso anterior.
- Cualquier estado distinto de *pendiente* exige un transportista.
- El transportista se elige de la lista de 10.
- Un envío *entregado* queda cerrado.

**Ruta activa:** la que lleva al menos un envío recogido, en tránsito o en reparto.

---

## 3. Cuenta de prueba

`operador@rutalog.pe`, clave `1234` (botón "Usar cuenta demo" en el Login).

Las cuentas las crea la empresa en el servidor: la app no tiene registro. La contraseña no se guarda en el teléfono; solo el token de sesión, cifrado.

---

## 4. Pantallas

| Pantalla | Archivo | Qué hace |
|---|---|---|
| Splash | `SplashScreen.kt` | Animación de entrada. Si hay sesión guardada, entra directo |
| Login | `LoginScreen.kt` | Inicio de sesión contra la API (rol administrador) |
| Inicio | `HomeScreen.kt` | Panel: resumen por estado y envíos sin transportista |
| Envíos | `ListScreen.kt` | RF06 y RF07: lista con filtros |
| Detalle | `DetailScreen.kt` | Estado, sincronización, línea de tiempo y datos |
| Formulario | `FormScreen.kt` | RF08 y RF09: cambiar estado y transportista |
| Rutas activas | `RutasScreen.kt` | RF10 |
| Sincronización | `SyncScreen.kt` | RF13, RF14 y RF15 |

La barra inferior tiene cuatro pestañas: **Inicio**, **Envíos**, **Rutas** y **Sincronizar**. La última muestra cuántos cambios hay en la cola.

---

## 5. Estructura de archivos

```
com.example.rutalogadmin/
├── data/
│   ├── local/
│   │   ├── entities/
│   │   │   ├── UsuarioEntity.kt
│   │   │   ├── EnvioEntity.kt
│   │   │   ├── RutaEntity.kt
│   │   │   ├── OperacionPendienteEntity.kt
│   │   │   └── SyncMetadataEntity.kt
│   │   ├── dao/
│   │   │   ├── UsuarioDao.kt
│   │   │   ├── EnvioDao.kt
│   │   │   ├── RutaDao.kt
│   │   │   └── OperacionPendienteDao.kt
│   │   ├── AppDatabase.kt
│   │   └── SesionSegura.kt             // token cifrado
│   ├── remote/
│   │   ├── api/ApiService.kt
│   │   ├── dto/EnvioDto.kt, RutaDto.kt, UsuarioDto.kt
│   │   └── RetrofitClient.kt
│   ├── mapper/EnvioMapper.kt, RutaMapper.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── EnvioRepository.kt
│       ├── RutaRepository.kt
│       └── SyncRepository.kt
├── model/
│   ├── RolUsuario.kt, Usuario.kt
│   ├── EstadoSincronizacion.kt
│   ├── Envio.kt, EstadoEnvio.kt, ReglasOperacion.kt, Transportistas.kt
│   └── Ruta.kt, RutaTarifa.kt, NumeroGuia.kt
├── viewmodel/
│   ├── AuthViewModel.kt
│   ├── EnvioViewModel.kt
│   └── SyncViewModel.kt
├── ui/
│   ├── navigation/AppNavigation.kt
│   ├── screens/
│   │   ├── SplashScreen.kt
│   │   ├── LoginScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── ListScreen.kt
│   │   ├── DetailScreen.kt
│   │   ├── FormScreen.kt
│   │   ├── SyncScreen.kt
│   │   └── RutasScreen.kt              // rutas activas (RF10)
│   ├── components/
│   │   ├── ConnectivityBanner.kt
│   │   ├── SyncStatusIndicator.kt
│   │   └── AppScaffold, ItemCard, InputField, AppLogo, EstadoEnvioUi, Mensajes, Formato
│   └── theme/
├── worker/SyncWorker.kt
├── util/ConnectivityObserver.kt
├── di/AppContainer.kt
├── RutaLogAdminApplication.kt
└── MainActivity.kt
```

La estructura sigue la plantilla de la guía. Lo que hay de más responde al caso, que tiene dos entidades (envíos y rutas): `RutaEntity`, `RutaDao`, `RutaDto`, `RutaMapper`, `RutaRepository` y `RutasScreen`.

---

## 6. Cómo ejecutar y probar

1. Abrir la carpeta `MIAPPadmin` en Android Studio y esperar a que sincronice Gradle.
2. Ejecutar en un teléfono o emulador con Android 10 o superior, con internet.
3. Entrar con la cuenta de prueba. La app descarga los envíos y las rutas.

**Pruebas unitarias** (16):

```
gradlew testDebugUnitTest
```

| Archivo | Qué revisa |
|---|---|
| `ReglasOperacionTest` | Reglas de RF08 y RF09 |
| `LlamarApiTest` | Cómo se clasifican las respuestas de la API (éxito, rechazo, fallo) y la conversión de datos |
| `ResumenSincronizacionTest` | El texto que resume una sincronización |

**APK:** `gradlew assembleDebug` lo deja en `app/build/outputs/apk/debug/app-debug.apk`.

El guion de las pruebas online, offline, error del servidor y conflicto está en el documento de la evaluación.

---

## 7. Proyectos relacionados

| Proyecto | Carpeta | Repositorio |
|---|---|---|
| App Operador / Admin | `V:\MIAPPadmin` | `Vikooo7/APPAdmin` |
| App Cliente | `V:\MIAPP` | `Vikooo7/APPCliente` |
| API REST | `V:\MIAPPapi` | `Vikooo7/APPApi` · `https://app-api-rutalog.vercel.app` |
