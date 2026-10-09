package br.com.listacompras

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.*
import br.com.listacompras.presentation.catalogo.CatalogoScreen
import br.com.listacompras.presentation.dashboard.BIScreen
import br.com.listacompras.presentation.header.AppHeader
import br.com.listacompras.presentation.historico.HistoricoScreen
import br.com.listacompras.presentation.listaativa.ListaAtivaScreen
import br.com.listacompras.presentation.settings.SettingsScreen
import br.com.listacompras.presentation.settings.SettingsViewModel
import br.com.listacompras.presentation.theme.ListaComprasTheme
import dagger.hilt.android.AndroidEntryPoint

private data class Destino(val rota: String, val rotulo: String, val icone: ImageVector)

private val DESTINOS = listOf(
    Destino("catalogo", "Catálogo", Icons.Filled.Store),
    Destino("lista", "Lista", Icons.Filled.ShoppingCart),
    Destino("dashboard", "BI", Icons.Filled.BarChart),
    Destino("historico", "Histórico", Icons.Filled.History),
    Destino("ajustes", "Ajustes", Icons.Filled.Settings)
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // #16: texto recebido via share do sistema (ACTION_SEND text/plain).
    private val sharedTextState = mutableStateOf<String?>(null)

    private fun extrairTextoShare(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extrairTextoShare(intent)?.let { sharedTextState.value = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedTextState.value = extrairTextoShare(intent)
        enableEdgeToEdge()
        setContent {
            // Tema: prefs.temaEscuro (Settings salva) com fallback p/ sistema quando nunca configurado.
            val settingsVm: SettingsViewModel = hiltViewModel()
            val temaPref by settingsVm.temaEscuroOrNull.collectAsState(initial = null)
            val escuro = temaPref ?: isSystemInDarkTheme()
            // Mesmo padrão visual da prévia web (tokens stylemaster)
            ListaComprasTheme(escuro = escuro) {
                val nav = rememberNavController()
                var listaId by remember { mutableStateOf("demo-lista-01") }
                // #16: share recebido → navega p/ lista; a importação pede confirmação no diálogo.
                val sharedText by sharedTextState
                LaunchedEffect(sharedText) {
                    if (!sharedText.isNullOrBlank()) {
                        nav.navigate("lista") {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
                val mainVm: MainViewModel = hiltViewModel()
                // P0: garante que a lista ativa exista antes de qualquer tela usar.
                LaunchedEffect(listaId) { mainVm.garantirLista(listaId) }
                // #22: evento app_aberto 1x por criação da Activity.
                LaunchedEffect(Unit) { mainVm.logAppAberto() }
                val badge by mainVm.badge.collectAsState()
                val rotaAtual = nav.currentBackStackEntryAsState().value?.destination?.route
                // Cabeçalho criativo (STYLEMASTER): selo verde + carrinho, eco do ícone do app.
                val (tituloBar, subtituloBar) = when (rotaAtual) {
                    "lista" -> "Lista de compras" to "Marque no carrinho"
                    "dashboard" -> "BI" to "Lista · Global"
                    "global" -> "BI Global" to "Finalizadas"
                    "historico" -> "Histórico" to "Compras finalizadas"
                    "ajustes" -> "Ajustes" to "Tema e backup"
                    else -> "Catálogo" to "Toque + Lista para comprar"
                }
                Scaffold(
                    topBar = { AppHeader(titulo = tituloBar, subtitulo = subtituloBar) },
                    bottomBar = {
                    // 5 destinos (era 6 e quebrava "Catálogo/Histórico" em 360dp).
                    // BI agora unifica Lista+Global via TabRow. Label travado 1 linha.
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
                                alwaysShowLabel = true,
                                label = {
                                    Text(
                                        d.rotulo,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp
                                    )
                                },
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
                        composable("catalogo") {
                            CatalogoScreen(
                                listaId = listaId,
                                onAdicionar = {
                                    nav.navigate("lista") {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable("lista") {
                            ListaAtivaScreen(
                                listaId,
                                onListaFechada = { novaId -> listaId = novaId },
                                textoCompartilhado = sharedText,
                                onTextoCompartilhadoConsumido = { sharedTextState.value = null }
                            )
                        }
                        composable("dashboard") { BIScreen(listaId, abaInicial = 0) }
                        composable("historico") { HistoricoScreen() }
                        // Compat: rota antiga "global" abre a aba Global dentro do BI
                        composable("global") { BIScreen(listaId, abaInicial = 1) }
                        composable("ajustes") { SettingsScreen() }
                    }
                }
            }
        }
    }
}
