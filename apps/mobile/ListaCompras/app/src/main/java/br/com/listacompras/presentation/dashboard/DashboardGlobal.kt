package br.com.listacompras.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.domain.model.TotalPorTipo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class Periodo(val dias: Long?, val rotulo: String) {
    SETE_DIAS(7, "7D"), TRINTA_DIAS(30, "30D"), TUDO(null, "Tudo")
}

@HiltViewModel
class DashboardGlobalViewModel @Inject constructor(private val dao: ItemDao) : ViewModel() {
    private val periodo = MutableStateFlow(Periodo.TUDO)
    val totais: StateFlow<List<TotalPorTipo>> = periodo.flatMapLatest { p ->
        val desde = p.dias?.let { System.currentTimeMillis() - it * 24 * 60 * 60 * 1000L } ?: 0L
        dao.totalPorTipoGlobalDesde(desde).map { rows -> rows.map { TotalPorTipo(it.tipo, it.total, it.qtdItens, it.qtdSel) } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun periodo(p: Periodo) { periodo.value = p }
}

@Composable
fun DashboardGlobalScreen(vm: DashboardGlobalViewModel = hiltViewModel()) {
    val totais by vm.totais.collectAsState()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("BI global (finalizadas)", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Periodo.values().forEach { p ->
                FilterChip(selected = false, onClick = { vm.periodo(p) }, label = { Text(p.rotulo) })
            }
        }
        Spacer(Modifier.height(8.dp))
        if (totais.isEmpty()) {
            Text(
                "Nenhuma compra finalizada ainda — feche uma lista na aba Lista.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        totais.forEach { t -> Text("${t.tipo} — R$ %.2f (%d itens)".format(t.total, t.qtdItens)) }
        Text("TOTAL: R$ %.2f".format(totais.sumOf { it.total }), style = MaterialTheme.typography.titleMedium)
    }
}
