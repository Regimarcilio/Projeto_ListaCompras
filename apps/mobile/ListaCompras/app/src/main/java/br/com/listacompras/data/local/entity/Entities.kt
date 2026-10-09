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
    val quantidadeDefault: Double = 1.0,
    val marca: String? = null,
    val precoRef: Double? = null,
    val codigoBarras: String? = null, // Sprint4: EAN digitado (câmera = roadmap, sem permissão agora)
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
    val finalizada: Boolean = false,
    val estabelecimento: String? = null
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
    val marca: String? = null,
    val selecionado: Boolean = false,
    val ordem: Int = 0
)

/** #22: outbox offline — eventos enfileirados localmente e enviados em lote pelo SyncWorker. */
@Entity(tableName = "evento_outbox", indices = [Index("sincronizado"), Index("criadoEm")])
data class EventoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tipo: String,
    val criadoEm: Long = System.currentTimeMillis(),
    val payloadJson: String = "{}",
    val sincronizado: Boolean = false
)
