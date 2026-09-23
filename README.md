# Cosecha Directa — HarvestDistributionApp 1.1.0

Aplicación Android local para conectar productores y compradores. Esta versión corrige los controles sin acción, elimina las rutas con datos fijos y conserva cuentas, sesión, productos, solicitudes, perfil y preferencias en el dispositivo.

## Abrir y ejecutar

1. Abre esta carpeta como proyecto en Android Studio.
2. Espera la sincronización de Gradle.
3. Selecciona un emulador o dispositivo con Android 7.0 (API 24) o superior.
4. Ejecuta la configuración `app`.

El proyecto requiere JDK 11 o superior y un SDK capaz de compilar con API 37.

## Arquitectura local

- `PersistentAppRepository`: única puerta de entrada para autenticación, perfiles, productos, solicitudes y ajustes.
- Preferences DataStore: persiste un estado versionado y restaura la sesión al reiniciar el proceso.
- `AppViewModel`: expone el estado reactivo a Compose y ejecuta las mutaciones en coroutines.
- Navegación por identificadores: cada detalle, edición, perfil y solicitud recibe el ID real seleccionado.
- Contraseñas: PBKDF2-HMAC-SHA256 con salt aleatorio; no se guarda la contraseña en texto plano.
- Fotos: el selector del sistema acepta JPG/PNG de hasta 10 MB y copia la imagen al almacenamiento privado de la app.
- Respaldo Android deshabilitado para evitar exportar cuentas y datos locales.

## Funciones corregidas

- Registro, inicio/cierre de sesión y restauración de sesión.
- Búsqueda por texto y filtros reales de categoría, municipio, precio y disponibilidad.
- Detalles exactos de productos y perfiles de productores.
- Cantidad mínima de 1, máximo según inventario, fecha válida y confirmación con datos reales.
- Publicación y edición de productos con foto obligatoria, datos validados y persistencia.
- Solicitudes recibidas exactas; aceptar descuenta inventario y rechazar exige confirmación.
- Bajo stock, búsquedas del productor, perfil, edición de datos, tema y notificaciones locales.

## Integraciones no configuradas

La app no inventa servicios externos. El botón de Google informa que Google Identity aún no está configurado. La recuperación de contraseña informa que necesita un backend/proveedor de identidad y no simula el envío de correo. Las notificaciones guardan la preferencia local, pero la entrega push requiere un servicio remoto.

## Verificación

En Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
.\gradlew.bat :app:assembleRelease
```

La instrumentación incluye dos recorridos críticos de interfaz y dos pruebas del repositorio persistente. Para ejecutarla en un emulador conectado:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

El APK `release` generado por Gradle queda sin firma hasta configurar la clave de distribución del propietario. El APK `debug` sí es instalable para pruebas.
