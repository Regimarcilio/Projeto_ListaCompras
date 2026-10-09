package br.com.listacompras.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** #22: evento pronto p/ serialização no lote de sync. */
data class EventoPayload(val tipo: String, val ts: Long, val data: Map<String, String> = emptyMap())

/** #22: montagem pura do JSON de sync — sem Android, testável em JVM. */
object SyncPayload {
    const val APP = "ListaCompras"
    const val V = 1

    /** Só https:// — http é recusado. */
    fun urlValida(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val u = url.trim()
        if (!u.startsWith("https://")) return false
        if (u.length <= "https://".length + 1) return false
        if (u.contains(" ")) return false
        return true
    }

    /** E-mail mínimo: contém @ com algo antes e ponto depois. */
    fun emailValido(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val e = email.trim()
        val at = e.indexOf('@')
        if (at <= 0 || at >= e.length - 1) return false
        val dominio = e.substring(at + 1)
        return '.' in dominio && !dominio.startsWith(".") && !dominio.endsWith(".")
    }

    /** Há o que postar? Lote não-vazio + endpoint https válido. */
    fun devePostar(events: List<EventoPayload>, endpointUrl: String?): Boolean =
        events.isNotEmpty() && urlValida(endpointUrl)

    /** Monta `{app, v, device:{model, android}, events:[{tipo, ts, data}]}`. */
    fun montar(
        model: String,
        androidRelease: String,
        events: List<EventoPayload>
    ): String {
        val root = buildJsonObject {
            put("app", APP)
            put("v", V)
            put("device", buildJsonObject {
                put("model", model)
                put("android", androidRelease)
            })
            putJsonArray("events") {
                events.forEach { ev ->
                    add(buildJsonObject {
                        put("tipo", ev.tipo)
                        put("ts", ev.ts)
                        put("data", buildJsonObject {
                            ev.data.forEach { (k, v) -> put(k, v) }
                        })
                    })
                }
            }
        }
        return Json.encodeToString(JsonObject.serializer(), root)
    }
}
