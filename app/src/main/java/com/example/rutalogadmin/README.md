# RutaLog Admin (App Operador logístico / Administrador)

**Sector:** logística y transporte de carga
**Alcance:** nacional (las 25 regiones del Perú)
**Tecnología:** Kotlin + Jetpack Compose + Material 3 · MVVM · Room · un solo módulo (`:app`) y una sola Activity (`MainActivity.kt`)
**Paquete:** `com.example.rutalogadmin` · se puede instalar junto a la App Cliente (`com.example.rutalogcliente`)

---

## 1. Problema que resuelve

> Los operadores de RutaLog gestionan envíos hacia todo el Perú sin una vista central. RutaLog Admin reúne en el teléfono los envíos, su estado, el transportista asignado y las rutas que están en operación.

**Qué le pasa hoy al operador logístico:**
- No tiene una vista central de todos los envíos por estado o por ruta.
- Actualiza los estados y asigna transportistas a mano (llamadas, WhatsApp u hojas de cálculo), y así se pierde información entre turnos.
- No sabe con facilidad qué rutas llevan carga en este momento ni cuánto tarda cada una en llegar.

### Cómo lo resuelve la app

| Problema | Función en la app | RF |
|---|---|---|
| No hay una vista central de los envíos | Lista de envíos guardados en Room | RF06 |
| Cuesta encontrar los envíos de un estado o de una ruta | Filtros por estado y por ruta, y búsqueda por guía o transportista | RF07 |
| Los estados se actualizan a mano | Formulario del envío: el estado avanza recogido → en tránsito → en reparto → entregado | RF08 |
| No se sabe quién lleva cada carga | Asignar un transportista al envío | RF09 |
| No se sabe qué rutas están en operación | Rutas activas con su tiempo estimado de llegada | RF10 |
| Los datos se pierden al cerrar la app | Todo se guarda en Room y sigue ahí en la siguiente sesión | Historia de usuario |

---

## 2. Historia de usuario

> Como **operador logístico**, quiero **actualizar el estado de los envíos y consultar las rutas activas guardadas en Room**, para **gestionar la operación de transporte entre sesiones**.

Todo lo que cambia el operador (estado y transportista) se guarda en Room. Si cierra la app y vuelve a entrar, los datos siguen ahí.

---

## 3. Requisitos funcionales

| RF | Descripción | Dónde está |
|---|---|---|
| RF06 | Consultar al menos 30 envíos leídos de Room (la base trae 40 de ejemplo) | `PantallaPrincipal` (en `MainActivity.kt`), `EnvioDao.obtenerTodos()` |
| RF07 | Filtrar por estado y por ruta. También se puede buscar por número de guía o por transportista | `PantallaPrincipal` (chips de estado y selector de ruta), `EnvioDao.filtrar(estado, ruta)` |
| RF08 | Actualizar el estado: recogido, en tránsito, en reparto o entregado | `FormScreen`, `EnvioViewModel.guardarOperacion()`, `EnvioDao.actualizar()` |
| RF09 | Asignar un transportista a un envío | `FormScreen`, catálogo en `model/Transportistas.kt` |
| RF10 | Consultar las rutas activas y su tiempo estimado de llegada | `RutasScreen`, `RutaDao.obtenerConCarga()` |

**Reglas de RF08 y RF09** (`model/ReglasOperacion.kt`, probadas en `ReglasOperacionTest`):
- El estado solo avanza. Un envío no vuelve a un paso anterior ni a *Pendiente*.
- Para pasar a *En tránsito*, *En reparto* o *Entregado*, el envío debe tener un transportista.
- El transportista se elige de la lista de 10 transportistas.
- Un envío *Entregado* queda cerrado: ya no se cambia su estado ni su transportista.
- Si no hay cambios, no se guarda nada.

**Ruta activa:** una ruta que lleva al menos un envío *recogido*, *en tránsito* o *en reparto*. El tiempo estimado de llegada es el campo `tiempoEstimado` de la tabla `rutas`.

---

## 4. Base de datos Room (`rutalog_admin.db`)

