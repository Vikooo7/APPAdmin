# RutaLog Perú · App Admin (operador y administrador)

App de la **operación** del caso 14 (proyecto final): envíos y rutas del parcial, asignación de couriers por zona y
capacidad con el **pronóstico de volumen** (ML-03), entregas fallidas, **riesgo de falla** con aviso previo al
destinatario (ML-01), liquidación a remitentes con Banco Andino, **tablero** con comparación semanal y Mi crédito.

```
App Admin (Compose · MVVM · Room) → API RutaLog (Vercel) → Neon (PostgreSQL) · Banco Andino (por la API)
```

La documentación completa está en [app/src/main/java/com/example/rutalogadmin/README.md](app/src/main/java/com/example/rutalogadmin/README.md).

## Cómo probarla

**Instalar (lo más rápido):** descargar `RutaLog-Admin.apk` desde **Releases** (columna derecha de esta página) en
un celular con Android 10 o superior, permitir "instalar apps de origen desconocido" y abrirla. Con este APK funciona
todo, incluido **Entrar con Google** (solo para cuentas que ya existen).

**O compilarla:** abrir esta carpeta en Android Studio y pulsar **Run**. Incluye `app/google-services.json`
(proyecto Firebase `rutalog-peru`). En un APK compilado en otra PC, *Entrar con Google* necesita que la huella
SHA-1 de esa PC se registre en Firebase; el ingreso con correo y clave funciona siempre.

La app usa la API publicada (`https://app-api-rutalog.vercel.app`): no hace falta levantar ningún servidor.

## Cuentas de prueba

| App | Usuario | Clave | Rol |
|---|---|---|---|
| Admin | `supervisor@rutalog.pe` | `1234` | Operador |
| Admin | `operador@rutalog.pe` | `1234` | Administrador (aprueba liquidaciones, Mi crédito) |
| Cliente | `cliente@rutalog.pe`, `cliente2@rutalog.pe` | `1234` | Remitentes |
| Courier | `courier1@rutalog.pe` … `courier10@rutalog.pe` | `1234` | Couriers |
| Billetera Banco Andino | `andina`, `cliente1` | `banco2026` | Remitente y destinatario |
| Web del comité | `https://app-api-rutalog.vercel.app/banco/comite` | `comite2026` | Aprueba o rechaza Mi crédito |

## Repositorios del proyecto

`APPApi` (API, núcleo de Banco Andino, notebook de ML y Power BI) · `APPCliente` · `APPAdmin` · `AppCourier` ·
`BancoAndino` (billetera).
