package br.com.listacompras

import br.com.listacompras.domain.usecase.dividirConta
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Sprint4Test {
    @Test fun `dividir 76_33 por 2 soma de volta`() {
        val partes = dividirConta(76.33, 2)
        assertEquals(2, partes.size)
        assertEquals(76.33, partes.sum(), 0.011)
    }

    @Test fun `dividir 100 por 3 sem perder centavos`() {
        val partes = dividirConta(100.0, 3)
        assertEquals(100.0, partes.sum(), 0.011)
    }
}
