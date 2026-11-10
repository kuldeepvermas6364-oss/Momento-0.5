package org.company.app.presentation.ui.components.common

import androidx.compose.runtime.Composable

@Composable
expect fun GoogleSignInButton(onResult: (String?) -> Unit)
