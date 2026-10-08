package br.com.listacompras.share

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class DownloadsHelper @Inject constructor(@ApplicationContext private val ctx: Context) {

    /** Salva JSON em Downloads (MediaStore API 29+ sem permissão) ou fallback cache. Retorna URI. */
    fun salvarEmDownloads(nomeArquivo: String, conteudo: String): Uri {
        val nome = if (nomeArquivo.endsWith(".json")) nomeArquivo else "$nomeArquivo.json"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nome)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/ListaCompras")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Falha ao criar arquivo em Downloads")
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(conteudo.toByteArray()) }
                ?: error("Falha ao escrever em Downloads")
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory("Download")
            val pasta = File(dir, "ListaCompras").apply { mkdirs() }
            val f = File(pasta, nome).apply { writeText(conteudo) }
            Uri.fromFile(f)
        }
    }

    fun lerUri(uri: Uri): String =
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Não foi possível ler o arquivo")
}
