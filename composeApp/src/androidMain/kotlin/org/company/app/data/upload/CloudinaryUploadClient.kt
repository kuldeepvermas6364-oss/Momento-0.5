package org.company.app.data.upload

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android upload pipeline for Momento.
 *
 * The Cloudinary API secret is never present in this class. A Cloudflare Worker
 * validates the Firebase ID token and returns a short-lived upload signature.
 */
object CloudinaryUploadClient {
    private const val FIREBASE_DATABASE_URL =
        "https://momento-816d1-default-rtdb.asia-southeast1.firebasedatabase.app"

    data class UploadResult(
        val secureUrl: String,
        val publicId: String,
        val resourceType: String,
        val format: String?,
        val bytes: Long
    )

    suspend fun uploadMedia(
        context: Context,
        fileUri: Uri,
        firebaseUid: String,
        firebaseIdToken: String,
        signingWorkerUrl: String,
        resourceType: String
    ): UploadResult = withContext(Dispatchers.IO) {
        require(resourceType == "image" || resourceType == "video") {
            "Only image and video uploads are supported."
        }
        require(signingWorkerUrl.startsWith("https://")) {
            "A valid HTTPS signing Worker URL is required."
        }
        require(firebaseUid.isNotBlank() && firebaseIdToken.isNotBlank()) {
            "Please sign in before uploading."
        }

        val resolver = context.contentResolver
        val mimeType = resolver.getType(fileUri) ?: "application/octet-stream"
        val fileName = queryFileName(context, fileUri) ?: "momento-${UUID.randomUUID()}"
        val signatureData = requestSignature(
            signingWorkerUrl = signingWorkerUrl,
            firebaseIdToken = firebaseIdToken,
            resourceType = resourceType
        )

        val cloudinaryResponse = uploadToCloudinary(
            context = context,
            uri = fileUri,
            mimeType = mimeType,
            fileName = fileName,
            signatureData = signatureData
        )

        val result = UploadResult(
            secureUrl = cloudinaryResponse.getString("secure_url"),
            publicId = cloudinaryResponse.getString("public_id"),
            resourceType = cloudinaryResponse.optString("resource_type", resourceType),
            format = cloudinaryResponse.optString("format").takeIf { it.isNotBlank() },
            bytes = cloudinaryResponse.optLong("bytes", 0L)
        )

        saveMediaMetadata(firebaseUid, firebaseIdToken, result)
        result
    }

    private fun requestSignature(
        signingWorkerUrl: String,
        firebaseIdToken: String,
        resourceType: String
    ): JSONObject {
        val connection = (URL(signingWorkerUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $firebaseIdToken")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { output ->
                output.write(JSONObject().put("resourceType", resourceType).toString().toByteArray())
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use(BufferedReader::readText)
            if (status !in 200..299) {
                throw IllegalStateException("Upload authorization failed ($status): ${safeError(response)}")
            }
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun uploadToCloudinary(
        context: Context,
        uri: Uri,
        mimeType: String,
        fileName: String,
        signatureData: JSONObject
    ): JSONObject {
        val cloudName = signatureData.getString("cloudName")
        val resourceType = signatureData.getString("resourceType")
        val boundary = "Momento-${UUID.randomUUID()}"
        val connection = (URL(
            "https://api.cloudinary.com/v1_1/$cloudName/$resourceType/upload"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 120_000
            doOutput = true
            setChunkedStreamingMode(256 * 1024)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            connection.outputStream.use { output ->
                writeField(output, boundary, "api_key", signatureData.getString("apiKey"))
                writeField(output, boundary, "timestamp", signatureData.getString("timestamp"))
                writeField(output, boundary, "folder", signatureData.getString("folder"))
                writeField(output, boundary, "signature", signatureData.getString("signature"))

                output.write("--$boundary\r\n".toByteArray())
                output.write(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"${fileName.replace("\"", "")}\"\r\n".toByteArray()
                )
                output.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Unable to open the selected file." }
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
                output.write("\r\n--$boundary--\r\n".toByteArray())
                output.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use(BufferedReader::readText)
            if (status !in 200..299) {
                throw IllegalStateException("Cloudinary upload failed ($status): ${safeError(response)}")
            }
            return JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun writeField(output: OutputStream, boundary: String, name: String, value: String) {
        output.write("--$boundary\r\n".toByteArray())
        output.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
        output.write(value.toByteArray())
        output.write("\r\n".toByteArray())
    }

    private fun saveMediaMetadata(
        uid: String,
        idToken: String,
        upload: UploadResult
    ) {
        val url = "$FIREBASE_DATABASE_URL/media/$uid.json?auth=" +
            URLEncoder.encode(idToken, Charsets.UTF_8.name())
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            val metadata = JSONObject()
                .put("uid", uid)
                .put("secureUrl", upload.secureUrl)
                .put("publicId", upload.publicId)
                .put("resourceType", upload.resourceType)
                .put("format", upload.format)
                .put("bytes", upload.bytes)
                .put("createdAt", mapOf(".sv" to "timestamp"))
            connection.outputStream.use { it.write(metadata.toString().toByteArray()) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val stream = connection.errorStream
                val message = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                throw IllegalStateException("Media uploaded, but Firebase metadata save failed ($status): ${safeError(message)}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else null
        } finally {
            cursor?.close()
        }
    }

    private fun safeError(raw: String): String =
        runCatching { JSONObject(raw).optString("error").ifBlank { "Request could not be completed." } }
            .getOrDefault("Request could not be completed.")
}
