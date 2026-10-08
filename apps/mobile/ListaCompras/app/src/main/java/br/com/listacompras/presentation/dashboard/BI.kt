package br.com.listacompras.presentation.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// Unifica BI da lista + BI global em 1 aba (bottom passa de 6 -> 5 destinos).
// Mantém telas existentes intactas, só alterna por TabRow.
@Composable
fun BIScreen(listaId: String, abaInicial: Int = 0) {
    var aba by remember(abaInicial) { mutableStateOf(abaInicial.coerceIn(0, 1)) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = aba) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Lista", maxLines = 1) })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Global", maxLines = 1) })
        }
        if (aba == 0) DashboardScreen(listaId)
        else DashboardGlobalScreen()
    }
}
