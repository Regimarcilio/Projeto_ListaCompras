package br.com.listacompras.share

import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.prefs.PrefsRepo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Backup silencioso: se ativado e passou 7 dias desde o último, salva em Downloads. */
class AutoBackupHelper @Inject constructor(
    private val listas: ListaDao,
    private val itens: ItemDao,
    private val prefs: PrefsRepo,
    private val downloads: DownloadsHelper,
    private val share: ExportShareHelper
) {
    suspend fun rodarSeNecessario(): String? {
        if (!prefs.backupAuto.first()) return null
        val ultimo = prefs.ultimoBackup.first()
        val agora = System.currentTimeMillis()
        if (agora - ultimo < 7 * 24 * 60 * 60 * 1000L) return null
        val ls = listas.observarTodas().first()
        if (ls.isEmpty()) return null
        val todas = ls.map { l -> l to itens.listarDaLista(l.id) }
        val json = JsonCodec.export(todas, share.agoraIso())
        downloads.salvarEmDownloads("backup-listacompras-${agora}.json", json)
        prefs.setUltimoBackup(agora)
        return "Backup automático salvo em Downloads ✅"
    }
}
