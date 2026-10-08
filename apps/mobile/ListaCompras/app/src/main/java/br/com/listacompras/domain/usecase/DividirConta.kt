package br.com.listacompras.domain.usecase

/** Divide total entre N pessoas, com arredondamento para centavos (última pessoa absorve resíduo). */
fun dividirConta(total: Double, pessoas: Int): List<Double> {
    require(pessoas in 1..20) { "Pessoas deve ser 1..20" }
    val base = (total / pessoas * 100).toInt() / 100.0
    val out = MutableList(pessoas) { base }
    val residuo = ((total - base * pessoas) * 100).toInt() / 100.0
    out[pessoas - 1] = base + residuo
    return out
}

fun textoDivisao(nome: String, total: Double, pessoas: Int): String {
    val partes = dividirConta(total, pessoas)
    val sb = StringBuilder("🛒 $nome — TOTAL R$ %.2f / %d pessoas\n".format(total, pessoas))
    partes.forEachIndexed { i, v -> sb.append("Pessoa ${i + 1}: R$ %.2f\n".format(v)) }
    return sb.toString()
}
