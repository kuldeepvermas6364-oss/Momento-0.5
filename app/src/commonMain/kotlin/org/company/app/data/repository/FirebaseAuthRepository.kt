package org.company.app.data.repository

import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
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

    suspend fun ensureUserProfile() {
        val user = auth.currentUser ?: return
        val updates = mapOf(
            "users/${user.uid}/uid" to user.uid,
            "users/${user.uid}/email" to user.email,
            "users/${user.uid}/displayName" to user.displayName,
            "users/${user.uid}/photoUrl" to user.photoURL,
            "users/${user.uid}/providerId" to user.providerId,
            "users/${user.uid}/updatedAt" to mapOf(".sv" to "timestamp"),
            "profiles/${user.uid}/uid" to user.uid,
            "profiles/${user.uid}/email" to user.email,
            "profiles/${user.uid}/displayName" to user.displayName,
            "profiles/${user.uid}/photoUrl" to user.photoURL,
            "profiles/${user.uid}/updatedAt" to mapOf(".sv" to "timestamp")
        )
        database.reference().updateChildren(updates)
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
