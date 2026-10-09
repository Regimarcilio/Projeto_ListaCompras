package br.com.listacompras.presentation.listaativa

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.model.TipoItem
import br.com.listacompras.domain.usecase.ListaOpsUseCase
import br.com.listacompras.share.parseListaTexto
import br.com.listacompras.sync.SyncRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class ListaUiState(val itens: List<ItemEntity> = emptyList(), val total: Double = 0.0)

data class ResumoFechado(
    val nomeLista: String,
    val data: String,
    val total: Double,
    val qtdItens: Int,
    val textoWhats: String,
    val novaId: String
)

/** #2: passo do stepper por unidade — un anda de 1 em 1; kg/g/L/ml de 0.5 em 0.5. */
fun passoPorUnidade(unidade: String): Double =
    if (unidade.trim().lowercase() == "un") 1.0 else 0.5

/** Formata quantidade sem casa decimal quando inteira ("2" em vez de "2.0"). */
fun formatarQtd(qtd: Double): String =
    if (qtd % 1.0 == 0.0) qtd.toInt().toString() else qtd.toString()

/** #4: validação do diálogo de edição (pura, testável). */
fun validarEdicao(nome: String, preco: Double?, qtd: Double?): String? {
    if (nome.isBlank()) return "Nome obrigatório"
    if (preco == null || preco <= 0) return "Preço deve ser > 0"
    if (qtd == null || qtd <= 0) return "Quantidade deve ser > 0"
    return null
}

/** #3: monta o texto do WhatsApp (pura, testável). #7: estabelecimento opcional no cabeçalho. */
fun montarTextoWhatsApp(
    nomeLista: String,
    data: String,
    itens: List<ItemEntity>,
    total: Double,
    estabelecimento: String? = null
): String {
    val sb = StringBuilder("🛒 $nomeLista ($data)\n")
    if (!estabelecimento.isNullOrBlank()) sb.append("🏪 ${estabelecimento.trim()}\n")
    itens.forEach { i ->
        val sub = (i.precoUnit ?: 0.0) * i.quantidade
        sb.append("• ${i.nome} — ${formatarQtd(i.quantidade)}${i.unidade} x R$ %.2f = R$ %.2f\n".format(i.precoUnit ?: 0.0, sub))
    }
    sb.append("TOTAL: R$ %.2f".format(total))
    return sb.toString()
}

/** #3: compartilha via WhatsApp com fallback p/ chooser geral (sem permissões novas). */
fun Context.compartilharWhatsApp(texto: String) {
    val viaWhats = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
        setPackage("com.whatsapp")
    }
    try {
        startActivity(viaWhats)
    } catch (_: ActivityNotFoundException) {
        val geral = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(geral, "Enviar via"))
    }
}

