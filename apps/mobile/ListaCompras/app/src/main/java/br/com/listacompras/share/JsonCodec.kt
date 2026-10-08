package br.com.listacompras.share

import br.com.listacompras.data.local.entity.ItemEntity
import br.com.listacompras.data.local.entity.ListaEntity
import br.com.listacompras.domain.model.TipoItem
import kotlinx.serialization.json.*
import java.util.UUID

object JsonCodec {
    const val SCHEMA = 2
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun export(listas: List<Pair<ListaEntity, List<ItemEntity>>>, exportedAt: String): String {
        val arr = buildJsonArray {
            listas.forEach { (l, itens) ->
                add(buildJsonObject {
                    put("id", l.id); put("nome", l.nome)
                    put("dataCriacao", l.dataCriacao)
                    l.dataCompra?.let { put("dataCompra", it) }
                    put("finalizada", l.finalizada)
                    put("itens", buildJsonArray {
                        itens.forEach { i ->
                            add(buildJsonObject {
                                put("id", i.id); put("nome", i.nome); put("tipo", i.tipo.name)
                                put("unidade", i.unidade); put("quantidade", i.quantidade)
                                i.precoUnit?.let { put("precoUnit", it) }
                                put("selecionado", i.selecionado); put("ordem", i.ordem)
                            })
                        }
                    })
                })
            }
        }
        return json.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("app", "ListaCompras"); put("schemaVersion", SCHEMA); put("exportedAt", exportedAt); put("listas", arr)
        })
    }

    data class ImportResult(val listas: List<Pair<ListaEntity, List<ItemEntity>>>, val erros: List<String>)

    fun import(texto: String): ImportResult {
        val erros = mutableListOf<String>()
        val root = runCatching { json.parseToJsonElement(texto).jsonObject }.getOrElse {
            return ImportResult(emptyList(), listOf("JSON inválido: ${it.message}"))
        }
        val v = root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 0
        if (v > SCHEMA) return ImportResult(emptyList(), listOf("schemaVersion $v maior que suportado ($SCHEMA)"))
        val out = mutableListOf<Pair<ListaEntity, List<ItemEntity>>>()
        root["listas"]?.jsonArray?.forEachIndexed { li, el ->
            try {
                val o = el.jsonObject
                val nome = o["nome"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                    ?: run { erros.add("lista[$li]: nome ausente"); return@forEachIndexed }
                val lid = o["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString()
                val lista = ListaEntity(
                    id = lid, nome = nome,
                    dataCriacao = o["dataCriacao"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
                    dataCompra = o["dataCompra"]?.jsonPrimitive?.contentOrNull,
                    finalizada = o["finalizada"]?.jsonPrimitive?.booleanOrNull ?: false
                )
                val itens = mutableListOf<ItemEntity>()
                o["itens"]?.jsonArray?.forEachIndexed { ii, ie ->
                    try {
                        val io = ie.jsonObject
                        val inome = io["nome"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                            ?: throw IllegalArgumentException("nome ausente")
                        val tipo = runCatching { TipoItem.valueOf(io["tipo"]?.jsonPrimitive?.content ?: "OUTROS") }.getOrDefault(TipoItem.OUTROS)
                        val qtd = io["quantidade"]?.jsonPrimitive?.doubleOrNull ?: 1.0
                        if (qtd <= 0) throw IllegalArgumentException("quantidade deve ser > 0")
                        val preco = io["precoUnit"]?.jsonPrimitive?.doubleOrNull
                        if (preco != null && preco < 0) throw IllegalArgumentException("preço negativo")
                        itens.add(ItemEntity(
                            id = io["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString(),
                            listaId = lid, nome = inome, tipo = tipo,
                            unidade = io["unidade"]?.jsonPrimitive?.contentOrNull ?: "un",
                            quantidade = qtd, precoUnit = preco,
                            selecionado = io["selecionado"]?.jsonPrimitive?.booleanOrNull ?: false,
                            ordem = io["ordem"]?.jsonPrimitive?.intOrNull ?: ii
                        ))
                    } catch (e: Exception) { erros.add("lista[$li].item[$ii]: ${e.message}") }
                }
                out.add(lista to itens)
            } catch (e: Exception) { erros.add("lista[$li]: ${e.message}") }
        }
        return ImportResult(out, erros)
    }
}
