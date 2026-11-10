package org.company.app

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.company.app.data.upload.CloudinaryUploadClient

private const val MOMENTO_CLOUDINARY_SIGNING_URL =
    "https://momento-cloudinary-sign.kuldeepvermas6364.workers.dev"

@Composable
internal fun AndroidCloudinaryUploadButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPickerOptions by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun upload(uri: Uri, resourceType: String) {
        scope.launch {
            isUploading = true
            statusMessage = null
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: error("Please sign in to Momento before uploading.")
                val idToken = user.getIdToken(false).await().token
                    ?: error("Could not get a fresh Firebase sign-in token.")
                val result = CloudinaryUploadClient.uploadMedia(
                    context = context,
                    fileUri = uri,
                    firebaseUid = user.uid,
                    firebaseIdToken = idToken,
                    signingWorkerUrl = MOMENTO_CLOUDINARY_SIGNING_URL,
                    resourceType = resourceType
                )
                statusMessage = "Upload complete. Your media is saved in Momento.\n${result.secureUrl}"
            } catch (error: Exception) {
                statusMessage = error.message ?: "Upload failed. Please try again."
            } finally {
                isUploading = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { upload(it, "image") } }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { upload(it, "video") } }

    FloatingActionButton(
        onClick = {
            if (FirebaseAuth.getInstance().currentUser == null) {
                Toast.makeText(context, "Please sign in before uploading.", Toast.LENGTH_SHORT).show()
            } else {
                showPickerOptions = true
            }
        },
        modifier = modifier.padding(end = 18.dp, bottom = 92.dp),
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Upload media")
            Text("Upload")
        }
    }

    if (showPickerOptions) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showPickerOptions = false },
            title = { Text("Upload to Momento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Choose a photo or video. Your upload is stored in your Momento media folder.")
                    OutlinedButton(
                        onClick = {
                            showPickerOptions = false
                            imagePicker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading
                    ) { Text("Choose photo") }
                    OutlinedButton(
                        onClick = {
                            showPickerOptions = false
                            videoPicker.launch("video/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading
                    ) { Text("Choose video") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPickerOptions = false }, enabled = !isUploading) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isUploading || statusMessage != null) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) statusMessage = null },
            title = { Text(if (isUploading) "Uploading media" else "Upload status") },
            text = {
                if (isUploading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text("Uploading securely to Cloudinary…")
                    }
                } else {
                    Text(statusMessage.orEmpty())
                }
            },
            confirmButton = {
                if (!isUploading) {
                    TextButton(onClick = { statusMessage = null }) { Text("Done") }
                }
            }
        )
    }
}
