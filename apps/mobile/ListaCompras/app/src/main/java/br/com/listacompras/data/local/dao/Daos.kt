package br.com.listacompras.data.local.dao

import androidx.room.*
import br.com.listacompras.data.local.entity.CatalogoEntity
import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.model.TipoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: CatalogoEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun seedAll(items: List<CatalogoEntity>)
    @Query("SELECT * FROM catalogo_item WHERE ativo=1 AND (:tipo IS NULL OR tipo=:tipo) ORDER BY nome")
    fun observar(tipo: TipoItem?): Flow<List<CatalogoEntity>>
    @Query("SELECT * FROM catalogo_item WHERE nome LIKE '%'||:q||'%' AND ativo=1 LIMIT 50")
    suspend fun buscar(q: String): List<CatalogoEntity>
    @Query("SELECT * FROM catalogo_item WHERE codigoBarras=:ean AND ativo=1 LIMIT 1")
    suspend fun porEan(ean: String): CatalogoEntity?
}

@Dao
interface ListaDao {
    @Insert suspend fun criar(lista: ListaEntity)
    @Update suspend fun atualizar(lista: ListaEntity)
    @Query("SELECT * FROM lista_compra ORDER BY dataCriacao DESC")
    fun observarTodas(): Flow<List<ListaEntity>>
    @Query("SELECT * FROM lista_compra WHERE id=:id LIMIT 1")
    suspend fun porId(id: String): ListaEntity?
    @Query("UPDATE lista_compra SET finalizada=1, dataCompra=:iso WHERE id=:id")
    suspend fun finalizar(id: String, iso: String)
}

data class TotalPorTipoRow(val tipo: TipoItem, val total: Double, val qtdItens: Int, val qtdSel: Int)

@Dao
interface ItemDao {
    @Insert suspend fun adicionar(item: ItemEntity)
    @Update suspend fun atualizar(item: ItemEntity)
    @Query("DELETE FROM item_lista WHERE id=:id") suspend fun remover(id: String)
    @Query("UPDATE item_lista SET selecionado=:v WHERE id=:id") suspend fun setSelecionado(id: String, v: Boolean)
    @Query("SELECT * FROM item_lista WHERE listaId=:listaId ORDER BY ordem, nome")
    fun observarDaLista(listaId: String): Flow<List<ItemEntity>>
    @Query("SELECT tipo AS tipo, SUM(COALESCE(precoUnit,0)*quantidade) AS total, COUNT(*) AS qtdItens, SUM(CASE WHEN selecionado=1 THEN 1 ELSE 0 END) AS qtdSel FROM item_lista WHERE listaId=:listaId GROUP BY tipo ORDER BY total DESC")
    fun totalPorTipo(listaId: String): Flow<List<TotalPorTipoRow>>
    @Query("SELECT COALESCE(SUM(COALESCE(precoUnit,0)*quantidade),0) FROM item_lista WHERE listaId=:listaId AND selecionado=1")
    fun acumuladoSelecionados(listaId: String): Flow<Double>
    @Query("SELECT COUNT(*) FROM item_lista WHERE selecionado=1")
    fun contarSelecionados(): Flow<Int>
    @Query("SELECT * FROM item_lista WHERE listaId=:listaId ORDER BY ordem, nome")
    suspend fun listarDaLista(listaId: String): List<ItemEntity>
    // BI global por tipo com filtro de período (epoch millis) — dashboard 7D/30D/Tudo
    @Query("SELECT i.tipo AS tipo, SUM(COALESCE(i.precoUnit,0)*i.quantidade) AS total, COUNT(*) AS qtdItens, SUM(CASE WHEN i.selecionado=1 THEN 1 ELSE 0 END) AS qtdSel FROM item_lista i JOIN lista_compra l ON l.id=i.listaId WHERE l.finalizada=1 AND l.dataCriacao>=:desde ORDER BY total DESC")
    fun totalPorTipoGlobalDesde(desde: Long): Flow<List<TotalPorTipoRow>>
}
