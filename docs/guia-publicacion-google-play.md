# Documentación de publicación de Cosecha Directa

## Alcance y estado

Este documento reúne la información técnica y comercial necesaria para preparar la simulación de publicación de Cosecha Directa en Google Play. Distingue lo que ya está implementado en el proyecto de lo que debe completarse antes de una publicación comercial.

| Elemento | Estado actual |
| --- | --- |
| Nombre visible | Cosecha Directa |
| Identificador actual | `com.example.harvestdistributionapp` |
| `versionCode` | `2` |
| `versionName` | `1.1.0` |
| `minSdk` | API 24 |
| `targetSdk` y `compileSdk` | API 37 |
| Minificación de release | R8 y reducción de recursos habilitadas |
| Firma de release | Configurada mediante variables de entorno, sin credenciales en Git |
| Permisos declarados | Internet y notificaciones |
| Política de privacidad pública | Pendiente de publicar en una URL permanente |
| Gráfico de funciones y capturas comerciales | Pendientes de preparar |

El identificador actual es funcional para pruebas, pero es genérico. Antes de crear la ficha definitiva en Google Play se debe elegir un identificador propio y descargar nuevamente el `google-services.json` correspondiente desde Firebase. El identificador de una aplicación publicada no se puede cambiar.

## Descripción de la aplicación

Cosecha Directa conecta a productores y compradores de alimentos locales. La aplicación permite crear una cuenta, consultar productos, buscar por criterios, revisar detalles, publicar productos, gestionar solicitudes de compra y mantener preferencias de usuario. La interfaz está implementada con Jetpack Compose y el proyecto conserva un flujo de persistencia local mediante Preferences DataStore. También contiene un flujo remoto mediante Firebase Authentication y Cloud Firestore para las operaciones configuradas en el repositorio remoto, además de Cloudinary para imágenes de productos.

## Política de privacidad propuesta

### Responsable y contacto

El responsable de la aplicación deberá completar antes de la publicación el nombre legal del responsable, el domicilio o país aplicable y un correo de contacto: **[correo de contacto del responsable]**. La política deberá estar disponible en una URL pública y permanente.

### Texto para la ficha de privacidad

Cosecha Directa es una aplicación que facilita la comunicación comercial entre productores y compradores de alimentos locales. Para ofrecer sus funciones, puede tratar datos que el usuario proporciona directamente, como nombre, correo electrónico, rol dentro de la plataforma, ubicación declarada, nombre del negocio, información de productos, disponibilidad, precios, solicitudes de compra y fotografías seleccionadas por el usuario.

Los datos se utilizan para crear y administrar la cuenta, autenticar al usuario, mostrar productos, procesar solicitudes de compra, actualizar perfiles, conservar preferencias y mantener el funcionamiento de las funciones de la aplicación. Cuando se habilita el flujo remoto, los datos necesarios se procesan mediante Firebase Authentication y Cloud Firestore. Las imágenes de productos pueden enviarse a Cloudinary para almacenarse y mostrarse mediante una URL segura. Estos proveedores tienen sus propias condiciones y políticas de privacidad.

La contraseña no se guarda en texto plano en la persistencia local de la aplicación. El flujo local utiliza un hash derivado mediante PBKDF2-HMAC-SHA256 y un salt aleatorio. En el flujo remoto, la autenticación se gestiona mediante el proveedor configurado. La aplicación no solicita permisos de cámara, ubicación, contactos, micrófono ni SMS. Las fotografías se seleccionan mediante el selector de archivos del sistema y se validan antes de conservarlas o enviarlas.

La aplicación no vende los datos personales del usuario. El responsable deberá definir y publicar los periodos de conservación aplicables, así como el procedimiento para solicitar acceso, rectificación, eliminación o retiro de la cuenta. Las solicitudes deberán dirigirse a **[correo de contacto del responsable]**. El responsable también deberá revisar las reglas de seguridad de Firebase y la configuración de Cloudinary antes de habilitar una publicación pública.

En la versión actual se deshabilitó el respaldo automático de Android para evitar que las cuentas y los datos locales se exporten mediante el mecanismo de backup del sistema. Esta medida no elimina los datos que se encuentren almacenados en los servicios remotos configurados.

### Información que debe declararse en Seguridad de los datos

La declaración final de Play Console debe reflejar la configuración real de la versión que se suba. Como punto de partida, se deben revisar las siguientes categorías:

