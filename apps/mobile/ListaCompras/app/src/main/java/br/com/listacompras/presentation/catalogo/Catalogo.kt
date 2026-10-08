package br.com.listacompras.presentation.catalogo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.CatalogoDao
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.CatalogoEntity
import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.model.TipoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val UNIDADES_CATALOGO = listOf("un", "kg", "g", "L", "ml")

@HiltViewModel
class CatalogoViewModel @Inject constructor(
    private val dao: CatalogoDao,
    private val itemDao: ItemDao,
    private val listas: ListaDao
) : ViewModel() {
    private val filtro = MutableStateFlow<TipoItem?>(null)
    val itens: StateFlow<List<CatalogoEntity>> = filtro.flatMapLatest { dao.observar(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun filtrar(t: TipoItem?) { filtro.value = t }
    fun salvar(
        nome: String,
        tipo: TipoItem,
        preco: Double?,
        unidade: String,
        quantidade: Double,
        marca: String?
    ) = viewModelScope.launch {
        dao.upsert(
            CatalogoEntity(
                nome = nome.trim(),
                tipo = tipo,
                precoRef = preco,
                unidadeDefault = unidade,
                quantidadeDefault = quantidade,
                marca = marca?.trim()?.takeIf { it.isNotBlank() }
            )
        )
    }
    // Mantido p/ compatibilidade (DAO porEan intacto); formulário novo não usa EAN.
    suspend fun buscarEan(ean: String) = dao.porEan(ean.trim())
    /** P0: "+ Lista" insere ItemEntity na lista ativa levando unidade+marca do catálogo. */
    suspend fun adicionarNaLista(listaId: String, item: CatalogoEntity): String {
        if (listas.porId(listaId) == null) listas.criar(ListaEntity(id = listaId, nome = "Compra da semana"))
        val ordem = itemDao.listarDaLista(listaId).size
        itemDao.adicionar(
            ItemEntity(
                listaId = listaId,
                catalogoItemId = item.id,
                nome = item.nome,
                tipo = item.tipo,
                unidade = item.unidadeDefault,
                precoUnit = item.precoRef,
                quantidade = item.quantidadeDefault,
                marca = item.marca,
                ordem = ordem
            )
        )
        return item.nome
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    listaId: String = "demo-lista-01",
    vm: CatalogoViewModel = hiltViewModel(),
    onAdicionar: (CatalogoEntity) -> Unit = {}
) {
    val itens by vm.itens.collectAsState()
    var nome by remember { mutableStateOf("") }
    var precoTxt by remember { mutableStateOf("") }
    // Filtro do grid (null = Todas) — NÃO afeta o salvamento.
    var filtroTipo by remember { mutableStateOf<TipoItem?>(null) }
    // Campos do novo item: categoria default MERCEARIA, qtd numérica + unidade dropdown.
    var novoTipo by remember { mutableStateOf(TipoItem.MERCEARIA) }
    var qtdTxt by remember { mutableStateOf("1") }
    var unidade by remember { mutableStateOf("un") }
    var marca by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var expandCat by remember { mutableStateOf(false) }
    var expandUn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
    // FIX print 08/10: Column + weight(1f) espremia o grid e cortava os cards
    // ("CARNE/LIMPEZA" decapitados). Agora tela inteira rola num único
    // LazyVerticalGrid com header full-span + filtro em LazyRow 1 linha.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(pad)
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                // 1. Nome do item (obrigatório)
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do item", maxLines = 1, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                // 2. Valor corrente (>0)
                OutlinedTextField(
                    value = precoTxt,
                    onValueChange = { precoTxt = it },
                    label = { Text("Valor corrente (ex 5.99)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                // 3. Categoria em dropdown (10 TipoItem, default MERCEARIA)
                ExposedDropdownMenuBox(expanded = expandCat, onExpandedChange = { expandCat = !expandCat }) {
                    OutlinedTextField(
                        value = novoTipo.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria", maxLines = 1) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandCat) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expandCat, onDismissRequest = { expandCat = false }) {
                        TipoItem.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = { novoTipo = t; expandCat = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 4. Quantidade (numérico) + Unidade em dropdown
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = qtdTxt,
                        onValueChange = { qtdTxt = it },
                        label = { Text("Quantidade", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    ExposedDropdownMenuBox(expanded = expandUn, onExpandedChange = { expandUn = !expandUn }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = unidade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidade", maxLines = 1) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandUn) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandUn, onDismissRequest = { expandUn = false }) {
                            UNIDADES_CATALOGO.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = { unidade = u; expandUn = false }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 5. Marca (texto opcional)
                OutlinedTextField(value = marca, onValueChange = { marca = it }, label = { Text("Marca (opcional)", maxLines = 1, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                // Filtro do grid: era FlowRow com 10 chips em 3-4 linhas (~140dp). Vira LazyRow 1 linha com scroll.
                // "Todas" = sem filtro (null). Só filtra o grid — não afeta o salvamento.
                Text("Filtrar catálogo:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filtroTipo == null,
                            onClick = { filtroTipo = null; vm.filtrar(null) },
                            label = { Text("Todas", maxLines = 1) }
                        )
                    }
                    items(TipoItem.values()) { t ->
                        FilterChip(selected = filtroTipo == t, onClick = { filtroTipo = t; vm.filtrar(t) }, label = { Text(t.name, maxLines = 1) })
                    }
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    val preco = precoTxt.replace(",", ".").toDoubleOrNull()
                    val qtd = qtdTxt.replace(",", ".").toDoubleOrNull()
                    if (nome.isBlank()) erro = "Nome obrigatório"
                    else if (preco == null || preco <= 0) erro = "Valor deve ser > 0"
                    else if (qtd == null || qtd <= 0) erro = "Quantidade deve ser > 0"
                    else {
                        vm.salvar(nome, novoTipo, preco, unidade, qtd, marca)
                        nome = ""; precoTxt = ""; qtdTxt = "1"; unidade = "un"; marca = ""
                        novoTipo = TipoItem.MERCEARIA; erro = null
                    }
                }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Adicionar (vira catálogo reutilizável)", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        // Grid 2 colunas — mesmo padrão da prévia web
        items(itens, key = { it.id }) { item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(item.nome, style = MaterialTheme.typography.titleMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        buildString {
                            append(item.tipo.name)
                            item.marca?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                            append(" • ${item.quantidadeDefaultFmt()} ${item.unidadeDefault}")
                        },
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
                    Button(onClick = {
                        scope.launch {
                            val nomeAdd = vm.adicionarNaLista(listaId, item)
                            snackbar.showSnackbar("$nomeAdd na lista ✅")
                            onAdicionar(item)
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("+ Lista", maxLines = 1) }
                }
            }
        }
    }
    }
}

/** Formata quantidadeDefault sem casa decimal quando inteira ("1" em vez de "1.0"). */
fun CatalogoEntity.quantidadeDefaultFmt(): String =
    if (quantidadeDefault % 1.0 == 0.0) quantidadeDefault.toInt().toString()
    else quantidadeDefault.toString()
