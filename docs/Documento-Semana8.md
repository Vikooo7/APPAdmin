# RutaLog Perú · App Operador logístico / Administrador

**Evaluación Parcial, Semana 8** · Desarrollo de Aplicaciones Móviles · Docente: Guillermo Peña
**Estudiante:** Victor Manuel Serrano Pure · **Caso 14:** RutaLog Perú

Este documento resume la arquitectura, las tablas, los endpoints, los permisos y la sincronización de la App Operador. La App Cliente y la API REST usan la misma base remota.

---

## 1. Arquitectura

```
Compose → ViewModel → Repository → Room / Retrofit → API REST → BD remota (Neon)
                                  ↓
                         Cola local → SyncWorker
```

| Pieza | Tecnología | Dónde está |
|---|---|---|
| App Operador | Kotlin, Jetpack Compose, MVVM | `Vikooo7/APPAdmin` |
| API REST | Node.js + Express, publicada en Vercel | `Vikooo7/APPApi` · `https://app-api-rutalog.vercel.app` |
| Base remota | PostgreSQL en Neon | Solo la API se conecta a ella |

### Responsabilidad de cada capa

| Capa | Carpeta | Qué hace |
|---|---|---|
| View | `ui/` | Pantallas Compose y navegación. No consulta DAO ni Retrofit |
| ViewModel | `viewmodel/` | Guarda el estado de pantalla y llama al Repository |
| Repository | `data/repository/` | Coordina Room y la API, y registra las operaciones pendientes |
| Room | `data/local/` | Entidades, DAO y `AppDatabase`: datos persistentes y cola local |
| API | `data/remote/` | DTO, endpoints y cliente Retrofit |
| Mapper | `data/mapper/` | Convierte DTO ↔ entidades Room ↔ modelos de la app |
| Worker | `worker/` | Sincroniza con `SyncRepository` aunque no haya pantalla abierta |
| Dependencias | `di/` | `AppContainer` crea una sola vez Room, la API y los repositorios |

**Regla central:** las pantallas siempre leen de Room. La API solo actualiza esa copia local. Por eso la app muestra los mismos datos con conexión y sin ella.

---

## 2. Tablas

### 2.1. Room (en el teléfono, `rutalog_admin.db`)

| Tabla | Campos | Uso |
|---|---|---|
| `usuarios` | id, nombre, correo, rol | Perfil del operador con sesión. El id es el del servidor. **No guarda la contraseña** |
| `envios` | id, idRemoto, uuid, numeroGuia, ruta, pesoKg, costoEnvio, estado, transportistaAsignado, version, estadoSync, mensajeError | Copia local de los envíos. El operador cambia `estado` y `transportistaAsignado` |
| `rutas` | id, nombre, tiempoEstimado | Rutas activas; `tiempoEstimado` en horas. Solo lectura |
| `operaciones_pendientes` | id, uuidOperacion, entidad, idEntidadLocal, tipoOperacion, payload, fechaRegistro, estado, intentos, mensajeError | Cola de cambios por enviar a la API |
| `sync_metadata` | recurso, ultimaSincronizacionExitosa, cursorRemoto | Fecha de la última sincronización de `envios` y de `rutas` |

Campos de sincronización de `envios`:

| Campo | Para qué sirve |
|---|---|
| `id` | Identificador local. No cambia aunque el envío se descargue otra vez |
| `idRemoto` | Identificador del envío en el servidor |
| `uuid` | Identificador único del envío, igual en el teléfono y en el servidor |
| `version` | Versión del servidor. Sirve para detectar conflictos |
| `estadoSync` | PENDIENTE, ENVIANDO, SINCRONIZADO o ERROR |

**Dos estados distintos.** El estado del negocio (`estado`: pendiente, recogido, en tránsito, en reparto, entregado) es independiente del estado de sincronización (`estadoSync`). Un envío puede estar "en tránsito" y "pendiente de enviar" a la vez.

### 2.2. Base remota (PostgreSQL en Neon)

| Tabla | Campos | Uso |
|---|---|---|
| `usuarios` | id, nombre, correo, clave_hash, rol | La clave se guarda como hash bcrypt |
| `rutas` | id, nombre, tiempo_estimado, tarifa_por_kg, region, activa | Rutas y tarifas |
| `envios` | id, uuid, numero_guia, ruta, peso_kg, costo_envio, estado, transportista_asignado, usuario_id, version, eliminado, actualizado_en | Envíos de todos los clientes |
| `operaciones_procesadas` | uuid_operacion, usuario_id, tipo, id_envio, estado_http, respuesta, fecha | Una fila por cada escritura aceptada. Evita aplicar dos veces la misma operación |

Datos de ejemplo: 3 usuarios, 27 rutas y 40 envíos. El esquema completo está en `db/esquema.sql` de la API.

---

## 3. Endpoints

