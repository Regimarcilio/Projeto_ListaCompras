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
    }

    val temaEscuro: Flow<Boolean> = ctx.settingsStore.data.map { it[TEMA_ESCURO] ?: false }
    val backupAuto: Flow<Boolean> = ctx.settingsStore.data.map { it[BACKUP_AUTO] ?: true }
    val ultimoBackup: Flow<Long> = ctx.settingsStore.data.map { it[ULTIMO_BACKUP] ?: 0L }

    suspend fun setTema(escuro: Boolean) { ctx.settingsStore.edit { it[TEMA_ESCURO] = escuro } }
    suspend fun setBackupAuto(v: Boolean) { ctx.settingsStore.edit { it[BACKUP_AUTO] = v } }
    suspend fun setUltimoBackup(ts: Long) { ctx.settingsStore.edit { it[ULTIMO_BACKUP] = ts } }
}