| Categoría | Ejemplos | Finalidad |
| --- | --- | --- |
| Datos personales | Nombre, correo y rol | Cuenta, autenticación y contacto funcional |
| Ubicación aproximada declarada | Municipio o ubicación escrita por el usuario | Mostrar el origen o ubicación comercial del producto |
| Fotos | Imagen del producto | Publicar y mostrar productos |
| Datos de la aplicación | Productos, precios, inventario y solicitudes | Operación principal del marketplace |
| Preferencias | Notificaciones y tema | Personalización de la experiencia |

La respuesta sobre recopilación, compartición, cifrado en tránsito y eliminación debe confirmarse contra el backend activo, las reglas de Firestore y la configuración de Cloudinary. No debe marcarse una opción únicamente por lo que se planea implementar.

## Justificación de permisos

El `AndroidManifest.xml` actual declara únicamente los siguientes permisos:

| Permiso | Justificación técnica | Uso visible para el usuario |
| --- | --- | --- |
| `android.permission.INTERNET` | Permite la comunicación con Firebase, Cloud Firestore, Cloudinary y recursos remotos de imágenes. | Registro, autenticación, sincronización de productos, solicitudes y carga de imágenes cuando el flujo remoto está activo. |
| `android.permission.POST_NOTIFICATIONS` | Permite publicar notificaciones en Android 13 o superior después de la autorización del usuario. | Avisos relacionados con solicitudes y preferencias de notificación. El usuario puede rechazar el permiso. |

No se solicitan `CAMERA`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `READ_CONTACTS`, `RECORD_AUDIO` ni permisos de SMS. Por esa razón, la aplicación no debe justificar acceso a cámara o ubicación en Play Console. El selector del sistema para fotografías no equivale a solicitar el permiso de cámara.

La aplicación también establece `android:usesCleartextTraffic="false"` para impedir tráfico HTTP sin cifrar desde la configuración general de la aplicación.

## Clasificación de contenido

### Público objetivo

La aplicación está dirigida a productores, compradores y personas adultas interesadas en la comercialización de alimentos locales. No está diseñada específicamente para niños y no incluye contenido infantil como objetivo principal.

### Respuestas previstas para el cuestionario IARC

Estas respuestas son una guía basada en las funciones actuales y deben confirmarse en el cuestionario oficial de Play Console:

| Tema | Respuesta prevista |
| --- | --- |
| Violencia, lesiones o miedo | No |
| Contenido sexual o desnudez | No |
| Lenguaje ofensivo | No |
| Drogas, alcohol o tabaco | No |
| Apuestas o juegos de azar | No |
| Compras dentro de la aplicación | No previstas en la implementación actual |
| Anuncios | No se integra un SDK de anuncios en esta versión |
| Interacción entre usuarios | Sí, mediante cuentas, productos y solicitudes de compra; no hay chat público implementado |
| Público infantil | No es el público objetivo |

Con estas características, la clasificación esperada es una categoría para todos, como PEGI 3 o equivalente. La clasificación oficial la asigna IARC a partir de las respuestas enviadas y de la configuración real de la versión publicada.

## Textos ASO

### Título

**Cosecha Directa**

El título se encuentra dentro del límite de 30 caracteres.

### Descripción breve

**Conecta productores y compradores de alimentos locales.**

La descripción breve se encuentra dentro del límite de 80 caracteres.

### Descripción completa

Cosecha Directa facilita la conexión entre productores y compradores de alimentos locales.

Encuentra productos por texto, categoría, municipio, precio y disponibilidad. Consulta la información del productor y revisa cada producto antes de enviar una solicitud de compra.

Si eres productor, publica y edita tus productos con nombre, descripción, precio, inventario, ubicación y fotografía. La aplicación valida los datos y ayuda a mantener actualizado el inventario cuando una solicitud es aceptada.

También puedes administrar tu perfil, revisar solicitudes, configurar tus preferencias y recibir avisos relacionados con la actividad de la aplicación. Cosecha Directa está pensada para hacer más claro y sencillo el contacto entre quienes producen y quienes buscan alimentos locales.

Las funciones que dependen de servicios remotos requieren la configuración correspondiente de Firebase y del servicio de imágenes. La disponibilidad de las funciones puede variar según la conectividad y la configuración de la cuenta.

## Recursos gráficos