La App Operador usa estos cuatro:

| Método | Ruta | Para qué |
|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión. Devuelve `token` y `usuario` |
| `GET` | `/api/envios` | Descargar todos los envíos (RF11) |
| `PUT` | `/api/envios/{id}` | Cambiar estado y transportista (RF08, RF09) |
| `GET` | `/api/rutas` | Descargar las rutas activas (RF10) |

La API tiene además `GET /api/envios/{id}`, `POST /api/envios` y `DELETE /api/envios/{id}`, que usa la App Cliente.

Las rutas con sesión exigen el encabezado `Authorization: Bearer <token>`.

**Ejemplo de escritura**

```
PUT /api/envios/35
{
  "uuidOperacion": "5f0c…",          ← identifica esta operación (RF15)
  "estado": "recogido",
  "transportistaAsignado": "Carlos Quispe Mamani · AQP-482",
  "version": 1                        ← versión que conocía el teléfono
}
```

**Formato de los errores**

```json
{ "codigo": "VERSION_DESACTUALIZADA", "mensaje": "El envío cambió en el servidor…", "envio": { … } }
```

En un conflicto (409), `envio` trae el envío tal como está en el servidor.

---

## 4. Permisos

El servidor valida los permisos; la app no decide sola.

| Acción | Cliente | Administrador |
|---|---|---|
| Iniciar sesión | Sí | Sí |
| Ver envíos | Solo los suyos | Todos |
| Registrar un envío | Sí | No (`403 SIN_PERMISO`) |
| Cambiar ruta y peso | Solo si sigue pendiente | No |
| Cambiar estado y transportista | No | Sí |
| Eliminar un envío | Solo si sigue pendiente | La API lo permite si sigue pendiente; la App Operador no lo usa |
| Ver rutas | Sí | Sí |

Controles adicionales:
- **Rol en la app:** si una cuenta que no es de administrador intenta entrar a esta app, el Login la rechaza.
- **Sesión:** token JWT válido por 30 días. En el teléfono se guarda cifrado con el Android Keystore (`SesionSegura`).
- **Contraseña:** nunca se guarda en el teléfono.
- **Acceso sin conexión:** quien ya inició sesión entra aunque no haya red. Un inicio de sesión nuevo necesita internet.

Reglas que valida el servidor al cambiar un envío como administrador:

| Regla | Respuesta |
|---|---|
| El estado no retrocede | `409 ESTADO_NO_RETROCEDE` |
| Un envío entregado no se modifica | `409 ENVIO_CERRADO` |
| Fuera de "pendiente" debe haber transportista | `422 SIN_TRANSPORTISTA` |
| El envío cambió en el servidor desde la última descarga | `409 VERSION_DESACTUALIZADA` |

La app aplica las tres primeras antes de guardar (`ReglasOperacion`), para que un cambio hecho sin conexión no se rechace después.

---

## 5. Sincronización

### 5.1. Guardar un cambio (RF12)

Cuando el operador cambia el estado o el transportista, `EnvioRepository.guardarOperacion()` hace dos cosas **en una sola transacción de Room**:

1. Actualiza el envío y lo marca `PENDIENTE`.
2. Inserta la operación en `operaciones_pendientes`, con un `uuidOperacion` nuevo.

Si algo falla, no se guarda ninguna de las dos. Esto funciona igual con conexión y sin ella. Los datos y la cola se conservan al cerrar la app.

Si el operador cambia dos veces el mismo envío antes de sincronizar, la cola conserva una sola operación con el resultado final.

### 5.2. Cuándo se sincroniza (RF13)

| Momento | Quién lo hace |
|---|---|
| Al entrar a la app | `SyncViewModel` |
| Al guardar un cambio, si hay conexión | `SyncViewModel` |
| Al recuperar la conexión, con la app abierta | `SyncViewModel` (escucha `ConnectivityObserver`) |
| Al recuperar la conexión, con la app cerrada | `SyncWorker` (WorkManager con restricción de red). Queda programado cada vez que se guarda un cambio |
| Cuando el operador lo pide | Botón "Sincronizar ahora" |

Un candado (`Mutex`) evita que dos sincronizaciones corran a la vez.

### 5.3. Qué hace una sincronización

`SyncRepository.sincronizar()`:

1. **Envía la cola**, en orden de registro. Cada operación pasa a `ENVIANDO`.
2. **Descarga** rutas y envíos, y actualiza Room.

Resultado de cada operación enviada:

| Respuesta del servidor | Qué hace la app |
|---|---|
| Aceptada | Quita la operación de la cola. El envío queda `SINCRONIZADO` con la versión nueva |
| Sin red, tiempo agotado o error 5xx | La operación vuelve a `PENDIENTE` y suma un intento. Se reintenta después |
| Rechazada (regla o conflicto) | La operación y el envío quedan en `ERROR`, con el motivo. No se pierde nada |
| Sesión vencida (401) | La operación vuelve a `PENDIENTE`; no cuenta como rechazo |

