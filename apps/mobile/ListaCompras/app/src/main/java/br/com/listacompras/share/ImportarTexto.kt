package br.com.listacompras.share

/** Item extraído do texto compartilhado (WhatsApp/SMS). */
data class ItemImportado(
    val nome: String,
    val qtd: Double,
    val unidade: String,
    val precoUnit: Double
)

/** Resultado do parser de texto: header opcional + itens + linhas ignoradas. */
data class ListaImportada(
    val nome: String?,
    val estabelecimento: String?,
    val itens: List<ItemImportado>,
    val ignoradas: Int
)

// Formato gerado por ListaAtiva.kt::montarTextoWhatsApp:
//   🛒 <nome> (<data>)
//   🏪 <estab> (opcional)
//   • <nome> — <qtd><un> x R$ <unit> = R$ <sub>
//   TOTAL: R$ <x>
private val ITEM_REGEX =
    Regex("""^•\s*(.+?)\s*[—–-]\s*([\d.,]+)\s*([A-Za-z]+)?\s*x\s*R\$\s*([\d.,]+)""")

// Fallback sem qtd/un: "• Arroz x R$ 5,00" → qtd 1.0, unidade "un".
private val ITEM_SEM_QTD_REGEX =
    Regex("""^•\s*(.+?)\s*[—–-]?\s*x\s*R\$\s*([\d.,]+)""")

private val NOME_DATA_REGEX = Regex("""^(.*?)\s*\(.*?\)\s*$""")

/** Aceita ponto e vírgula decimal ("5,99" e "5.99"); "1.234,56" → 1234.56. */
internal fun parseNumeroImportado(raw: String): Double? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    val norm = if (',' in t) t.replace(".", "").replace(",", ".") else t
    return norm.toDoubleOrNull()
}

/**
 * Parser PURO e testável do formato de share (não quebra o formato atual).
 * - header `🛒 nome (data)` → nome (sem a data); `🏪` → estabelecimento.
 * - pula linhas em branco, `TOTAL:` e headers (não contam como ignoradas).
 * - unidade default "un" se ausente; qtd default 1.0 (fallback sem qtd).
 * - demais linhas não reconhecidas (ex. linhas quebradas) contam em [ignoradas].
 */
fun parseListaTexto(texto: String): ListaImportada {
    var nome: String? = null
    var estabelecimento: String? = null
    val itens = mutableListOf<ItemImportado>()
    var ignoradas = 0

    texto.lines().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty()) return@forEach
        when {
            line.startsWith("🛒") -> {
                val resto = line.removePrefix("🛒").trim()
                if (resto.isNotEmpty()) {
                    val semData = NOME_DATA_REGEX.find(resto)?.groupValues?.get(1)?.trim() ?: resto
                    nome = semData.takeIf { it.isNotBlank() }
                }
            }
            line.startsWith("🏪") -> {
                val resto = line.removePrefix("🏪").trim()
                if (resto.isNotEmpty()) estabelecimento = resto
            }
            line.startsWith("TOTAL:") -> Unit // silencioso
            line.startsWith("•") -> {
                val m = ITEM_REGEX.find(line)
                if (m != null) {
                    val nomeItem = m.groupValues[1].trim()
                    val preco = parseNumeroImportado(m.groupValues[4])
                    if (nomeItem.isBlank() || preco == null) {
                        ignoradas++
                    } else {
                        val qtd = parseNumeroImportado(m.groupValues[2]) ?: 1.0
                        val unidade = m.groupValues[3].takeIf { it.isNotBlank() } ?: "un"
                        itens.add(ItemImportado(nomeItem, qtd, unidade, preco))
                    }
                } else {
                    val f = ITEM_SEM_QTD_REGEX.find(line)
                    val nomeItem = f?.groupValues?.get(1)?.trim()
                    val preco = f?.let { parseNumeroImportado(it.groupValues[2]) }
                    if (f != null && !nomeItem.isNullOrBlank() && preco != null) {
                        itens.add(ItemImportado(nomeItem, 1.0, "un", preco))
                    } else {
                        ignoradas++
                    }
                }
            }
            else -> ignoradas++
        }
    }

    return ListaImportada(nome, estabelecimento, itens, ignoradas)
}