@HiltViewModel
class ListaAtivaViewModel @Inject constructor(
    private val dao: ItemDao,
    private val listas: ListaDao,
    private val ops: ListaOpsUseCase,
    private val sync: SyncRepo
) : ViewModel() {
    private val _listaId = MutableStateFlow("")
    val ui: StateFlow<ListaUiState> = _listaId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(ListaUiState())
        else combine(dao.observarDaLista(id), dao.acumuladoSelecionados(id)) { itens, total ->
            ListaUiState(itens, total)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListaUiState())

    /** #7: lista atual (nome + estabelecimento) para header e diálogo 1x por lista. */
    val lista: StateFlow<br.com.listacompras.data.local.entity.ListaEntity?> =
        _listaId.flatMapLatest { id ->
            if (id.isBlank()) flowOf(null) else listas.observarPorId(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun abrir(listaId: String) { _listaId.value = listaId }
    fun toggle(item: ItemEntity) = viewModelScope.launch { dao.setSelecionado(item.id, !item.selecionado) }
    /** #7: persiste o nome do estabelecimento. */
    fun salvarEstabelecimento(nome: String) = viewModelScope.launch {
        val id = _listaId.value
        val limpo = nome.trim()
        if (id.isNotBlank() && limpo.isNotBlank()) listas.setEstabelecimento(id, limpo)
    }
    /** Legado (delta fixo) — mantido p/ compat; a UI usa [ajustarQuantidade] com passo por unidade. */
    fun qtd(item: ItemEntity, delta: Double) = viewModelScope.launch {
        val nova = (item.quantidade + delta).coerceAtLeast(0.5)
        dao.atualizar(item.copy(quantidade = nova))
    }
    /** #2: stepper com passo por unidade e trava mínima 0.5. */
    fun ajustarQuantidade(item: ItemEntity, aumentar: Boolean) = viewModelScope.launch {
        val passo = passoPorUnidade(item.unidade)
        val nova = (item.quantidade + if (aumentar) passo else -passo).coerceAtLeast(0.5)
        dao.atualizar(item.copy(quantidade = nova))
    }
    fun remover(item: ItemEntity) = viewModelScope.launch { dao.remover(item.id) }
    /** #4: salva edição do diálogo via ItemDao.atualizar (total recalcula via Flow). */
    fun salvarEdicao(
        item: ItemEntity,
        nome: String,
        preco: Double,
        quantidade: Double,
        unidade: String,
        marca: String?
    ) = viewModelScope.launch {
        dao.atualizar(
            item.copy(
                nome = nome.trim(),
                precoUnit = preco,
                quantidade = quantidade,
                unidade = unidade,
                marca = marca?.trim()?.takeIf { it.isNotBlank() }
            )
        )
    }
    /** #3: finaliza via ListaOpsUseCase.finalizar, monta resumo e cria a nova lista ativa.
     *  #7: inclui estabelecimento no texto. #8: desmarca todos os itens da lista fechada
     *  (zera o badge) antes de criar a nova — sem apagar itens (histórico precisa deles). */
    suspend fun fechar(listaId: String): ResumoFechado {
        val itens = dao.listarDaLista(listaId)
        val lista = listas.porId(listaId)
        ops.finalizar(listaId)
        dao.desmarcarTodos(listaId)
        val data = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        val total = itens.sumOf { (it.precoUnit ?: 0.0) * it.quantidade }
        val texto = montarTextoWhatsApp(
            lista?.nome ?: "Lista de compras", data, itens, total,
            estabelecimento = lista?.estabelecimento
        )
        val novaId = ops.criar("Compra da semana")
        // #22: evento lista_fechada (nome/total/qtd) — enfileira + agenda sync.
        runCatching {
            sync.logListaFechada(
                lista?.nome ?: "Lista de compras",
                total,
                itens.size
            )
        }
        return ResumoFechado(
            nomeLista = lista?.nome ?: "Lista de compras",
            data = data,
            total = total,
            qtdItens = itens.size,
            textoWhats = texto,
            novaId = novaId
        )
    }

    /** #16: resultado da importação de texto (nova lista + contadores p/ Snackbar). */
    data class ResumoImportacao(val novaId: String, val qtd: Int, val ignoradas: Int)

    /** #16: importa texto no formato do share (WhatsApp) p/ nova lista ativa.
     *  Nome do header ou "Lista recebida dd/MM"; estabelecimento do header. */
    suspend fun importarTexto(texto: String): ResumoImportacao {
        val parsed = parseListaTexto(texto)
        val dataCurta = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM"))
        val nome = parsed.nome?.takeIf { it.isNotBlank() } ?: "Lista recebida $dataCurta"
        val novaId = UUID.randomUUID().toString()
        listas.criar(
            ListaEntity(
                id = novaId,
                nome = nome,
                estabelecimento = parsed.estabelecimento?.takeIf { it.isNotBlank() }
            )
        )
        parsed.itens.forEachIndexed { idx, item ->
            dao.adicionar(
                ItemEntity(
                    listaId = novaId,
                    nome = item.nome,
                    tipo = TipoItem.OUTROS,
                    unidade = item.unidade,
                    quantidade = item.qtd,
                    precoUnit = item.precoUnit,
                    ordem = idx
                )
            )
        }
        return ResumoImportacao(novaId, parsed.itens.size, parsed.ignoradas)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListaAtivaScreen(
    listaId: String,
    vm: ListaAtivaViewModel = hiltViewModel(),
    onListaFechada: (String) -> Unit = {},
    // #16: texto recebido via share do sistema (ACTION_SEND) — abre o diálogo pré-preenchido.
    textoCompartilhado: String? = null,
    onTextoCompartilhadoConsumido: () -> Unit = {}
) {
    LaunchedEffect(listaId) { vm.abrir(listaId) }
    val st by vm.ui.collectAsState()
    val listaAtual by vm.lista.collectAsState()
    val estabelecimento = listaAtual?.estabelecimento
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var resumo by remember { mutableStateOf<ResumoFechado?>(null) }
    var editando by remember { mutableStateOf<ItemEntity?>(null) }
    var fechando by remember { mutableStateOf(false) }
    // #16: diálogo Importar texto (colar ou share recebido).
    var mostrarImportar by remember { mutableStateOf(false) }
    var textoImportar by remember { mutableStateOf("") }
    // Share do sistema: confirma via diálogo pré-preenchido, nunca importa silencioso.
    LaunchedEffect(textoCompartilhado) {
        if (!textoCompartilhado.isNullOrBlank()) {
            textoImportar = textoCompartilhado
            mostrarImportar = true
            onTextoCompartilhadoConsumido()
        }
    }
    // #7: pergunta do estabelecimento — 1 vez por lista (reset ao trocar de lista).
    var mostrarEstab by remember(listaId) { mutableStateOf(false) }
    var perguntouEstab by remember(listaId) { mutableStateOf(false) }
    val vazia = st.itens.isEmpty()

    /** Marca/desmarca e, se era o 1º selecionado e sem estabelecimento, abre o diálogo 1x. */
    fun aoAlternar(item: ItemEntity) {
        val nSelAntes = st.itens.count { it.selecionado }
        vm.toggle(item)
        if (!item.selecionado && nSelAntes == 0 && estabelecimento.isNullOrBlank() && !perguntouEstab) {
            perguntouEstab = true
            mostrarEstab = true
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
    Column(Modifier.padding(pad)) {
        // Header fixo com total acumulado (RF-004) — padrão web: total grande verde + pill
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // #7: nome do estabelecimento no header da Lista.
                if (!estabelecimento.isNullOrBlank()) {
                    Text(
                        "🏪 ${estabelecimento!!.trim()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            fechando = true
                            scope.launch {
                                try {
                                    resumo = vm.fechar(listaId)
                                    onListaFechada(resumo!!.novaId)
                                } finally {
                                    fechando = false
                                }
                            }
                        },
                        enabled = !vazia && !fechando,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (fechando) "Fechando…" else "Fechar lista", maxLines = 1) }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val itens = st.itens
                                val data = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                ctx.compartilharWhatsApp(
                                    montarTextoWhatsApp(
                                        listaAtual?.nome ?: "Lista de compras",
                                        data, itens, st.total,
                                        estabelecimento = estabelecimento
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Enviar via WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    // #16: ponto de entrada do Importar texto (colar ou share recebido).
                    IconButton(
                        onClick = { mostrarImportar = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) { Icon(Icons.Filled.Download, contentDescription = "Importar texto") }
                }
                if (vazia) Text(
                    "Lista vazia — adicione itens pelo Catálogo para fechar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LazyColumn {
            items(st.itens, key = { it.id }) { item ->
                val verde = item.selecionado
                ListItem(
                    headlineContent = { Text(item.nome, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                buildString {
                                    append("${item.tipo} • R$ %.2f".format(item.precoUnit ?: 0.0))
                                    item.marca?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall
                            )
                            // #2: stepper em linha única — botões ≥48dp, sem sobrepor nome/preço em 360dp
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { vm.ajustarQuantidade(item, false) },
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text("−") }
                                Text(
                                    "${formatarQtd(item.quantidade)}${item.unidade}",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                OutlinedButton(
                                    onClick = { vm.ajustarQuantidade(item, true) },
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text("+") }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "R$ %.2f".format((item.precoUnit ?: 0.0) * item.quantidade),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { editando = item }) { Icon(Icons.Filled.Edit, contentDescription = "Editar item") }
                            IconButton(onClick = { vm.remover(item) }) { Icon(Icons.Filled.Delete, contentDescription = "Remover item") }
                            Checkbox(checked = verde, onCheckedChange = { aoAlternar(item) })
                        }
                    },
                    colors = if (verde) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.combinedClickable(
                        onClick = { aoAlternar(item) },
                        onLongClick = { editando = item },
                        // #12 TalkBack: só rótulos, sem mudar funcionamento.
                        onClickLabel = if (verde) "Desmarcar ${item.nome}" else "Marcar ${item.nome}",
                        onLongClickLabel = "Editar ${item.nome}"
                    )
                )
                HorizontalDivider()
            }
        }
        }
    }

    // #4: diálogo de edição por item
    editando?.let { item ->
        EditarItemDialog(
            item = item,
            onDismiss = { editando = null },
            onSalvar = { nome, preco, qtd, unidade, marca ->
                vm.salvarEdicao(item, nome, preco, qtd, unidade, marca)
                editando = null
            }
        )
    }

    // #7: pergunta 1x por lista; cancelar (X) mantém a marcação sem salvar.
    if (mostrarEstab) {
        EstabelecimentoDialog(
            onConfirmar = { nome ->
                vm.salvarEstabelecimento(nome)
                mostrarEstab = false
            },
            onCancelar = { mostrarEstab = false }
        )
    }

    // #3: resumo pós-fechamento
    resumo?.let { r ->
        AlertDialog(
            onDismissRequest = { resumo = null },
            title = { Text("Lista fechada ✅") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.nomeLista, style = MaterialTheme.typography.titleMedium)
                    Text("Data: ${r.data}")
                    Text("Itens: ${r.qtdItens}")
                    Text("Total: R$ %.2f".format(r.total), fontWeight = FontWeight.Bold)
                    Text("Nova lista ativa criada.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { ctx.compartilharWhatsApp(r.textoWhats) }) { Text("Enviar via WhatsApp") }
            },
            dismissButton = {
                TextButton(onClick = { resumo = null }) { Text("OK") }
            }
        )
    }

    // #16: diálogo Importar texto — cria nova lista e troca o ativo via onListaFechada.
    if (mostrarImportar) {
        ImportarTextoDialog(
            textoInicial = textoImportar,
            onDismiss = { mostrarImportar = false },
            onConfirmar = { texto ->
                scope.launch {
                    val r = vm.importarTexto(texto)
                    mostrarImportar = false
                    textoImportar = ""
                    if (r.qtd == 0) {
                        snackbar.showSnackbar("Nenhum item válido encontrado no texto")
                    } else {
                        onListaFechada(r.novaId)
                        val msg = if (r.ignoradas > 0) "${r.qtd} itens importados ✅ (${r.ignoradas} ignoradas)"
                        else "${r.qtd} itens importados ✅"
                        snackbar.showSnackbar(msg)
                    }
                }
            }
        )
    }
}

@Composable
private fun ImportarTextoDialog(
    textoInicial: String,
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var texto by remember(textoInicial) { mutableStateOf(textoInicial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar texto") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Cole o texto recebido via WhatsApp/SMS no formato do app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    placeholder = { Text("🛒 Minha lista (…)\n• …") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(texto) },
                enabled = texto.isNotBlank()
            ) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EstabelecimentoDialog(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Deseja adicionar o nome do estabelecimento?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCancelar) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar sem salvar")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Estabelecimento") },
                    placeholder = { Text("Ex.: Atacadão") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nome) },
                enabled = nome.isNotBlank()
            ) { Text("Confirmar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarItemDialog(
    item: ItemEntity,
    onDismiss: () -> Unit,
    onSalvar: (nome: String, preco: Double, qtd: Double, unidade: String, marca: String?) -> Unit
) {
    var nome by remember(item.id) { mutableStateOf(item.nome) }
    var precoTxt by remember(item.id) { mutableStateOf(item.precoUnit?.toString() ?: "") }
    var qtdTxt by remember(item.id) { mutableStateOf(formatarQtd(item.quantidade)) }
    var unidade by remember(item.id) { mutableStateOf(item.unidade) }
    var marca by remember(item.id) { mutableStateOf(item.marca ?: "") }
    var erro by remember(item.id) { mutableStateOf<String?>(null) }
    var expandUn by remember { mutableStateOf(false) }
    val unidades = listOf("un", "kg", "g", "L", "ml")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar item", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = precoTxt,
                    onValueChange = { precoTxt = it },
                    label = { Text("Preço unitário") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = qtdTxt,
                        onValueChange = { qtdTxt = it },
                        label = { Text("Quantidade") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandUn,
                        onExpandedChange = { expandUn = !expandUn },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unidade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidade") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandUn) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandUn, onDismissRequest = { expandUn = false }) {
                            unidades.forEach { u ->
                                DropdownMenuItem(text = { Text(u) }, onClick = { unidade = u; expandUn = false })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val preco = precoTxt.replace(",", ".").toDoubleOrNull()
                val qtd = qtdTxt.replace(",", ".").toDoubleOrNull()
                val msg = validarEdicao(nome, preco, qtd)
                if (msg != null) erro = msg
                else onSalvar(nome, preco!!, qtd!!, unidade, marca)
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
