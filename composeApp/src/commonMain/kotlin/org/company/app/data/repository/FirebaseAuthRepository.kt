package org.company.app.data.repository

import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authStateChanged: Flow<FirebaseUser?>
        get() = auth.authStateChanged

    suspend fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password)
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim())
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
