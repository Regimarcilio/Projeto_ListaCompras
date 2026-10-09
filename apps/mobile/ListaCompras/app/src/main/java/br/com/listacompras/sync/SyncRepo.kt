package br.com.listacompras.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.com.listacompras.data.local.dao.EventoDao
import br.com.listacompras.data.local.entity.EventoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** #22: enfileira eventos no outbox + agenda flush único (APPEND) sob rede. */
@Singleton
class SyncRepo @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val eventos: EventoDao
) {
    /** Contador de pendentes p/ Ajustes. */
    fun pendentes(): Flow<Int> = eventos.contarPendentes()

    /** Enfileira evento genérico e agenda o flush. */
    suspend fun logEvent(tipo: String, data: Map<String, String> = emptyMap()) {
        val payload = buildJsonObject {
            data.forEach { (k, v) -> put(k, v) }
        }.toString()
        eventos.enfileirar(
            EventoEntity(tipo = tipo, criadoEm = System.currentTimeMillis(), payloadJson = payload)
        )
        agendar()
    }

    /** Hook PRONTO p/ #20 (Google Sign-In): chamar após login com sub do idToken + e-mail. */
    suspend fun logLogin(idTokenSub: String, email: String) {
        logEvent("login", mapOf("sub" to idTokenSub, "email" to email))
    }

    /** Evento de abertura do app (MainActivity, 1x por criação). */
    suspend fun logAppAberto() = logEvent("app_aberto")

    /** Evento de fechamento de lista (nome/total/qtd). */
    suspend fun logListaFechada(nome: String, total: Double, qtdItens: Int) =
        logEvent(
            "lista_fechada",
            mapOf(
                "nome" to nome,
                "total" to String.format(Locale.US, "%.2f", total),
                "qtd" to qtdItens.toString()
            )
        )

    /** Agenda flush único; APPEND acumula sem duplicar trabalho. */
    fun agendar() {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .addTag(TAG_SYNC)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork(NOME_TRABALHO, ExistingWorkPolicy.APPEND, req)
    }

    /** Força flush imediato (botão "Enviar agora" em Ajustes). */
    fun flushAgora() = agendar()

    companion object {
        const val NOME_TRABALHO = "sync-outbox"
        const val TAG_SYNC = "sync-outbox"
    }
}

/** Valida JSON de payload (objeto) — usado antes de enfileirar. */
internal fun jsonValido(s: String): Boolean = runCatching {
    Json.parseToJsonElement(s)
    true
}.getOrDefault(false)
