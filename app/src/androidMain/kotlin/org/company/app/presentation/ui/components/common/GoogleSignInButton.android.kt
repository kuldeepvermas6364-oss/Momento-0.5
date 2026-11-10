package org.company.app.presentation.ui.components.common

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.company.app.R

@Composable
actual fun GoogleSignInButton(onResult: (String?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                try {
                    val googleOption = GetGoogleIdOption.Builder()
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleOption)
                        .build()
                    val credentialResponse = CredentialManager.create(context)
                        .getCredential(context, request)
                    val credential = credentialResponse.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleToken.idToken, null)
                        FirebaseAuth.getInstance()
                            .signInWithCredential(firebaseCredential)
                            .await()
                        onResult(null)
                    } else {
                        onResult("Google account selection did not return a valid credential.")
                    }
                } catch (_: Exception) {
                    onResult("Google sign-in was cancelled or could not be completed. Please try again.")
                }
            }
        }
    ) {
        Text("Continue with Google")
    }
}
