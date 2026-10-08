package br.com.listacompras.presentation.historico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.ItemEntity
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

    /** Legado: mantido p/ não quebrar chamadas de ListaOpsUseCase (UI #9 não usa mais). */
    fun finalizar(id: String) = viewModelScope.launch { ops.finalizar(id); msg = "Lista finalizada com data ✅" }
    /** Legado: mantido p/ não quebrar chamadas de ListaOpsUseCase (UI #9 não usa mais). */
    fun duplicar(id: String, zerar: Boolean = true) = viewModelScope.launch {
        val nova = ops.duplicar(id, zerar); msg = "Duplicada: $nova"
    }
    suspend fun itensDaLista(id: String): List<ItemEntity> = itens.listarDaLista(id)
    /** #9: apaga itens da lista + a lista. */
    fun excluir(id: String) = viewModelScope.launch {
        itens.removerDaLista(id)
        listas.excluirLista(id)
        msg = "Registro excluído"
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
    var verLista by remember { mutableStateOf<ListaEntity?>(null) }
    var excluirAlvo by remember { mutableStateOf<ListaEntity?>(null) }
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
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(l.nome, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!l.estabelecimento.isNullOrBlank()) {
                            Text(
                                "🏪 ${l.estabelecimento!!.trim()}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("criada ${java.util.Date(l.dataCriacao)} • ${if (l.finalizada) "finalizada ${l.dataCompra}" else "aberta"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { verLista = l }) { Text("Ver lista") }
                            OutlinedButton(onClick = { excluirAlvo = l }) { Text("Excluir") }
                        }
                    }
                }
            }
        }
    }

    // #9: diálogo somente-leitura (nome, qtd+un, preço unit, subtotal + total geral).
    verLista?.let { l ->
        VerListaDialog(lista = l, onFechar = { verLista = null }, carregarItens = { vm.itensDaLista(it) })
    }

    // #9: confirmação de exclusão.
    excluirAlvo?.let { l ->
        AlertDialog(
            onDismissRequest = { excluirAlvo = null },
            title = { Text("Excluir registro?") },
            text = { Text("Apagar \"${l.nome}\" e seus itens do histórico? Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { vm.excluir(l.id); excluirAlvo = null }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { excluirAlvo = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun VerListaDialog(
    lista: ListaEntity,
    onFechar: () -> Unit,
    carregarItens: suspend (String) -> List<ItemEntity>
) {
    var itens by remember(lista.id) { mutableStateOf<List<ItemEntity>?>(null) }
    LaunchedEffect(lista.id) { itens = carregarItens(lista.id) }
    val total = (itens ?: emptyList()).sumOf { (it.precoUnit ?: 0.0) * it.quantidade }
    AlertDialog(
        onDismissRequest = onFechar,
        title = {
            Column {
                Text(lista.nome, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!lista.estabelecimento.isNullOrBlank()) {
                    Text(
                        "🏪 ${lista.estabelecimento!!.trim()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        text = {
            val listaItens = itens
            if (listaItens == null) {
                Text("Carregando…")
            } else if (listaItens.isEmpty()) {
                Text("Nenhum item nesta lista.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(listaItens, key = { it.id }) { i ->
                        val sub = (i.precoUnit ?: 0.0) * i.quantidade
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(i.nome, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "${formatarQtdVer(i.quantidade)}${i.unidade} • R$ %.2f un. • Subtotal R$ %.2f".format(i.precoUnit ?: 0.0, sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Total: R$ %.2f".format(total), fontWeight = FontWeight.Bold)
                TextButton(onClick = onFechar) { Text("Fechar") }
            }
        }
    )
}

private fun formatarQtdVer(qtd: Double): String =
    if (qtd % 1.0 == 0.0) qtd.toInt().toString() else qtd.toString()
