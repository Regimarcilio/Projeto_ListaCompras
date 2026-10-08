package br.com.listacompras.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.listacompras.data.local.AppDb
import br.com.listacompras.data.local.MIGRATION_1_2
import br.com.listacompras.data.local.MIGRATION_2_3
import br.com.listacompras.data.local.MIGRATION_3_4
import br.com.listacompras.data.local.entity.CatalogoEntity
import br.com.listacompras.domain.model.TipoItem
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDb =
        Room.databaseBuilder(ctx, AppDb::class.java, "listacompras.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed idempotente (espelha seed/catalogo_seed.json)
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            val built = Room.databaseBuilder(ctx, AppDb::class.java, "listacompras.db")
                                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                            built.catalogo().seedAll(
                                listOf(
                                    CatalogoEntity(nome = "Banana prata", tipo = TipoItem.HORTIFRUTI, unidadeDefault = "kg", precoRef = 5.99),
                                    CatalogoEntity(nome = "Alface crespa", tipo = TipoItem.HORTIFRUTI, unidadeDefault = "un", precoRef = 3.49),
                                    CatalogoEntity(nome = "Patinho", tipo = TipoItem.CARNE, unidadeDefault = "kg", precoRef = 42.90),
                                    CatalogoEntity(nome = "Peito de frango", tipo = TipoItem.CARNE, unidadeDefault = "kg", precoRef = 24.90),
                                    CatalogoEntity(nome = "Arroz 5kg", tipo = TipoItem.MERCEARIA, unidadeDefault = "pct", precoRef = 22.90),
                                    CatalogoEntity(nome = "Feijão 1kg", tipo = TipoItem.MERCEARIA, unidadeDefault = "pct", precoRef = 8.49),
                                    CatalogoEntity(nome = "Água sanitária 2L", tipo = TipoItem.LIMPEZA, unidadeDefault = "un", precoRef = 8.49),
                                    CatalogoEntity(nome = "Detergente 500ml", tipo = TipoItem.LIMPEZA, unidadeDefault = "un", precoRef = 2.99)
                                )
                            )
                            built.close()
                        }
                    }
                }
            })
            .build()

    @Provides
    fun provideCatalogo(db: AppDb) = db.catalogo()

    @Provides
    fun provideListas(db: AppDb) = db.listas()

    @Provides
    fun provideItens(db: AppDb) = db.itens()
}
