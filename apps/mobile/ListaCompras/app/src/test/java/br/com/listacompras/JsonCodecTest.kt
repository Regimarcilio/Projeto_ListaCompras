package br.com.listacompras

import br.com.listacompras.share.JsonCodec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonCodecTest {
    @Test fun `export contem schemaVersion e exportedAt`() {
        val json = JsonCodec.export(emptyList(), "2026-10-07T14:30:00-03:00")
        assertTrue(json.contains("schemaVersion") && json.contains("exportedAt"))
    }

    @Test fun `import rejeita schema futuro`() {
        val r = JsonCodec.import("""{"schemaVersion":99,"listas":[]}""")
        assertTrue(r.erros.isNotEmpty() && r.listas.isEmpty())
    }

    @Test fun `import rejeita quantidade zero e segue demais`() {
        val payload = """{"schemaVersion":2,"exportedAt":"x","listas":[{"id":"l1","nome":"Feira","dataCriacao":1,"finalizada":false,"itens":[{"id":"a","nome":"Banana","tipo":"HORTIFRUTI","unidade":"kg","quantidade":0},{"id":"b","nome":"Arroz","tipo":"MERCEARIA","unidade":"pct","quantidade":1,"precoUnit":22.9}]}]}"""
        val r = JsonCodec.import(payload)
        assertEquals(1, r.listas.size)
        assertEquals(1, r.listas[0].second.size) // só Arroz
        assertTrue(r.erros.any { it.contains("quantidade") })
    }
}
