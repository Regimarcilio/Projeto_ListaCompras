package br.com.listacompras.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.domain.model.TotalPorTipo
import br.com.listacompras.domain.usecase.dividirConta
import br.com.listacompras.presentation.theme.Green700
import br.com.listacompras.presentation.theme.Green900
import br.com.listacompras.share.ExportShareHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(dao: ItemDao) : ViewModel() {
    private val listaId = MutableStateFlow("")
    val totais: StateFlow<List<TotalPorTipo>> = listaId.flatMapLatest {
        if (it.isBlank()) flowOf(emptyList())
        else dao.totalPorTipo(it).map { rows -> rows.map { r -> TotalPorTipo(r.tipo, r.total, r.qtdItens, r.qtdSel) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun abrir(id: String) { listaId.value = id }
}

@Composable
fun DashboardScreen(listaId: String, vm: DashboardViewModel = hiltViewModel()) {
    LaunchedEffect(listaId) { vm.abrir(listaId) }
    val totais by vm.totais.collectAsState()
    val max = (totais.maxOfOrNull { it.total } ?: 1.0)
    val geral = totais.sumOf { it.total }
    Column(Modifier.padding(16.dp)) {
        Text("BI por tipo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        totais.forEach { t ->
            val pct = if (geral > 0) (t.total / geral * 100).toInt() else 0
            Text("${t.tipo}", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("R$ %.2f (%d itens)".format(t.total, t.qtdItens), style = MaterialTheme.typography.bodyMedium)
                Text("$pct%", style = MaterialTheme.typography.bodyMedium)
            }
            // Barra arredondada com gradiente — mesmo padrão da prévia web
            Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                drawRoundRect(color = Color(0xFFE0E0E0), cornerRadius = CornerRadius(999f, 999f))
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Green700, Green900)),
                    size = Size(size.width * (t.total / max).toFloat(), size.height),
                    cornerRadius = CornerRadius(999f, 999f)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        Text("TOTAL GERAL: R$ %.2f".format(geral), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        // Dividir conta (Sprint4)
        var pessoasTxt by remember { mutableStateOf("2") }
        val pessoas = pessoasTxt.toIntOrNull()?.coerceIn(1, 20) ?: 2
        OutlinedTextField(value = pessoasTxt, onValueChange = { pessoasTxt = it.filter(Char::isDigit).take(2) }, label = { Text("Dividir entre N pessoas") }, modifier = Modifier.fillMaxWidth())
        dividirConta(geral, pessoas).forEachIndexed { i, v ->
            Text("Pessoa ${i + 1}: R$ %.2f".format(v))
        }
    }
}
