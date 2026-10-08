package br.com.listacompras.presentation.historico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.usecase.ListaOpsUseCase
import br.com.listacompras.share.ExportShareHelper
import br.com.listacompras.share.JsonCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoricoViewModel @Inject constructor(
    private val listas: ListaDao,
    private val itens: ItemDao,
    private val ops: ListaOpsUseCase,
    private val share: ExportShareHelper
) : ViewModel() {
    val listasFlow: StateFlow<List<ListaEntity>> = listas.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var msg by mutableStateOf<String?>(null); private set

    fun finalizar(id: String) = viewModelScope.launch { ops.finalizar(id); msg = "Lista finalizada com data ✅" }
    fun duplicar(id: String, zerar: Boolean = true) = viewModelScope.launch {
        val nova = ops.duplicar(id, zerar); msg = "Duplicada: $nova"
    }
    fun exportarTodas() = viewModelScope.launch {
        val ls = listasFlow.value
        val todas = ls.map { l -> l to itens.listarDaLista(l.id) }
        val json = JsonCodec.export(todas, share.agoraIso())
        val file = share.salvarJson("listacompras-${System.currentTimeMillis()}.json", json)
        val total = todas.sumOf { (_, is_) -> is_.sumOf { (it.precoUnit ?: 0.0) * it.quantidade } }
        val texto = share.listaParaTexto("Minhas listas (${ls.size})", emptyList(), total)
        share.shareJson(file, texto)
        msg = "JSON exportado: ${file.name}"
    }
    fun limparMsg() { msg = null }
}

@Composable
fun HistoricoScreen(vm: HistoricoViewModel = hiltViewModel()) {
    val ls by vm.listasFlow.collectAsState()
    var novaLista by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Text("Histórico + Export", style = MaterialTheme.typography.titleLarge)
        vm.msg?.let { Text(it, color = MaterialTheme.colorScheme.primary); vm.limparMsg() }
        Spacer(Modifier.height(8.dp))
        ImportExportCard()
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.exportarTodas() }, modifier = Modifier.fillMaxWidth()) {
            Text("Exportar JSON + Compartilhar (SMS/WhatsApp)")
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(ls, key = { it.id }) { l ->
                ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(l.nome, style = MaterialTheme.typography.titleMedium)
                        Text("criada ${java.util.Date(l.dataCriacao)} • ${if (l.finalizada) "finalizada ${l.dataCompra}" else "aberta"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!l.finalizada) Button(onClick = { vm.finalizar(l.id) }) { Text("Finalizar") }
                            OutlinedButton(onClick = { vm.duplicar(l.id, true) }) { Text("Duplicar zerada") }
                            OutlinedButton(onClick = { vm.duplicar(l.id, false) }) { Text("Duplicar c/ preços") }
                        }
                    }
                }
            }
        }
    }
}
