package com.finzen.app.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.repository.ThemeMode
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.FilterChipPill
import com.finzen.app.ui.components.SectionHeader
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val lastAutoBackupAt by viewModel.lastAutoBackupAt.collectAsStateWithLifecycle()
    val capturePurchasesEnabled by viewModel.capturePurchasesEnabled.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var nameDraft by remember { mutableStateOf<String?>(null) }

    var showClearDialog by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val diagPrefs = remember { context.getSharedPreferences("finzen_diag", Context.MODE_PRIVATE) }
    var lastCrash by remember { mutableStateOf(diagPrefs.getString("last_crash", null)) }

    var notificationAccessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    LifecycleResumeEffect(Unit) {
        notificationAccessGranted = isNotificationAccessGranted(context)
        onPauseOrDispose { }
    }
    val postNotificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importBackup(it) } }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.enableAutoBackup(uri.toString())
        }
    }

    val safeLaunch: (() -> Unit) -> Unit = { action ->
        try {
            action()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Nenhum app de arquivos disponível neste dispositivo.", Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            Toast.makeText(context, "Não foi possível abrir: ${t.message ?: t.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            item {
                SettingsSection(title = "Perfil") {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Seu nome",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Aparece na saudação da tela inicial.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = nameDraft ?: userName,
                            onValueChange = {
                                nameDraft = it
                                viewModel.setUserName(it)
                            },
                            placeholder = { Text("Ex: Allan") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (lastCrash != null) {
                item {
                    SettingsSection(title = "Diagnóstico") {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "O app registrou um erro recente. Copie e me envie para eu corrigir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = lastCrash.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(lastCrash.orEmpty()))
                                    Toast.makeText(context, "Erro copiado.", Toast.LENGTH_SHORT).show()
                                }) { Text("Copiar erro") }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = {
                                    diagPrefs.edit().remove("last_crash").remove("last_crash_at").apply()
                                    lastCrash = null
                                }) { Text("Limpar") }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Aparência") {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Tema do aplicativo",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Escolha como o Error404Saldo deve se adaptar ao seu dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ThemeOption(
                                label = "Sistema",
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setTheme(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeOption(
                                label = "Claro",
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setTheme(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeOption(
                                label = "Escuro",
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setTheme(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Segurança") {
                    SettingsRow(
                        icon = Icons.Rounded.Fingerprint,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Bloqueio por digital",
                        subtitle = "Pede sua impressão digital ao abrir o app.",
                        trailing = {
                            Switch(checked = biometricEnabled, onCheckedChange = null)
                        },
                        onClick = {
                            val enabling = !biometricEnabled
                            val canAuth = if (enabling) {
                                BiometricManager.from(context)
                                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                            } else {
                                BiometricManager.BIOMETRIC_SUCCESS
                            }
                            if (enabling && canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                                Toast.makeText(
                                    context,
                                    "Nenhuma biometria cadastrada. Configure a impressão digital nas configurações do aparelho.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                viewModel.setBiometricLock(enabling)
                            }
                        },
                    )
                }
            }

            item {
                SettingsSection(title = "Captura de compras") {
                    Column {
                        SettingsRow(
                            icon = Icons.Rounded.NotificationsActive,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Capturar compras automaticamente",
                            subtitle = capturePurchasesSubtitle(capturePurchasesEnabled, notificationAccessGranted),
                            trailing = { Switch(checked = capturePurchasesEnabled, onCheckedChange = null) },
                            onClick = {
                                if (capturePurchasesEnabled) {
                                    viewModel.setCapturePurchases(false)
                                } else {
                                    viewModel.setCapturePurchases(true)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    if (!notificationAccessGranted) {
                                        openNotificationAccessSettings(context)
                                    }
                                }
                            },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.OpenInNew,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Acesso às notificações",
                            subtitle = if (notificationAccessGranted) {
                                "Permissão concedida."
                            } else {
                                "Necessário para o app ler as notificações do banco."
                            },
                            trailing = { Chevron() },
                            onClick = { openNotificationAccessSettings(context) },
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Dados") {
                    Column {
                        SettingsRow(
                            icon = Icons.Rounded.PlaylistAdd,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Carregar dados de exemplo",
                            subtitle = "Adiciona transações de demonstração dos últimos meses.",
                            enabled = !isBusy,
                            trailing = {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Chevron()
                                }
                            },
                            onClick = { viewModel.loadSampleData() },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Backup,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Backup automático",
                            subtitle = autoBackupSubtitle(autoBackupEnabled, lastAutoBackupAt),
                            trailing = { Switch(checked = autoBackupEnabled, onCheckedChange = null) },
                            onClick = {
                                if (autoBackupEnabled) {
                                    viewModel.disableAutoBackup()
                                } else {
                                    safeLaunch { folderLauncher.launch(null) }
                                }
                            },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Save,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Exportar backup",
                            subtitle = "Salva todos os seus dados em um arquivo .json que você guarda onde quiser.",
                            enabled = !isBusy,
                            trailing = { Chevron() },
                            onClick = { safeLaunch { exportLauncher.launch(backupFileName()) } },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Restore,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Importar backup",
                            subtitle = "Restaura os dados de um arquivo. Substitui o que está no app.",
                            enabled = !isBusy,
                            trailing = { Chevron() },
                            onClick = { showImportConfirm = true },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.DeleteSweep,
                            iconTint = FinanceTheme.colors.expense,
                            title = "Apagar todas as transações",
                            subtitle = "Remove os lançamentos, mantendo contas e categorias.",
                            titleColor = FinanceTheme.colors.expense,
                            enabled = !isBusy,
                            trailing = { Chevron() },
                            onClick = { showClearDialog = true },
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Sobre") {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "E",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Error404Saldo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Controle financeiro premium",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "v1.0.0",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "O Error404Saldo ajuda você a organizar receitas, despesas, contas e " +
                                "cartões em um só lugar, com relatórios claros e um visual elegante. " +
                                "Seus dados ficam salvos somente neste dispositivo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = FinanceTheme.colors.expense,
                )
            },
            title = { Text("Apagar transações?") },
            text = {
                Text(
                    "Todos os lançamentos serão removidos permanentemente. " +
                        "Suas contas, cartões e categorias serão mantidos. " +
                        "Esta ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearTransactions()
                    },
                ) {
                    Text("Apagar", color = FinanceTheme.colors.expense)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            icon = {
                Icon(
                    Icons.Rounded.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text("Importar backup?") },
            text = {
                Text(
                    "Os dados atuais do app serão substituídos pelos dados do arquivo " +
                        "escolhido. Esta ação não pode ser desfeita.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    safeLaunch { importLauncher.launch(arrayOf("*/*")) }
                }) { Text("Escolher arquivo") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        SectionHeader(title = title, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChipPill(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

private fun backupFileName(): String {
    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale("pt", "BR"))
        .format(java.util.Date())
    return "error404saldo-backup-$stamp.json"
}

private fun capturePurchasesSubtitle(enabled: Boolean, accessGranted: Boolean): String = when {
    !enabled -> "Lê as notificações do banco e sugere lançar a compra. Você confirma antes de salvar."
    !accessGranted -> "Ative o \"Acesso às notificações\" abaixo para funcionar."
    else -> "Ativo • toque na notificação de compra para lançar."
}

private fun isNotificationAccessGranted(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun openNotificationAccessSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun autoBackupSubtitle(enabled: Boolean, lastAt: Long): String {
    if (!enabled) return "Salva uma cópia sozinho quando você abre o app."
    if (lastAt <= 0L) return "Ativo • aguardando o primeiro backup."
    val label = java.text.SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", java.util.Locale("pt", "BR"))
        .format(java.util.Date(lastAt))
    return "Ativo • último: $label"
}
