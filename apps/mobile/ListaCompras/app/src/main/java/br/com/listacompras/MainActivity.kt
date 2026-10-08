package br.com.listacompras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import br.com.listacompras.presentation.catalogo.CatalogoScreen
import br.com.listacompras.presentation.dashboard.DashboardGlobalScreen
import br.com.listacompras.presentation.dashboard.DashboardScreen
import br.com.listacompras.presentation.historico.HistoricoScreen
import br.com.listacompras.presentation.listaativa.ListaAtivaScreen
import br.com.listacompras.presentation.settings.SettingsScreen
import br.com.listacompras.presentation.theme.ListaComprasTheme
import dagger.hilt.android.AndroidEntryPoint

private data class Destino(val rota: String, val rotulo: String, val icone: ImageVector)

private val DESTINOS = listOf(
    Destino("catalogo", "Catálogo", Icons.Filled.Store),
    Destino("lista", "Lista", Icons.Filled.ShoppingCart),
    Destino("dashboard", "BI", Icons.Filled.BarChart),
    Destino("historico", "Histórico", Icons.Filled.History),
    Destino("global", "Global", Icons.Filled.Dashboard),
    Destino("ajustes", "Ajustes", Icons.Filled.Settings)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Mesmo padrão visual da prévia web (tokens stylemaster)
            ListaComprasTheme {
                val nav = rememberNavController()
                var listaId by remember { mutableStateOf("demo-lista-01") }
                val mainVm: MainViewModel = hiltViewModel()
                val badge by mainVm.badge.collectAsState()
                val rotaAtual = nav.currentBackStackEntryAsState().value?.destination?.route
                Scaffold(bottomBar = {
                    NavigationBar {
                        DESTINOS.forEach { d ->
                            NavigationBarItem(
                                selected = rotaAtual == d.rota,
                                onClick = {
                                    nav.navigate(d.rota) {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                label = { Text(d.rotulo) },
                                icon = {
                                    if (d.rota == "lista" && badge > 0) {
                                        BadgedBox(badge = { Badge { Text(if (badge > 99) "99+" else "$badge") } }) {
                                            Icon(d.icone, contentDescription = d.rotulo)
                                        }
                                    } else {
                                        Icon(d.icone, contentDescription = d.rotulo)
                                    }
                                }
                            )
                        }
                    }
                }) { pad ->
                    NavHost(nav, startDestination = "catalogo", Modifier.padding(pad)) {
                        composable("catalogo") { CatalogoScreen() }
                        composable("lista") { ListaAtivaScreen(listaId) }
                        composable("dashboard") { DashboardScreen(listaId) }
                        composable("historico") { HistoricoScreen() }
                        composable("global") { DashboardGlobalScreen() }
                        composable("ajustes") { SettingsScreen() }
                    }
                }
            }
        }
    }
}
