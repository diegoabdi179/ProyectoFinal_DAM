# Reglas específicas de la aplicación para el release.
# Las reglas generales de Android y las reglas consumer de las dependencias las añade AGP.

# Firestore crea estos modelos mediante reflexión al convertir documentos remotos.
-keep class com.example.harvestdistributionapp.data.Product { *; }
-keep class com.example.harvestdistributionapp.data.PurchaseRequest { *; }
-keep class com.example.harvestdistributionapp.data.Usuario { *; }
-keep class com.example.harvestdistributionapp.data.Producto { *; }
-keep class com.example.harvestdistributionapp.data.Solicitud { *; }

# Firebase Messaging localiza el servicio desde AndroidManifest.xml.
-keep class com.example.harvestdistributionapp.service.MyFirebaseMessagingService { *; }

# Cloudinary incluye adaptadores opcionales para estos cargadores de imágenes. La app usa
# el flujo de subida y Coil para mostrar imágenes, por lo que no necesita esos adaptadores.
-dontwarn com.bumptech.glide.**
-dontwarn com.squareup.picasso.**
-dontwarn com.bumptech.glide.Glide
-dontwarn com.bumptech.glide.RequestBuilder
-dontwarn com.bumptech.glide.RequestManager
-dontwarn com.bumptech.glide.load.DataSource
-dontwarn com.bumptech.glide.load.engine.GlideException
-dontwarn com.bumptech.glide.request.BaseRequestOptions
-dontwarn com.bumptech.glide.request.RequestListener
-dontwarn com.bumptech.glide.request.target.Target
-dontwarn com.bumptech.glide.request.target.ViewTarget
-dontwarn com.squareup.picasso.Callback
-dontwarn com.squareup.picasso.Picasso$Builder
-dontwarn com.squareup.picasso.Picasso
-dontwarn com.squareup.picasso.RequestCreator
