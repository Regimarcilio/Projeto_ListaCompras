package br.com.listacompras.domain.usecase

import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.domain.model.TipoItem
import br.com.listacompras.domain.model.TotalPorTipo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ListaUiTotal(val total: Double, val qtdSelecionados: Int, val qtdTotal: Int)

class ToggleSelecaoUseCase @Inject constructor(private val dao: ItemDao) {
    suspend fun invoke(id: String, checked: Boolean) = dao.setSelecionado(id, checked)
}

class GetTotalPorTipoUseCase @Inject constructor(private val dao: ItemDao) {
    fun invoke(listaId: String): Flow<List<TotalPorTipo>> =
        dao.totalPorTipo(listaId).let { flow ->
            kotlinx.coroutines.flow.map(flow) { rows -> rows.map { TotalPorTipo(it.tipo, it.total, it.qtdItens, it.qtdSel) } }
        }
}

class ObserveAcumuladoUseCase @Inject constructor(private val dao: ItemDao) {
    fun invoke(listaId: String): Flow<Double> = dao.acumuladoSelecionados(listaId)
}

fun validarItem(nome: String, preco: Double?, qtd: Double, tipo: TipoItem?): String? {
    if (nome.isBlank()) return "Nome obrigatório"
    if (preco != null && preco <= 0) return "Preço deve ser > 0"
    if (qtd <= 0) return "Quantidade deve ser > 0"
    if (tipo == null) return "Tipo obrigatório"
    return null
}
