package org.company.app.presentation.ui.components.common

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun GoogleSignInButton(onResult: (String?) -> Unit) {
    Button(onClick = { onResult("Google sign-in is currently available in the Android app.") }) {
        Text("Continue with Google")
    }
}
