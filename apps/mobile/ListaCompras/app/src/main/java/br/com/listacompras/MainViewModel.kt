package br.com.listacompras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.sync.SyncRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Badge do carrinho no bottom nav: total de itens selecionados (todas as listas). */
@HiltViewModel
class MainViewModel @Inject constructor(
    dao: ItemDao,
    private val listas: ListaDao,
    private val sync: SyncRepo
) : ViewModel() {
    val badge: StateFlow<Int> = dao.contarSelecionados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Garante que a lista ativa exista (idempotente): cria "demo-lista-01" se ausente. */
    fun garantirLista(id: String, nome: String = "Compra da semana") = viewModelScope.launch {
        if (listas.porId(id) == null) listas.criar(ListaEntity(id = id, nome = nome))
    }

    /** #22: evento app_aberto (1x por abertura — chamado do LaunchedEffect da MainActivity). */
    fun logAppAberto() = viewModelScope.launch { sync.logAppAberto() }
}
