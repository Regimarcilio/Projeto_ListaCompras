package br.com.listacompras.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.*
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportShareHelper @Inject constructor(@ApplicationContext private val ctx: Context) {

    fun listaParaTexto(nome: String, itens: List<Triple<String, Double, Double>>, total: Double): String {
        val sb = StringBuilder("🛒 $nome\n")
        itens.forEach { (n, q, p) -> sb.append("• $n — ${q}x R$ %.2f = R$ %.2f\n".format(p, q * p)) }
        sb.append("TOTAL: R$ %.2f".format(total))
        return sb.toString()
    }

    fun salvarJson(nomeArquivo: String, json: String): File {
        val dir = File(ctx.cacheDir, "export").apply { mkdirs() }
        return File(dir, nomeArquivo).apply { writeText(json) }
    }

    fun shareTexto(texto: String) {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto)
        }
        ctx.startActivity(Intent.createChooser(i, "Enviar lista via").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun shareJson(file: File, texto: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, texto)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, "Compartilhar JSON").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun agoraIso(): String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
