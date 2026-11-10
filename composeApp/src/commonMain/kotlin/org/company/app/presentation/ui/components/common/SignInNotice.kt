package org.company.app.presentation.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.company.app.data.repository.FirebaseAuthRepository
import org.koin.compose.koinInject

@Composable
fun SignInNotice(onDismiss: () -> Unit) {
    val authRepository: FirebaseAuthRepository = koinInject()
    val scope = rememberCoroutineScope()
    var isCreateAccount by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
        title = { Text(if (isCreateAccount) "Create Momento account" else "Sign in to Momento") },
        text = {
            Column {
                Text("Use your email and password to continue.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !busy
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !busy
                )
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && email.contains("@") && password.length >= 6,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            if (isCreateAccount) authRepository.createAccount(email, password)
                            else authRepository.signIn(email, password)
                            onDismiss()
                        } catch (_: Exception) {
                            message = "Could not complete sign-in. Check your details and try again."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text(if (busy) "Please wait…" else if (isCreateAccount) "Create account" else "Sign in")
            }
        },
        dismissButton = {
            Column {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        isCreateAccount = !isCreateAccount
                        message = null
                    }
                ) {
                    Text(if (isCreateAccount) "Already have an account? Sign in" else "Create account")
                }
                if (!isCreateAccount) {
                    TextButton(
                        enabled = !busy && email.contains("@"),
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                try {
                                    authRepository.sendPasswordReset(email)
                                    message = "Password reset email sent. Check your inbox."
                                } catch (_: Exception) {
                                    message = "Could not send reset email. Check the address and try again."
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    ) { Text("Forgot password?") }
                }
            }
        }
    )
}
