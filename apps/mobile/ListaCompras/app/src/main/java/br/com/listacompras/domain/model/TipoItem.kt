package br.com.listacompras.domain.model

enum class TipoItem {
    HORTIFRUTI, CARNE, MERCEARIA, LIMPEZA,
    PADARIA, LATICINIOS, BEBIDAS, HIGIENE, CONGELADOS, OUTROS
}

data class TotalPorTipo(
    val tipo: TipoItem,
    val total: Double,
    val qtdItens: Int = 0,
    val qtdSelecionados: Int = 0
)
