package br.com.listacompras.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.listacompras.data.local.prefs.PrefsRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: PrefsRepo) : ViewModel() {
    val temaEscuro = prefs.temaEscuro
    // null = nunca configurado -> UI usa isSystemInDarkTheme() como fallback.
    val temaEscuroOrNull = prefs.temaEscuroOrNull
    val backupAuto = prefs.backupAuto
    fun tema(v: Boolean) = viewModelScope.launch { prefs.setTema(v) }
    fun backup(v: Boolean) = viewModelScope.launch { prefs.setBackupAuto(v) }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val escuro by vm.temaEscuro.collectAsState(false)
    val backup by vm.backupAuto.collectAsState(true)
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Tema escuro"); Switch(checked = escuro, onCheckedChange = { vm.tema(it) })
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Backup automático semanal"); Switch(checked = backup, onCheckedChange = { vm.backup(it) })
        }
        Text("Backup salva JSON em Downloads a cada 7 dias ao abrir o app.", style = MaterialTheme.typography.bodySmall)
    }
}
