package com.example.harvestdistributionapp.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ProductImageStore {
    private const val MAX_IMAGE_BYTES = 10L * 1024L * 1024L

    suspend fun copyToPrivateStorage(context: Context, source: Uri, mimeType: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                else -> return@withContext AppResult.Error("El formato de imagen no es compatible")
            }
            val directory = File(context.filesDir, "product_images")
            if (!directory.exists() && !directory.mkdirs()) {
                return@withContext AppResult.Error("No se pudo preparar el almacenamiento de imágenes")
            }
            val destination = File(directory, "product-${UUID.randomUUID()}.$extension")
            runCatching {
                val input = context.contentResolver.openInputStream(source)
                    ?: error("No se pudo abrir la imagen seleccionada")
                input.use { sourceStream ->
                    destination.outputStream().use { destinationStream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalBytes = 0L
                        while (true) {
                            val bytesRead = sourceStream.read(buffer)
                            if (bytesRead < 0) break
                            totalBytes += bytesRead
                            check(totalBytes <= MAX_IMAGE_BYTES) { "La imagen supera el límite de 10 MB" }
                            destinationStream.write(buffer, 0, bytesRead)
                        }
                    }
                }
                check(destination.length() > 0L) { "La imagen seleccionada está vacía" }
                Uri.fromFile(destination).toString()
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = {
                    destination.delete()
                    AppResult.Error(it.message ?: "No se pudo guardar la imagen")
                }
            )
        }
}
