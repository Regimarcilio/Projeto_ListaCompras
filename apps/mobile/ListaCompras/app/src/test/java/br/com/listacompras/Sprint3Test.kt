package br.com.listacompras

import br.com.listacompras.domain.model.TipoItem
import br.com.listacompras.share.JsonCodec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Sprint3Test {
    @Test fun `backup nome segue padrao`() {
        val nome = "backup-listacompras-123.json"
        assertTrue(nome.startsWith("backup-listacompras-") && nome.endsWith(".json"))
    }

    @Test fun `roundtrip export-import preserva tipos`() {
        val json = JsonCodec.export(emptyList(), "2026-10-08T00:00:00-03:00")
        val r = JsonCodec.import(json)
        assertTrue(r.erros.isEmpty() && r.listas.isEmpty())
        assertTrue(TipoItem.values().size == 10)
    }
}
