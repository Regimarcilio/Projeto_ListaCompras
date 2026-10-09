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
import br.com.listacompras.sync.SyncPayload
import br.com.listacompras.sync.SyncRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PrefsRepo,
    private val sync: SyncRepo
) : ViewModel() {
    val temaEscuro = prefs.temaEscuro
    // null = nunca configurado -> UI usa isSystemInDarkTheme() como fallback.
    val temaEscuroOrNull = prefs.temaEscuroOrNull
    val backupAuto = prefs.backupAuto
    fun tema(v: Boolean) = viewModelScope.launch { prefs.setTema(v) }
    fun backup(v: Boolean) = viewModelScope.launch { prefs.setBackupAuto(v) }

    // #22: sync outbox.
    val endpointUrl = prefs.endpointUrl
    val ownerEmail = prefs.ownerEmail
    val pendentes = sync.pendentes()
    fun salvarEndpoint(v: String) = viewModelScope.launch { prefs.setEndpointUrl(v) }
    fun salvarEmail(v: String) = viewModelScope.launch { prefs.setOwnerEmail(v) }
    fun enviarAgora() = viewModelScope.launch { sync.flushAgora() }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val escuro by vm.temaEscuro.collectAsState(false)
    val backup by vm.backupAuto.collectAsState(true)
    // #22: estado do sync.
    val endpoint by vm.endpointUrl.collectAsState("")
    val email by vm.ownerEmail.collectAsState("")
    val pendentes by vm.pendentes.collectAsState(0)
    var endpointTxt by remember(endpoint) { mutableStateOf(endpoint) }
    var emailTxt by remember(email) { mutableStateOf(email) }
    val endpointOk = endpointTxt.isBlank() || SyncPayload.urlValida(endpointTxt)
    val emailOk = emailTxt.isBlank() || SyncPayload.emailValido(emailTxt)

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Tema escuro"); Switch(checked = escuro, onCheckedChange = { vm.tema(it) })
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Backup automático semanal"); Switch(checked = backup, onCheckedChange = { vm.backup(it) })
        }
        Text("Backup salva JSON em Downloads a cada 7 dias ao abrir o app.", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        // #22: sync online (outbox offline).
        Text("Sincronização online", style = MaterialTheme.typography.titleMedium)
        Text(
            "Sem endpoint, o app segue 100% offline. Com endpoint https, eventos enfileirados são enviados ao abrir rede.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = endpointTxt,
            onValueChange = { endpointTxt = it },
            label = { Text("Endpoint (URL https)") },
            placeholder = { Text("https://script.google.com/…/exec") },
            singleLine = true,
            isError = !endpointOk,
            supportingText = {
                if (!endpointOk) Text("Use URL https válida")
            },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = emailTxt,
            onValueChange = { emailTxt = it },
            label = { Text("E-mail do dono") },
            placeholder = { Text("voce@exemplo.com") },
            singleLine = true,
            isError = !emailOk,
            supportingText = {
                if (!emailOk) Text("E-mail precisa de @")
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    vm.salvarEndpoint(endpointTxt.trim())
                    vm.salvarEmail(emailTxt.trim())
                    vm.enviarAgora()
                },
                enabled = endpointOk && emailOk,
                modifier = Modifier.weight(1f)
            ) { Text("Salvar e enviar agora") }
            OutlinedButton(
                onClick = { vm.enviarAgora() },
                modifier = Modifier.weight(1f)
            ) { Text("Enviar agora") }
        }
        Text(
            "$pendentes evento(s) pendente(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
