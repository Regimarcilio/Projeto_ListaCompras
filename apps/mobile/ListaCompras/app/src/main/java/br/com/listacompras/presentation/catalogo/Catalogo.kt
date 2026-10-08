package br.com.listacompras.presentation.catalogo

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
import br.com.listacompras.data.local.dao.CatalogoDao
import br.com.listacompras.data.local.entity.CatalogoEntity
import br.com.listacompras.domain.model.TipoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogoViewModel @Inject constructor(private val dao: CatalogoDao) : ViewModel() {
    private val filtro = MutableStateFlow<TipoItem?>(null)
    val itens: StateFlow<List<CatalogoEntity>> = filtro.flatMapLatest { dao.observar(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun filtrar(t: TipoItem?) { filtro.value = t }
    fun salvar(nome: String, tipo: TipoItem, preco: Double?) = viewModelScope.launch {
        dao.upsert(CatalogoEntity(nome = nome.trim(), tipo = tipo, precoRef = preco))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(vm: CatalogoViewModel = hiltViewModel(), onAdicionar: (CatalogoEntity) -> Unit = {}) {
    val itens by vm.itens.collectAsState()
    var nome by remember { mutableStateOf("") }
    var precoTxt by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoItem.MERCEARIA) }
    var erro by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do item") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = precoTxt, onValueChange = { precoTxt = it }, label = { Text("Valor corrente (ex 5.99)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow { TipoItem.values().forEach { t ->
            FilterChip(selected = tipo == t, onClick = { tipo = t; vm.filtrar(t) }, label = { Text(t.name) }, modifier = Modifier.padding(end = 4.dp))
        } }
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            val preco = precoTxt.replace(",", ".").toDoubleOrNull()
            if (nome.isBlank()) erro = "Nome obrigatório"
            else if (preco != null && preco <= 0) erro = "Preço deve ser > 0"
            else { vm.salvar(nome, tipo, preco); nome = ""; precoTxt = ""; erro = null }
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Adicionar (vira catálogo reutilizável)") }
        LazyColumn { items(itens, key = { it.id }) { item ->
            ListItem(headlineContent = { Text(item.nome) }, supportingContent = { Text("${item.tipo} • R$ %.2f".format(item.precoRef ?: 0.0)) }, trailingContent = { Button(onClick = { onAdicionar(item) }) { Text("+ Lista") } })
            HorizontalDivider()
        } }
    }
}
