package br.com.listacompras.presentation.historico

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.share.AutoBackupHelper
import br.com.listacompras.share.DownloadsHelper
import br.com.listacompras.share.ExportShareHelper
import br.com.listacompras.share.JsonCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val listas: ListaDao,
    private val itens: ItemDao,
    private val downloads: DownloadsHelper,
    private val share: ExportShareHelper,
    private val backup: AutoBackupHelper
) : ViewModel() {
    var msg by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch {
            runCatching { backup.rodarSeNecessario() }.getOrNull()?.let { msg = it }
        }
    }

    fun exportarParaDownloads() = viewModelScope.launch {
        runCatching {
            val ls = listas.observarTodas().first()
            val todas = ls.map { l -> l to itens.listarDaLista(l.id) }
            val json = JsonCodec.export(todas, share.agoraIso())
            downloads.salvarEmDownloads("listacompras-${System.currentTimeMillis()}.json", json)
            msg = "Salvo em Downloads ✅ (${ls.size} listas)"
        }.onFailure { msg = "Falha ao exportar: ${it.message}" }
    }

    fun importarUri(uri: Uri) = viewModelScope.launch {
        runCatching {
            val texto = downloads.lerUri(uri)
            val r = JsonCodec.import(texto)
            r.listas.forEach { (l, is_) ->
                runCatching { listas.criar(l) }
                is_.forEach { runCatching { itens.adicionar(it) } }
            }
            msg = if (r.erros.isEmpty()) "Importado ${r.listas.size} lista(s) ✅"
            else "Importado com ${r.erros.size} aviso(s): ${r.erros.take(2).joinToString("; ")}"
        }.onFailure { msg = "Arquivo inválido: ${it.message}" }
    }

    fun limpar() { msg = null }
}

@Composable
fun ImportExportCard(vm: ImportExportViewModel = hiltViewModel()) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.importarUri(it) }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        vm.msg?.let { Text(it, color = MaterialTheme.colorScheme.primary); vm.limpar() }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.exportarParaDownloads() }) { Text("Salvar em Downloads") }
            OutlinedButton(onClick = { launcher.launch(arrayOf("application/json", "*/*")) }) { Text("Importar JSON") }
        }
    }
}
