package org.company.app.data.repository

import dev.gitlive.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteSettings(
    @SerialName("maintenanceMode") val maintenanceMode: Boolean = false,
    @SerialName("maintenanceMessage") val maintenanceMessage: String = "",
    @SerialName("announcement") val announcement: String = "",
    @SerialName("announcementEnabled") val announcementEnabled: Boolean = false,
    @SerialName("theme") val theme: String = "system",
    @SerialName("homeLayout") val homeLayout: String = "list",
    @SerialName("homeColumns") val homeColumns: Int = 2,
    @SerialName("features") val features: Map<String, Boolean> = emptyMap(),
    @SerialName("api") val api: Map<String, String> = emptyMap(),
    @SerialName("updatedAt") val updatedAt: Long? = null
)

class RemoteSettingsRepository(
    private val database: FirebaseDatabase
) {
    fun observe(): Flow<RemoteSettings> = flow {
        try {
            database.reference()
                .child("remoteSettings")
                .valueEvents
                .map { snapshot -> snapshot.value<RemoteSettings?>() ?: RemoteSettings() }
                .collect { emit(it) }
        } catch (t: Throwable) {
            println("RemoteSettings error: ${t.message}")
            emit(RemoteSettings())
        }
    }.catch { emit(RemoteSettings()) }
}