### 5.4. Descarga sin pisar cambios locales (RF11)

Al guardar lo descargado:
- Un envío nuevo en el servidor se inserta.
- Un envío sin cambios locales se actualiza.
- Un envío con cambios pendientes o rechazados **no se toca**.
- Un envío que el servidor ya no devuelve se quita, salvo que tenga cambios pendientes.

### 5.5. Sin duplicados (RF15)

Cada operación lleva su `uuidOperacion`. El servidor guarda los que ya aplicó en `operaciones_procesadas`. Si el mismo UUID llega otra vez (por un reintento), devuelve la respuesta guardada con `"duplicado": true` y no repite el cambio.

### 5.6. Conflictos (RF15)

Cuando el servidor rechaza un cambio, la pantalla Sincronización muestra el motivo y cómo está el envío en el servidor. El operador elige:

- **Reintentar:** vuelve a enviar el cambio. La app ya tomó la versión actual del servidor, así que el cambio se aplica sobre el dato vigente.
- **Descartar mi cambio:** el envío vuelve a como estaba antes del cambio y se actualiza con lo que diga el servidor.

### 5.7. Qué ve el operador (RF14)

| Dónde | Qué muestra |
|---|---|
| Pantalla Sincronización | En línea o sin conexión, última sincronización, pendientes, rechazadas y la lista de operaciones |
| Pestaña Sincronizar | Un globo con el número de operaciones en cola |
| Cada envío de la lista | Pendiente de enviar, Enviando, Sincronizado o Rechazado |
| Detalle del envío | Id remoto, versión y el motivo si fue rechazado |
| Todas las pantallas | Franja "Sin conexión" cuando no hay red |

### 5.8. Manejo de errores de red

`RetrofitClient` usa 10 s para conectar y 20 s para leer. `llamarApi()` clasifica cada respuesta en éxito, rechazo (4xx) o fallo reintentable (sin red, tiempo agotado o 5xx).

---

## 6. Pruebas

### 6.1. Prueba online

1. Iniciar sesión: la app descarga los envíos y las rutas a Room.
2. Cambiar el estado de un envío y asignarle transportista: pasa a `SINCRONIZADO`.
3. Sincronizar la App Cliente con el dueño del envío: recibe el cambio.

### 6.2. Prueba offline y recuperación

1. Con datos ya descargados, activar el modo avión.
2. Consultar los envíos y guardar un cambio: queda `PENDIENTE`.
3. Cerrar y abrir la app: el cambio sigue `PENDIENTE`.
4. Quitar el modo avión: se envía solo y queda `SINCRONIZADO`, con su id remoto.
5. En Neon, la tabla `operaciones_procesadas` tiene una sola fila para ese cambio.

### 6.3. Error del servidor

1. Activar "Simular error del servidor" en la pantalla Sincronización.
2. Guardar un cambio y sincronizar: la API responde 503 y la operación se conserva en la cola.
3. Desactivar el interruptor y sincronizar: se envía.

### 6.4. Conflicto

1. En modo avión, cambiar un envío pendiente.
2. En la PC, ejecutar `pruebas/simular-conflicto.ps1 -Guia "<guía>"` (carpeta de la API). Simula que el cliente cambió el peso de ese envío.
3. Quitar el modo avión: el cambio queda `ERROR` con el motivo "El envío cambió en el servidor…".
4. Probar **Reintentar** o **Descartar mi cambio**.

### 6.5. Pruebas automáticas

| Proyecto | Pruebas | Qué revisan |
|---|---|---|
| App Operador | 16 (`gradlew testDebugUnitTest`) | Reglas de RF08 y RF09, clasificación de respuestas de la API, conversión de datos y texto de sincronización |
| API | 8 (`npm test`) | Reglas del administrador y datos de ejemplo |

---

## 7. Evidencias

Capturas para adjuntar:

| N.º | Evidencia | Requisito |
|---|---|---|
| 1 | Panel con los 40 envíos después del login | RF06, RF11 |
| 2 | Lista filtrada por estado y por ruta | RF07 |
| 3 | Envío `PENDIENTE` con la franja "Sin conexión" | RF12 |
| 4 | El mismo envío `PENDIENTE` después de cerrar y abrir la app | Persistencia |
| 5 | El envío `SINCRONIZADO` con su id remoto | RF13 |
| 6 | Pantalla Sincronización con pendientes, rechazadas y última fecha | RF14 |
| 7 | Operación rechazada con el motivo del conflicto | RF15 |
| 8 | Tabla `operaciones_procesadas` en Neon con una sola fila por operación | Sin duplicados |
| 9 | La App Cliente mostrando el estado que cambió el operador | Prueba online |
