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
import androidx.compose.material.icons.filled.Delete
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
import br.com.listacompras.domain.usecase.ListaOpsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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

/** #3: monta o texto do WhatsApp (pura, testável). */
fun montarTextoWhatsApp(
    nomeLista: String,
    data: String,
    itens: List<ItemEntity>,
    total: Double
): String {
    val sb = StringBuilder("🛒 $nomeLista ($data)\n")
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
    private val ops: ListaOpsUseCase
) : ViewModel() {
    private val _listaId = MutableStateFlow("")
    val ui: StateFlow<ListaUiState> = _listaId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(ListaUiState())
        else combine(dao.observarDaLista(id), dao.acumuladoSelecionados(id)) { itens, total ->
            ListaUiState(itens, total)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListaUiState())

    fun abrir(listaId: String) { _listaId.value = listaId }
    fun toggle(item: ItemEntity) = viewModelScope.launch { dao.setSelecionado(item.id, !item.selecionado) }
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
    /** #3: finaliza via ListaOpsUseCase.finalizar, monta resumo e cria a nova lista ativa. */
    suspend fun fechar(listaId: String): ResumoFechado {
        val itens = dao.listarDaLista(listaId)
        val lista = listas.porId(listaId)
        ops.finalizar(listaId)
        val data = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        val total = itens.sumOf { (it.precoUnit ?: 0.0) * it.quantidade }
        val texto = montarTextoWhatsApp(lista?.nome ?: "Lista de compras", data, itens, total)
        val novaId = ops.criar("Compra da semana")
        return ResumoFechado(
            nomeLista = lista?.nome ?: "Lista de compras",
            data = data,
            total = total,
            qtdItens = itens.size,
            textoWhats = texto,
            novaId = novaId
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListaAtivaScreen(
    listaId: String,
    vm: ListaAtivaViewModel = hiltViewModel(),
    onListaFechada: (String) -> Unit = {}
) {
    LaunchedEffect(listaId) { vm.abrir(listaId) }
    val st by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var resumo by remember { mutableStateOf<ResumoFechado?>(null) }
    var editando by remember { mutableStateOf<ItemEntity?>(null) }
    var fechando by remember { mutableStateOf(false) }
    val vazia = st.itens.isEmpty()

    Column {
        // Header fixo com total acumulado (RF-004) — padrão web: total grande verde + pill
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                                ctx.compartilharWhatsApp(montarTextoWhatsApp("Lista de compras", data, itens, st.total))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Enviar via WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
                            Checkbox(checked = verde, onCheckedChange = { vm.toggle(item) })
                        }
                    },
                    colors = if (verde) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.combinedClickable(
                        onClick = { vm.toggle(item) },
                        onLongClick = { editando = item }
                    )
                )
                HorizontalDivider()
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