| Tabla | Campos | Uso |
|---|---|---|
| `usuarios` | id, nombre, correo, clave, rol | Solo pueden entrar las cuentas con rol `"administrador"` |
| `envios` | id, numeroGuia, ruta, pesoKg, costoEnvio, estado, transportistaAsignado | Tiene los mismos campos que en la App Cliente. El operador cambia `estado` y `transportistaAsignado` |
| `rutas` | id, nombre, tiempoEstimado | `tiempoEstimado` está en horas. `nombre` coincide con `envios.ruta` |

**Datos iniciales** (se cargan solo la primera vez, en `AppDatabase.kt`):
- 1 operador, 27 rutas (Lima hacia las 25 regiones, más 2 interregionales) y 40 envíos.
- Los envíos se reparten así: 10 pendientes, 5 recogidos, 10 en tránsito, 5 en reparto y 10 entregados.
- Los envíos están en 15 rutas, todas con carga en curso. Las otras 12 rutas aparecen como "Sin carga".

**Cuenta demo:** `operador@rutalog.pe`, clave `1234` (también está el botón "Usar cuenta demo" en el Login).

> La clave se guarda en texto plano solo por ser una práctica académica. En un proyecto real se guardaría un hash.

---

## 5. Usuario

| Rol | Descripción | Pantalla al iniciar sesión |
|---|---|---|
| **Operador logístico / administrador** | Personal de RutaLog. Actualiza estados, asigna transportistas y supervisa las rutas activas. | Inicio (panel de operaciones) |

Los clientes remitentes usan la otra app, **RutaLog Cliente**. Si una cuenta que no es de administrador intenta entrar aquí, el Login la rechaza.

---

## 6. Flujo principal

```
Ícono → Splash → Login / Registro (operador)
   └─ Inicio (panel de operaciones)
        ├─ Envíos (PantallaPrincipal) → filtros por estado y ruta → Detalle → Formulario (estado + transportista)
        └─ Rutas activas → tiempo estimado de llegada → envíos de esa ruta
```

La barra inferior tiene tres pestañas: **Inicio**, **Envíos** y **Rutas**. En el Inicio, al tocar un estado se abre Envíos con ese filtro puesto. En Rutas, al tocar una ruta se abren sus envíos.

---

## 7. Pantallas

| Pantalla | Archivo | Qué hace |
|---|---|---|
| Splash | `SplashScreen.kt` | Animación de entrada |
| Login | `LoginScreen.kt` | Inicio de sesión con rol administrador |
| Registro | `RegistroScreen.kt` | Crea una cuenta de operador (rol fijo `administrador`) |
| Inicio | `HomeScreen.kt` | Panel de operaciones: resumen por estado, accesos rápidos y envíos sin transportista |
| Envíos | `PantallaPrincipal()` en `MainActivity.kt` | RF06 y RF07: lista de envíos (LazyColumn) con filtros |
| Detalle | `DetailScreen.kt` | Estado, línea de tiempo y datos del envío |
| Formulario | `FormScreen.kt` | RF08 y RF09: editar el estado y el transportista |
| Rutas activas | `RutasScreen.kt` | RF10 |

---

## 8. Estructura de archivos MVVM con Room

La app usa una sola Activity (`MainActivity.kt`). El código sigue la estructura MVVM del curso: en esta app la entidad de negocio es `Envio` y la entidad de operación es `Ruta`. Como en el ejemplo del curso (`app12_jc_bdlocal_room`), `MainActivity.kt` también contiene `PantallaPrincipal()`, la lista de envíos.

