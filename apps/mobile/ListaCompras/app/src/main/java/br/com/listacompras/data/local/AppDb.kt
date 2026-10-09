package br.com.listacompras.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.listacompras.data.local.dao.CatalogoDao
import br.com.listacompras.data.local.dao.EventoDao
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.CatalogoEntity
import br.com.listacompras.data.local.entity.EventoEntity
import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.model.TipoItem

class Converters {
    @TypeConverter fun fromTipo(v: TipoItem): String = v.name
    @TypeConverter fun toTipo(v: String): TipoItem = runCatching { TipoItem.valueOf(v) }.getOrDefault(TipoItem.OUTROS)
}

@Database(entities = [CatalogoEntity::class, ListaEntity::class, ItemEntity::class, EventoEntity::class], version = 6, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun catalogo(): CatalogoDao
    abstract fun listas(): ListaDao
    abstract fun itens(): ItemDao
    abstract fun eventos(): EventoDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE catalogo_item ADD COLUMN precoRef REAL DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE catalogo_item ADD COLUMN codigoBarras TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_catalogo_item_codigo ON catalogo_item(codigoBarras)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE catalogo_item ADD COLUMN marca TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE catalogo_item ADD COLUMN quantidadeDefault REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE item_lista ADD COLUMN marca TEXT DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE lista_compra ADD COLUMN estabelecimento TEXT DEFAULT NULL")
    }
}

/** #22: outbox offline — cria evento_outbox sem perda (tabela nova, sem ALTER). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `evento_outbox` (" +
                "`id` TEXT NOT NULL, `tipo` TEXT NOT NULL, `criadoEm` INTEGER NOT NULL, " +
                "`payloadJson` TEXT NOT NULL, `sincronizado` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_outbox_sincronizado` ON `evento_outbox` (`sincronizado`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evento_outbox_criadoEm` ON `evento_outbox` (`criadoEm`)")
    }
}
