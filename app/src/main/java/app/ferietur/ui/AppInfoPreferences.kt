package app.ferietur.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appInfoDataStore by preferencesDataStore(name = "ferietur_app_info")

internal object AppInfoPreferences {
    const val CURRENT_DISCLAIMER_VERSION: Int = 2

    private val disclaimerAcknowledgementVersionKey =
        intPreferencesKey("disclaimer_ack_version")
    private val lastAcknowledgedVersionCodeKey =
        intPreferencesKey("last_acknowledged_version_code")

    fun disclaimerAcknowledgementVersion(context: Context): Flow<Int> =
        context.appInfoDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                preferences[disclaimerAcknowledgementVersionKey] ?: 0
            }

    suspend fun acknowledgeCurrentDisclaimer(context: Context) {
        context.appInfoDataStore.edit { preferences ->
            preferences[disclaimerAcknowledgementVersionKey] =
                CURRENT_DISCLAIMER_VERSION
        }
    }

    fun lastAcknowledgedVersionCode(context: Context): Flow<Int> =
        context.appInfoDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                preferences[lastAcknowledgedVersionCodeKey] ?: 0
            }

    suspend fun acknowledgeVersionCode(
        context: Context,
        versionCode: Int,
    ) {
        require(versionCode >= 0) { "versionCode must be non-negative" }
        context.appInfoDataStore.edit { preferences ->
            preferences[lastAcknowledgedVersionCodeKey] = versionCode
        }
    }
}
