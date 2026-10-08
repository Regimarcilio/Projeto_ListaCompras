package br.com.listacompras.domain.usecase

import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.share.ExportShareHelper
import br.com.listacompras.share.JsonCodec
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class ListaOpsUseCase @Inject constructor(
    private val listas: ListaDao,
    private val itens: ItemDao,
    private val share: ExportShareHelper
) {
    suspend fun criar(nome: String): String {
        val id = UUID.randomUUID().toString()
        listas.criar(ListaEntity(id = id, nome = nome.ifBlank { "Compra ${OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM"))}" }))
        return id
    }

    suspend fun finalizar(listaId: String) {
        listas.finalizar(listaId, OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    /** Duplica itens para nova lista; zerarPrecos=true para próxima compra. */
    suspend fun duplicar(origemId: String, zerarPrecos: Boolean = true): String {
        val origem = listas.porId(origemId) ?: error("lista inexistente")
        val novaId = UUID.randomUUID().toString()
        listas.criar(ListaEntity(id = novaId, nome = origem.nome + " (cópia)"))
        itens.listarDaLista(origemId).forEachIndexed { idx, i ->
            itens.adicionar(i.copy(id = UUID.randomUUID().toString(), listaId = novaId, precoUnit = if (zerarPrecos) null else i.precoUnit, selecionado = false, ordem = idx))
        }
        return novaId
    }

    suspend fun exportarJson(exportedAt: String = share.agoraIso()): Pair<String, String> {
        // retorna (textoJson, resumoErros="")
        val todas = mutableListOf<Pair<ListaEntity, List<ItemEntity>>>()
        // coleta síncrona via listarDaLista para cada lista (primeira emissão)
        // chamador coleta observarTodas().first() e chama este método com dados — versão simples:
        return "" to ""
    }

    fun serializar(todas: List<Pair<ListaEntity, List<ItemEntity>>>): String =
        JsonCodec.export(todas, share.agoraIso())
}
