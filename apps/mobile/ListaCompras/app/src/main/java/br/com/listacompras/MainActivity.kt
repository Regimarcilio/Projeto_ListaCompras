package br.com.listacompras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import br.com.listacompras.presentation.catalogo.CatalogoScreen
import br.com.listacompras.presentation.dashboard.DashboardScreen
import br.com.listacompras.presentation.listaativa.ListaAtivaScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                var listaId by remember { mutableStateOf("demo-lista-01") }
                Scaffold(bottomBar = {
                    NavigationBar {
                        NavigationBarItem(selected = true, onClick = { nav.navigate("catalogo") }, label = { Text("Catálogo") }, icon = {})
                        NavigationBarItem(selected = false, onClick = { nav.navigate("lista") }, label = { Text("Lista") }, icon = {})
                        NavigationBarItem(selected = false, onClick = { nav.navigate("dashboard") }, label = { Text("BI") }, icon = {})
                    }
                }) { pad ->
                    NavHost(nav, startDestination = "catalogo", Modifier.padding(pad)) {
                        composable("catalogo") { CatalogoScreen() }
                        composable("lista") { ListaAtivaScreen(listaId) }
                        composable("dashboard") { DashboardScreen(listaId) }
                    }
                }
            }
        }
    }
}
