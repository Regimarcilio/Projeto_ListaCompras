package br.com.listacompras.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.domain.model.TotalPorTipo
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
    Column(Modifier.padding(16.dp)) {
        Text("BI por tipo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        totais.forEach { t ->
            Text("${t.tipo} — R$ %.2f (%d itens)".format(t.total, t.qtdItens))
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                drawRect(color = androidx.compose.ui.graphics.Color(0xFF2E7D32), size = Size(size.width * (t.total / max).toFloat(), size.height))
            }
            Spacer(Modifier.height(8.dp))
        }
        val geral = totais.sumOf { it.total }
        Text("TOTAL GERAL: R$ %.2f".format(geral), style = MaterialTheme.typography.titleMedium)
    }
}
