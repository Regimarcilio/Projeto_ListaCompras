package br.com.listacompras

import br.com.listacompras.domain.usecase.validarItem
import br.com.listacompras.domain.model.TipoItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ValidacaoTest {
    @Test fun `nome vazio bloqueia`() { assertEquals("Nome obrigatório", validarItem("", 10.0, 1.0, TipoItem.CARNE)) }
    @Test fun `preco zero bloqueia`() { assertEquals("Preço deve ser > 0", validarItem("Arroz", 0.0, 1.0, TipoItem.MERCEARIA)) }
    @Test fun `qtd zero bloqueia`() { assertEquals("Quantidade deve ser > 0", validarItem("Arroz", 5.0, 0.0, TipoItem.MERCEARIA)) }
    @Test fun `valido passa`() { assertNull(validarItem("Patinho", 42.9, 1.5, TipoItem.CARNE)) }
    @Test fun `acumulado 2kg banana + 1_5kg patinho = 76_33`() {
        val total = 2.0 * 5.99 + 1.5 * 42.90
        assertEquals(76.33, total, 0.01)
    }
}
