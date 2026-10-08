package br.com.listacompras.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tokens stylemaster (mesmos da prévia web) — ESSENCIAL, AA
val Green900 = Color(0xFF1B5E20)
val Green700 = Color(0xFF2E7D32)
val GreenBg = Color(0xFFE8F5E9)
val BgApp = Color(0xFFF2F4F2)
val LineColor = Color(0xFFE0E0E0)
val Danger = Color(0xFFC62828)

private val CoresClaras = lightColorScheme(
    primary = Green900,
    onPrimary = Color.White,
    primaryContainer = GreenBg,
    onPrimaryContainer = Green900,
    secondary = Green700,
    onSecondary = Color.White,
    surface = Color.White,
    background = BgApp,
    error = Danger
)

private val CoresEscuras = darkColorScheme(
    primary = Color(0xFFA5D6A7),
    onPrimary = Color(0xFF1B3A1F),
    primaryContainer = Color(0xFF1B3A1F),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF10240F),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF0F2422),
    background = Color(0xFF121614),
    onBackground = Color(0xFFE8ECE8),
    surface = Color(0xFF1A1F1A),
    onSurface = Color(0xFFE8ECE8),
    surfaceVariant = Color(0xFF2A332A),
    onSurfaceVariant = Color(0xFFC8D0C8),
    error = Color(0xFFCF6679),
    onError = Color(0xFF3A0A12)
)

@Composable
fun ListaComprasTheme(escuro: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) CoresEscuras else CoresClaras,
        content = content
    )
}
