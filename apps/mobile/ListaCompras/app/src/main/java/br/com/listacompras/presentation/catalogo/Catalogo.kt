package br.com.listacompras.presentation.catalogo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    fun salvar(nome: String, tipo: TipoItem, preco: Double?, ean: String?) = viewModelScope.launch {
        dao.upsert(CatalogoEntity(nome = nome.trim(), tipo = tipo, precoRef = preco, codigoBarras = ean?.takeIf { it.isNotBlank() }))
    }
    suspend fun buscarEan(ean: String) = dao.porEan(ean.trim())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(vm: CatalogoViewModel = hiltViewModel(), onAdicionar: (CatalogoEntity) -> Unit = {}) {
    val itens by vm.itens.collectAsState()
    var nome by remember { mutableStateOf("") }
    var precoTxt by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoItem.MERCEARIA) }
    var ean by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // FIX print 08/10: Column + weight(1f) espremia o grid e cortava os cards
    // ("CARNE/LIMPEZA" decapitados). Agora tela inteira rola num único
    // LazyVerticalGrid com header full-span + filtro em LazyRow 1 linha.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do item", maxLines = 1, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = precoTxt, onValueChange = { precoTxt = it }, label = { Text("Valor corrente (ex 5.99)", maxLines = 1, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = ean, onValueChange = { ean = it }, label = { Text("Código de barras (opcional)", maxLines = 1, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val achou = ean.takeIf { it.isNotBlank() }?.let { vm.buscarEan(it) }
                            erro = if (achou != null) "EAN encontrado: ${achou.nome} ✅" else "EAN não cadastrado — preencha e adicione"
                        }
                    }) { Text("Buscar EAN", maxLines = 1) }
                }
                Spacer(Modifier.height(8.dp))
                // Filtro: era FlowRow com 10 chips em 3-4 linhas (~140dp). Vira LazyRow 1 linha com scroll.
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TipoItem.values()) { t ->
                        FilterChip(selected = tipo == t, onClick = { tipo = t; vm.filtrar(t) }, label = { Text(t.name, maxLines = 1) })
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    val preco = precoTxt.replace(",", ".").toDoubleOrNull()
                    if (nome.isBlank()) erro = "Nome obrigatório"
                    else if (preco != null && preco <= 0) erro = "Preço deve ser > 0"
                    else { vm.salvar(nome, tipo, preco, ean); nome = ""; precoTxt = ""; ean = ""; erro = null }
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Adicionar (vira catálogo reutilizável)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        // Grid 2 colunas — mesmo padrão da prévia web
        items(itens, key = { it.id }) { item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.nome, style = MaterialTheme.typography.titleMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${item.tipo}${item.codigoBarras?.let { " • EAN" } ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "R$ %.2f".format(item.precoRef ?: 0.0),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onAdicionar(item) }, modifier = Modifier.fillMaxWidth()) { Text("+ Lista", maxLines = 1) }
                }
            }
        }
    }
}
