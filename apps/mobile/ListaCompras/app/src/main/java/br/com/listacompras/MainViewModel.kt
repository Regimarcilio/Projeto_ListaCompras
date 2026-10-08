package br.com.listacompras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.dao.ItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Badge do carrinho no bottom nav: total de itens selecionados (todas as listas). */
@HiltViewModel
class MainViewModel @Inject constructor(dao: ItemDao) : ViewModel() {
    val badge: StateFlow<Int> = dao.contarSelecionados()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
