package br.com.listacompras.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.listacompras.data.local.dao.EventoDao
import br.com.listacompras.data.local.prefs.PrefsRepo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.first

/** #22: entryPoint manual p/ Worker (forma simples, sem hilt-work). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncEntryPoint {
    fun eventoDao(): EventoDao
    fun prefsRepo(): PrefsRepo
}

/**
 * #22: envia o lote do outbox quando há rede (constraint CONNECTED).
 * Sem endpoint configurado → success silencioso. Só https://.
 * 2xx/4xx → marca sincronizados (4xx não retenta: erro definitivo);
 * resto/exceção → retry (backoff do WorkManager).
 */
class SyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext, SyncEntryPoint::class.java
        )
        val eventos: EventoDao = entry.eventoDao()
        val prefs: PrefsRepo = entry.prefsRepo()

        val endpoint = prefs.endpointUrl.first()
        if (!SyncPayload.urlValida(endpoint)) {
            Log.i(TAG, "sem endpoint https configurado — nada a enviar")
            return Result.success()
        }
        val pendentes = eventos.pendentes()
        if (pendentes.isEmpty()) return Result.success()

        val payloads = pendentes.map { e ->
            EventoPayload(tipo = e.tipo, ts = e.criadoEm, data = mapOf("payload" to e.payloadJson))
        }
        val body = SyncPayload.montar(Build.MODEL ?: "unknown", Build.VERSION.RELEASE ?: "?", payloads)
        val ids = pendentes.map { it.id }

        return try {
            val code = postJson(endpoint.trim(), body)
            when {
                code in 200..299 -> {
                    eventos.marcarSincronizados(ids)
                    eventos.limparAntigos(System.currentTimeMillis() - RETENCAO_MS)
                    Log.i(TAG, "sync ok: ${ids.size} eventos (HTTP $code)")
                    Result.success()
                }
                code in 400..499 -> {
                    // Erro definitivo (ex.: Apps Script revogado) — não adianta retentar.
                    eventos.marcarSincronizados(ids)
                    Log.w(TAG, "sync descartado (erro definitivo HTTP $code): ${ids.size} eventos")
                    Result.success()
                }
                else -> {
                    Log.w(TAG, "sync retry (HTTP $code)")
                    Result.retry()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "sync retry (exceção: ${t.message})")
            Result.retry()
        }
    }

    /** POST síncrono via HttpURLConnection (sem lib HTTP nova). Roda no Dispatchers.IO do CoroutineWorker. */
    internal fun postJson(endpoint: String, body: String): Int {
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            return conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        internal const val TIMEOUT_MS = 15_000
        private const val RETENCAO_MS = 7L * 24 * 60 * 60 * 1000
    }
}