```
com.example.rutalogadmin/
├── data/
│   └── local/
│       ├── Usuario.kt              // @Entity(tableName = "usuarios")
│       ├── UsuarioDao.kt           // @Dao: registrar(), login()
│       ├── Envio.kt                // @Entity de la tabla principal: "envios"
│       ├── EnvioDao.kt             // @Dao: insertar, actualizar, eliminar, obtenerTodos (Flow), filtrar
│       ├── Ruta.kt                 // @Entity de la operación: "rutas" (+ RutaConCarga)
│       ├── RutaDao.kt              // @Dao: obtenerTodas, obtenerConCarga (rutas activas)
│       └── AppDatabase.kt          // @Database(entities = [...]) + getDB() + datos iniciales
├── model/
│   ├── RolUsuario.kt               // enum: CLIENTE, ADMINISTRADOR, EMPLEADO
│   ├── EstadoEnvio.kt              // pendiente, recogido, en tránsito, en reparto, entregado
│   ├── ReglasOperacion.kt          // validación de RF08 y RF09
│   ├── Transportistas.kt           // transportistas que se pueden asignar
│   ├── RutaTarifa.kt               // catálogo de rutas (región, zona, tarifa)
│   └── NumeroGuia.kt               // formato del número de guía
├── ui/
│   ├── navigation/
│   │   └── AppNavigation.kt        // Splash → Login/Registro → pantallas del operador
│   ├── screens/
│   │   ├── SplashScreen.kt
│   │   ├── LoginScreen.kt
│   │   ├── RegistroScreen.kt
│   │   ├── HomeScreen.kt           // inicio: panel de operaciones
│   │   ├── DetailScreen.kt
│   │   ├── FormScreen.kt           // editar estado y transportista
│   │   └── RutasScreen.kt          // rutas activas (RF10)
│   ├── components/
│   │   ├── AppLogo.kt
│   │   ├── InputField.kt           // + SelectorDesplegable
│   │   ├── ItemCard.kt             // + TarjetaSeccion
│   │   ├── AppScaffold.kt          // barra superior y pestañas
│   │   ├── EstadoEnvioUi.kt        // colores, íconos, EstadoBadge y LineaDeTiempo
│   │   ├── Mensajes.kt             // MensajeError, MensajeInfo, EstadoVacio, BannerDegradado
│   │   └── Formato.kt              // soles, kg y horas
│   └── theme/                      // Color.kt, Theme.kt, Type.kt
├── viewmodel/
│   ├── AuthViewModel.kt            // registrar(), login(), estado de sesión
│   └── EnvioViewModel.kt           // lista (Flow), filtros, actualizar, validación y rutas activas
└── MainActivity.kt                 // crea AppDatabase.getDB(this) e inicializa los ViewModel
                                    // + @Composable PantallaPrincipal(): lista de envíos (LazyColumn) con filtros
```

---

## 9. Logo, ícono y colores

- **Ícono de la app:** el emblema del logo (mapa del Perú, caja, flecha y pin) sobre fondo blanco. Es un ícono adaptativo y tiene versión monocromática.
  Archivos: `res/mipmap-*/ic_launcher_foreground.png` y `ic_launcher_monochrome.png`
- **Logo completo:** `res/drawable-nodpi/logo_rutalog.png`, que se usa en el Splash y el Login.

| Color | Hex | Uso |
|---|---|---|
| Azul marino | `#0B2F5B` | Color primario: barra superior y cabeceras |
| Azul claro | `#1B4F8A` | Degradados de las cabeceras |
| Rojo | `#E3192A` | Acentos y envíos sin transportista |
| Verde | `#2E7D32` | Estado "Entregado" y rutas activas |

---

## 10. Cómo ejecutar y probar

1. Abrir la carpeta `MIAPPadmin` en Android Studio y esperar a que termine de sincronizar Gradle.
2. Crear un emulador en *Device Manager* (API 29 o superior) o conectar un teléfono.
3. Ejecutar la app y entrar con `operador@rutalog.pe` / `1234`.

**Pruebas unitarias:** `ReglasOperacionTest` revisa las reglas de RF08 y RF09 (7 casos).

```
gradlew testDebugUnitTest
```

**APK:** para generar el instalador, en Android Studio usa *Build → Generate App Bundles or APKs → Generate APKs*, o ejecuta:

```
gradlew assembleDebug
```

El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`. Se instala en Android 10 o superior; el teléfono pide permitir apps de origen desconocido.

---

## 11. Avance

**Hecho:**
- RF06 a RF10 con datos guardados en Room: 40 envíos, 27 rutas y 10 transportistas.
- Login y registro con el rol `administrador`.
- Pruebas unitarias de las reglas de operación.

