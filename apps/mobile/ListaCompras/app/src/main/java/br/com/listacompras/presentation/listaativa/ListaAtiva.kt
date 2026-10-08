package br.com.listacompras.presentation.listaativa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.entity.ItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListaUiState(val itens: List<ItemEntity> = emptyList(), val total: Double = 0.0)

@HiltViewModel
class ListaAtivaViewModel @Inject constructor(private val dao: ItemDao) : ViewModel() {
    private val _listaId = MutableStateFlow("")
    val ui: StateFlow<ListaUiState> = _listaId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(ListaUiState())
        else combine(dao.observarDaLista(id), dao.acumuladoSelecionados(id)) { itens, total ->
            ListaUiState(itens, total)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListaUiState())

    fun abrir(listaId: String) { _listaId.value = listaId }
    fun toggle(item: ItemEntity) = viewModelScope.launch { dao.setSelecionado(item.id, !item.selecionado) }
    fun qtd(item: ItemEntity, delta: Double) = viewModelScope.launch {
        val nova = (item.quantidade + delta).coerceAtLeast(0.5)
        dao.atualizar(item.copy(quantidade = nova))
    }
    fun remover(item: ItemEntity) = viewModelScope.launch { dao.remover(item.id) }
}

@Composable
fun ListaAtivaScreen(listaId: String, vm: ListaAtivaViewModel = hiltViewModel()) {
    LaunchedEffect(listaId) { vm.abrir(listaId) }
    val st by vm.ui.collectAsState()
    Column {
        // Header fixo com total acumulado (RF-004) — padrão web: total grande verde + pill
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total no carrinho", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "R$ %.2f".format(st.total),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                AssistChip(onClick = {}, label = { Text("%d %s".format(st.itens.count { it.selecionado }, if (st.itens.count { it.selecionado } == 1) "item" else "itens")) })
            }
        }
        LazyColumn {
            items(st.itens, key = { it.id }) { item ->
                val verde = item.selecionado
                ListItem(
                    headlineContent = { Text(item.nome) },
                    supportingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${item.tipo} • R$ %.2f".format(item.precoUnit ?: 0.0))
                            // Stepper quantidade (Sprint4)
                            OutlinedButton(onClick = { vm.qtd(item, -0.5) }, contentPadding = PaddingValues(4.dp)) { Text("−") }
                            Text("%.1f".format(item.quantidade))
                            OutlinedButton(onClick = { vm.qtd(item, 0.5) }, contentPadding = PaddingValues(4.dp)) { Text("+") }
                        }
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { vm.remover(item) }) { Text("✕") }
                            Checkbox(checked = verde, onCheckedChange = { vm.toggle(item) })
                        }
                    },
                    colors = if (verde) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { vm.toggle(item) }
                )
                HorizontalDivider()
            }
        }
    }
}
