package br.com.listacompras.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.listacompras.data.local.dao.CatalogoDao
import br.com.listacompras.data.local.dao.ItemDao
import br.com.listacompras.data.local.dao.ListaDao
import br.com.listacompras.data.local.entity.CatalogoEntity
import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.model.TipoItem

class Converters {
    @TypeConverter fun fromTipo(v: TipoItem): String = v.name
    @TypeConverter fun toTipo(v: String): TipoItem = runCatching { TipoItem.valueOf(v) }.getOrDefault(TipoItem.OUTROS)
}

@Database(entities = [CatalogoEntity::class, ListaEntity::class, ItemEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun catalogo(): CatalogoDao
    abstract fun listas(): ListaDao
    abstract fun itens(): ItemDao
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
