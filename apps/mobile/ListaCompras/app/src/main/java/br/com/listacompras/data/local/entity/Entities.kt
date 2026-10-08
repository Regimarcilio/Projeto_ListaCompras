package br.com.listacompras.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.listacompras.domain.model.TipoItem
import java.time.Instant
import java.util.UUID

@Entity(tableName = "catalogo_item", indices = [Index("tipo"), Index(value = ["nome"], unique = true)])
data class CatalogoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val tipo: TipoItem,
    val unidadeDefault: String = "un",
    val precoRef: Double? = null,
    val ativo: Boolean = true,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString()
)

@Entity(tableName = "lista_compra", indices = [Index("dataCriacao"), Index("finalizada")])
data class ListaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataCompra: String? = null,
    val finalizada: Boolean = false
)

@Entity(
    tableName = "item_lista",
    indices = [Index("listaId"), Index("catalogoItemId"), Index("tipo"), Index("selecionado"), Index(value = ["listaId", "ordem"])]
)
data class ItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val listaId: String,
    val catalogoItemId: String? = null,
    val nome: String,
    val tipo: TipoItem,
    val unidade: String = "un",
    val quantidade: Double = 1.0,
    val precoUnit: Double? = null,
    val selecionado: Boolean = false,
    val ordem: Int = 0
)