El ícono proporcionado por el responsable se integró en los recursos `mipmap` de la aplicación en sus densidades Android y se utiliza también en las variantes normal y redonda. El archivo original tiene resolución de 512 por 512 píxeles y fondo transparente en las esquinas.

Antes de publicar se deben preparar y revisar los siguientes recursos comerciales:

- Ícono de ficha de Play Store en PNG de 512 por 512 píxeles.
- Gráfico de funciones de 1024 por 500 píxeles.
- Entre tres y ocho capturas de pantalla de teléfono.
- Capturas adicionales de tablet si se declara compatibilidad y se desea optimizar esa ficha.
- Textos y gráficos que no contengan afirmaciones falsas, calificaciones inventadas ni botones que simulen una funcionalidad inexistente.

## Preparación técnica del APK y AAB

Google Play utiliza el formato AAB para nuevas aplicaciones. El proyecto admite firma de release mediante estas variables de entorno:

```text
HARVEST_RELEASE_STORE_FILE
HARVEST_RELEASE_STORE_PASSWORD
HARVEST_RELEASE_KEY_ALIAS
HARVEST_RELEASE_KEY_PASSWORD
```

El archivo `.jks`, sus contraseñas y los archivos DPAPI de configuración deben permanecer fuera del repositorio. Para generar un release firmado se deben configurar esas variables y ejecutar:

```powershell
.\gradlew.bat :app:assembleRelease :app:bundleRelease
```

El APK se verifica con `apksigner` y el AAB con `jarsigner`. La aplicación debe inscribirse en Play App Signing para que Google Play administre la clave de firma de la aplicación. La clave local utilizada para subir el AAB es una clave de subida; no debe confundirse con la clave de firma final que conserva Google Play.

La huella pública SHA-256 del certificado de subida utilizado para la compilación de entrega es:

```text
b7e1d56349193db865a05d903fe63cf0920ad657e1df9ac884dfeceabdeccc80
```

Si Firebase o algún proveedor de API exige huellas digitales, se deben registrar las huellas que correspondan tanto a la clave de subida como a la clave de firma de Play App Signing después de que Google la genere o la muestre en Play Console.

## Simulación de Google Play Console

1. Crear la aplicación en Play Console usando el identificador definitivo y el nombre Cosecha Directa.
2. Seleccionar el idioma principal, indicar si la aplicación es gratuita o de pago y aceptar las declaraciones iniciales.
3. Completar la ficha principal con el título, descripción breve, descripción completa, ícono, gráfico de funciones y capturas.
4. Publicar la URL de la política de privacidad y completar Seguridad de los datos con base en la versión real del backend.
5. Completar el cuestionario de clasificación de contenido y declarar el público objetivo.
6. Configurar la sección de contenido de la aplicación, anuncios, acceso a la aplicación y cualquier declaración relacionada con datos personales.
7. Crear una pista de pruebas internas y subir el archivo AAB firmado con la clave de subida.
8. Instalar la versión desde la pista interna en los dispositivos de prueba y comprobar inicio de sesión, registro, productos, solicitudes, imágenes, notificaciones y restauración de sesión.
9. Revisar los informes de errores, compatibilidad y seguridad de la versión.
10. Enviar la versión a revisión y corregir cualquier observación de Play Console antes de solicitar el lanzamiento público.

## Lista de pendientes antes de publicación comercial

- Sustituir `com.example.harvestdistributionapp` por el identificador definitivo y descargar el `google-services.json` coincidente.
- Publicar la política de privacidad en una URL estable y reemplazar el correo de contacto pendiente.
- Revisar las reglas de Firestore, la autenticación de Firebase y la configuración de Cloudinary.
- Completar el formulario de Seguridad de los datos con el backend activo.
- Preparar el gráfico de funciones y las capturas de teléfono y tablet.
- Subir el AAB a pruebas internas mediante Play App Signing.
- Resolver o documentar el aviso de compatibilidad de tamaño de página de 16 KB observado en el emulador. El APK puede instalarse en modo compatible, pero las bibliotecas nativas deben actualizarse o reconstruirse para una distribución comercial completamente compatible.
- No subir al repositorio el keystore, las contraseñas, los certificados privados ni los APK/AAB generados como archivos de código fuente.

## Referencias oficiales

- [Android App Bundles](https://developer.android.com/guide/app-bundle)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=es)
- [Firma de aplicaciones en Android Studio](https://developer.android.com/studio/publish/app-signing)
