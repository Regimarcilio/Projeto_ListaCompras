package br.com.listacompras

import br.com.listacompras.share.parseListaTexto
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ImportarTextoTest {

    @Test fun `texto valido com 3 itens extrai nome estab e itens`() {
        val texto = """
            🛒 Compra da semana (09/10/2026 10:00)
            🏪 Atacadão
            • Banana prata — 2kg x R$ 5.99 = R$ 11.98
            • Arroz 5kg — 1un x R$ 22.90 = R$ 22.90
            • Leite — 2L x R$ 4.50 = R$ 9.00
            TOTAL: R$ 43.88
        """.trimIndent()

        val res = parseListaTexto(texto)

        assertEquals("Compra da semana", res.nome)
        assertEquals("Atacadão", res.estabelecimento)
        assertEquals(3, res.itens.size)
        assertEquals(0, res.ignoradas)
        assertEquals("Banana prata", res.itens[0].nome)
        assertEquals(2.0, res.itens[0].qtd, 0.001)
        assertEquals("kg", res.itens[0].unidade)
        assertEquals(5.99, res.itens[0].precoUnit, 0.001)
        assertEquals("un", res.itens[1].unidade)
        assertEquals(22.90, res.itens[1].precoUnit, 0.001)
        assertEquals("L", res.itens[2].unidade)
    }

    @Test fun `decimais com virgula sao aceitos`() {
        val texto = """
            🛒 Feira (09/10/2026)
            • Banana — 2,5kg x R$ 5,99 = R$ 14,98
            • Arroz — 1un x R$ 22,90 = R$ 22,90
            TOTAL: R$ 37,88
        """.trimIndent()

        val res = parseListaTexto(texto)

        assertEquals("Feira", res.nome)
        assertEquals(2, res.itens.size)
        assertEquals(0, res.ignoradas)
        assertEquals(2.5, res.itens[0].qtd, 0.001)
        assertEquals(5.99, res.itens[0].precoUnit, 0.001)
        assertEquals(22.90, res.itens[1].precoUnit, 0.001)
    }

    @Test fun `linhas quebradas sao ignoradas com contagem`() {
        val texto = """
            🛒 Lista quebrada (09/10)
            • Arroz — 1un x R$ 5.00 = R$ 5.00
            • linha quebrada sem preco
            texto aleatorio fora do formato
            • Feijão — 2un x R$ 8.49 = R$ 16.98
            TOTAL: R$ 21.98
        """.trimIndent()

        val res = parseListaTexto(texto)

        assertEquals(2, res.itens.size)
        assertEquals(2, res.ignoradas)
        assertEquals("Arroz", res.itens[0].nome)
        assertEquals("Feijão", res.itens[1].nome)
    }

    @Test fun `unidade ausente vira un e qtd ausente vira 1`() {
        val texto = """
            🛒 Lista simples (09/10)
            • Leite x R$ 4,50 = R$ 4,50
            • Ovos — 12 x R$ 0,80 = R$ 9,60
        """.trimIndent()

        val res = parseListaTexto(texto)

        assertEquals(2, res.itens.size)
        assertEquals(0, res.ignoradas)
        assertEquals(1.0, res.itens[0].qtd, 0.001)
        assertEquals("un", res.itens[0].unidade)
        assertEquals(4.50, res.itens[0].precoUnit, 0.001)
        assertEquals(12.0, res.itens[1].qtd, 0.001)
        assertEquals("un", res.itens[1].unidade)
    }
}
