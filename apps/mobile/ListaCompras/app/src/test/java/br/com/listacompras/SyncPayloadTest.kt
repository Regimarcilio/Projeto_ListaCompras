package br.com.listacompras

import br.com.listacompras.sync.EventoPayload
import br.com.listacompras.sync.SyncPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** #22: montagem do lote de sync (pura, JVM). */
class SyncPayloadTest {

    private fun lote2() = listOf(
        EventoPayload("app_aberto", 1_700_000_000_000L),
        EventoPayload("lista_fechada", 1_700_000_000_001L, mapOf("nome" to "Compra", "total" to "42.90", "qtd" to "3"))
    )

    @Test fun `monta chaves app v device events`() {
        val json = Json.parseToJsonElement(
            SyncPayload.montar("Pixel 7", "14", lote2())
        ).jsonObject
        assertEquals("ListaCompras", json["app"]!!.jsonPrimitive.content)
        assertEquals(1, json["v"]!!.jsonPrimitive.int)
        val device = json["device"]!!.jsonObject
        assertEquals("Pixel 7", device["model"]!!.jsonPrimitive.content)
        assertEquals("14", device["android"]!!.jsonPrimitive.content)
        val events = json["events"]!!.jsonArray
        assertEquals(2, events.size)
        assertEquals("app_aberto", events[0].jsonObject["tipo"]!!.jsonPrimitive.content)
        assertEquals(1_700_000_000_000L, events[0].jsonObject["ts"]!!.jsonPrimitive.long)
        val data = events[1].jsonObject["data"]!!.jsonObject
        assertEquals("Compra", data["nome"]!!.jsonPrimitive.content)
        assertEquals("42.90", data["total"]!!.jsonPrimitive.content)
    }

    @Test fun `https obrigatoria`() {
        assertTrue(SyncPayload.urlValida("https://script.google.com/macros/s/ABC/exec"))
        assertFalse(SyncPayload.urlValida("http://script.google.com/x"))
        assertFalse(SyncPayload.urlValida("https://"))
        assertFalse(SyncPayload.urlValida(""))
        assertFalse(SyncPayload.urlValida(null))
        assertFalse(SyncPayload.urlValida("ftp://x.com/y"))
    }

    @Test fun `email com arroba`() {
        assertTrue(SyncPayload.emailValido("a@b.com"))
        assertFalse(SyncPayload.emailValido("sem-arroba"))
        assertFalse(SyncPayload.emailValido(""))
        assertFalse(SyncPayload.emailValido(null))
        assertFalse(SyncPayload.emailValido("@sem-local.com"))
    }

    @Test fun `vazio nao posta`() {
        assertFalse(SyncPayload.devePostar(emptyList(), "https://x.com/exec"))
        assertFalse(SyncPayload.devePostar(lote2(), ""))
        assertFalse(SyncPayload.devePostar(lote2(), "http://x.com/exec"))
        assertFalse(SyncPayload.devePostar(lote2(), null))
        assertTrue(SyncPayload.devePostar(lote2(), "https://x.com/exec"))
    }
}
