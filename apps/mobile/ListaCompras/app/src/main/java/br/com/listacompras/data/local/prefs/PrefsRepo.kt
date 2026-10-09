package br.com.listacompras.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.settingsStore by preferencesDataStore("listacompras_prefs")

class PrefsRepo @Inject constructor(@ApplicationContext private val ctx: Context) {
    companion object {
        val TEMA_ESCURO = booleanPreferencesKey("tema_escuro")
        val BACKUP_AUTO = booleanPreferencesKey("backup_auto")
        val ULTIMO_BACKUP = longPreferencesKey("ultimo_backup")
        val FILTRO_TIPO = stringPreferencesKey("filtro_tipo")
        // #22: sync outbox — endpoint https + e-mail do dono (vazios por padrão = sem tráfego).
        val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
        val OWNER_EMAIL = stringPreferencesKey("owner_email")
    }

    val temaEscuro: Flow<Boolean> = ctx.settingsStore.data.map { it[TEMA_ESCURO] ?: false }
    // Nullable: null = nunca configurado -> UI usa isSystemInDarkTheme() como fallback.
    val temaEscuroOrNull: Flow<Boolean?> = ctx.settingsStore.data.map { it[TEMA_ESCURO] }
    val backupAuto: Flow<Boolean> = ctx.settingsStore.data.map { it[BACKUP_AUTO] ?: true }
    val ultimoBackup: Flow<Long> = ctx.settingsStore.data.map { it[ULTIMO_BACKUP] ?: 0L }
    // #22: fluxos do sync (string vazia = não configurado).
    val endpointUrl: Flow<String> = ctx.settingsStore.data.map { it[ENDPOINT_URL] ?: "" }
    val ownerEmail: Flow<String> = ctx.settingsStore.data.map { it[OWNER_EMAIL] ?: "" }

    suspend fun setTema(escuro: Boolean) { ctx.settingsStore.edit { it[TEMA_ESCURO] = escuro } }
    suspend fun setBackupAuto(v: Boolean) { ctx.settingsStore.edit { it[BACKUP_AUTO] = v } }
    suspend fun setUltimoBackup(ts: Long) { ctx.settingsStore.edit { it[ULTIMO_BACKUP] = ts } }
    // #22: setters do sync.
    suspend fun setEndpointUrl(v: String) { ctx.settingsStore.edit { it[ENDPOINT_URL] = v.trim() } }
    suspend fun setOwnerEmail(v: String) { ctx.settingsStore.edit { it[OWNER_EMAIL] = v.trim() } }
}
