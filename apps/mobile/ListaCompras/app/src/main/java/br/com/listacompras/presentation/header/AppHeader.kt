package br.com.listacompras.presentation.header

// STYLEMASTER · Nível ESSENCIAL — cabeçalho criativo reutilizável.
// Não depende do drawable do launcher (usa Icons.Filled.ShoppingCart).
// Theme-aware: funciona em claro + escuro via MaterialTheme (tokens de Theme.kt).

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.listacompras.presentation.theme.ListaComprasTheme

/**
 * TopAppBar pequena e criativa.
 *
 * @param titulo ex. "Minhas listas", "Catálogo", nome da lista ativa.
 * @param subtitulo opcional, ex. "Total: R$ 128,40 · 12 itens" ou nome da aba.
 * @param actions slot opcional à direita (ex. IconButton de busca, filtro, menu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    titulo: String,
    subtitulo: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Selo 40dp rounded 12dp verde com carrinho branco — eco do ícone do app.
                // Usa primary/onPrimary para contraste AA automático em claro e escuro.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingCart,
                        contentDescription = null, // decorativo; o título carrega a semântica
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitulo != null) {
                        Text(
                            text = subtitulo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

// ---- Previews (claro + escuro) ----

@Preview(name = "AppHeader · Claro", showBackground = true)
@Composable
private fun AppHeaderPreviewClaro() {
    ListaComprasTheme(escuro = false) {
        Surface {
            AppHeader(
                titulo = "Minhas listas",
                subtitulo = "Total: R$ 128,40 · 12 itens"
            )
        }
    }
}

@Preview(name = "AppHeader · Escuro", showBackground = true)
@Composable
private fun AppHeaderPreviewEscuro() {
    ListaComprasTheme(escuro = true) {
        Surface {
            AppHeader(
                titulo = "Catálogo",
                subtitulo = "Hortifruti"
            )
        }
    }
}
