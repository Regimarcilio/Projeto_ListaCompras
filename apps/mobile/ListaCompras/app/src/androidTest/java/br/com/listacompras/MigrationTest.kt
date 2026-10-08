package br.com.listacompras

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.listacompras.data.local.AppDb
import br.com.listacompras.data.local.MIGRATION_3_4
import br.com.listacompras.data.local.MIGRATION_4_5
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Issue #14 — valida migrations sem perda de dados.
// Roda em aparelho/emulador: ./gradlew :app:connectedDebugAndroidTest
// (o CI atual não tem emulador, então este teste ainda não executa no GitHub).
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDb::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migra3_para5_preservaDados() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO catalogo_item (id, nome, tipo, unidadeDefault, precoRef, codigoBarras, ativo, createdAt, updatedAt) " +
                    "VALUES ('c1', 'Arroz', 'MERCEARIA', 'kg', 5.99, NULL, 1, 't', 't')"
            )
            execSQL("INSERT INTO lista_compra (id, nome, dataCriacao, dataCompra, finalizada) VALUES ('l1', 'Compra', 1, NULL, 0)")
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_3_4, MIGRATION_4_5).apply {
            query("SELECT marca, quantidadeDefault FROM catalogo_item WHERE id='c1'").use {
                assertTrue(it.moveToFirst())
                assertTrue(it.isNull(0))
                assertEquals(1.0, it.getDouble(1), 0.0)
            }
            query("SELECT estabelecimento FROM lista_compra WHERE id='l1'").use {
                assertTrue(it.moveToFirst())
                assertTrue(it.isNull(0))
            }
            query("SELECT nome, precoRef FROM catalogo_item WHERE id='c1'").use {
                assertTrue(it.moveToFirst())
                assertEquals("Arroz", it.getString(0))
                assertEquals(5.99, it.getDouble(1), 0.0)
            }
            close()
        }
    }

    @Test
    fun migra4_para5_preservaDados() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO catalogo_item (id, nome, tipo, unidadeDefault, precoRef, codigoBarras, ativo, createdAt, updatedAt, marca, quantidadeDefault) " +
                    "VALUES ('c2', 'Leite', 'LATICINIOS', 'L', 4.5, NULL, 1, 't', 't', 'Parmalat', 1.0)"
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5).apply {
            query("SELECT marca, quantidadeDefault FROM catalogo_item WHERE id='c2'").use {
                assertTrue(it.moveToFirst())
                assertEquals("Parmalat", it.getString(0))
                assertEquals(1.0, it.getDouble(1), 0.0)
            }
            close()
        }
    }

    @Test
    fun migra3_para4_colunasNovasDefault() {
        helper.createDatabase(TEST_DB2, 3).apply {
            execSQL(
                "INSERT INTO catalogo_item (id, nome, tipo, unidadeDefault, precoRef, codigoBarras, ativo, createdAt, updatedAt) " +
                    "VALUES ('c3', 'Feijão', 'MERCEARIA', 'kg', 8.49, NULL, 1, 't', 't')"
            )
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB2, 4, true, MIGRATION_3_4).apply {
            query("PRAGMA table_info(catalogo_item)").use { c ->
                val cols = mutableSetOf<String>()
                while (c.moveToNext()) cols += c.getString(1)
                assertTrue(cols.contains("marca"))
                assertTrue(cols.contains("quantidadeDefault"))
            }
            query("PRAGMA table_info(item_lista)").use { c ->
                val cols = mutableSetOf<String>()
                while (c.moveToNext()) cols += c.getString(1)
                assertTrue(cols.contains("marca"))
            }
            close()
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
        private const val TEST_DB2 = "migration-test-2"
    }
}
