package com.example.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncBottomSheet(
    onDismissRequest: () -> Unit,
    shifts: List<RideShift>,
    fixedExpenses: List<FixedExpense>,
    categories: List<FinanceCategory>,
    repository: FinanceRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by FirebaseSyncManager.currentUser.collectAsState()
    val syncState by FirebaseSyncManager.syncState.collectAsState()

    var showRestoreWarning by remember { mutableStateOf(false) }

    // Configuração do Launcher de Google Sign In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                coroutineScope.launch {
                    val user = FirebaseSyncManager.signInWithGoogle(idToken)
                    if (user != null) {
                        FirebaseSyncManager.syncOnLogin(repository, shifts, fixedExpenses, categories)
                    }
                }
            } else {
                FirebaseSyncManager.setSyncStatus(
                    SyncStatus.Error("Falha: Token ID retornado pelo Google é nulo. A configuração OAuth pode estar incompleta no Firebase.")
                )
            }
        } catch (e: ApiException) {
            LogUtils.e("CloudSyncBottomSheet", "Erro de login com o Google (API)", e)
            val code = e.statusCode
            var hint = "Verifique as configurações do Firebase."
            if (code == 10) {
                hint = "Código 10 (DEVELOPER_ERROR). Isso significa que a assinatura (SHA-1) deste aplicativo gerado no ambiente AI Studio não está registrada na console do Firebase, ou o login do Google não está ativo nas configurações do Firebase Authentication."
            } else if (code == 12500) {
                hint = "Código 12500 (Sign-In Cancelado). Verifique se o Google Play Services está ativo no celular/emulador e conectado."
            }
            FirebaseSyncManager.setSyncStatus(
                SyncStatus.Error("Erro no Google Sign-In (Status $code): $hint")
            )
        } catch (e: Exception) {
            LogUtils.e("CloudSyncBottomSheet", "Erro de login com o Google", e)
            FirebaseSyncManager.setSyncStatus(
                SyncStatus.Error("Erro inesperado: ${e.localizedMessage ?: "Consulte o console/suporte"}")
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("cloud_sync_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Ícone Nuvem",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Backup em Nuvem",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sync States Messages (Loading / Error / Success)
            AnimatedVisibility(
                visible = syncState != SyncStatus.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                when (val state = syncState) {
                    is SyncStatus.Loading -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    is SyncStatus.Error -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = "Erro de Sincronização",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    is SyncStatus.Success -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = Color(0xFFE8F5E9).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Sucesso",
                                    tint = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            val user = currentUser
            if (user == null) {
                // --- DESLOGADO ---
                val isConfigured = remember(context) { FirebaseSyncManager.isGoogleConfigured(context) }
                val clipboardManager = LocalClipboardManager.current
                var showHelp by remember { mutableStateOf(!isConfigured) }

                Text(
                    text = "Proteja os seus dados financeiros! Ao fazer login com o Google, você pode salvar os seus turnos, custos mensais e categorias personalizadas com total segurança na nuvem Firebase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (!isConfigured) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Configuração incompleta",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google Sign-In Pendente",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Seu arquivo 'google-services.json' não possui clientes OAuth registrados. Para que o login funcione, adicione a assinatura SHA-1 desta visualização no Console do Firebase e envie o novo arquivo modificando a pasta '/app'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Card de informações de segurança e assinaturas SHA
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHelp = !showHelp },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Chaves do App",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Como Configurar / Ver Chaves SHA",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (showHelp) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (showHelp) "Esconder" else "Visualizar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showHelp) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = "Impressão Digital SHA-1 (Obrigatório para login):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "4C:30:EE:46:8C:4D:73...CD:6E",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Button(
                                        onClick = {
                                            clipboardManager?.setText(
                                                AnnotatedString("4C:30:EE:46:8C:4D:73:D0:90:A5:4E:BB:61:F4:70:56:28:57:CD:6E")
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Text("Copiar SHA-1", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Impressão Digital SHA-256 (Recomendado):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "14:A3:10:25:9D:E1...96:0D",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Button(
                                        onClick = {
                                            clipboardManager?.setText(
                                                AnnotatedString("14:A3:10:25:9D:E1:95:63:B2:83:72:56:49:50:F4:41:0F:1C:8C:31:8A:4F:A9:A6:8F:52:39:4D:05:48:96:0D")
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Text("Copiar SHA-256", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                )

                                Text(
                                    text = "Passo-a-passo para ativar:\n" +
                                            "1. Abra o Console do Firebase e clique na engrenagem de Configurações do Projeto.\n" +
                                            "2. Em 'Seus aplicativos', clique no botão 'Adicionar impressão digital' e insira a chave SHA-1 copiada acima.\n" +
                                            "3. Acesse a barra lateral 'Authentication' -> Guia 'Sign-in method' -> Adicionar provedor -> 'Google' e deixe ativado.\n" +
                                            "4. Baixe o arquivo google-services.json atualizado.\n" +
                                            "5. Envie o novo arquivo pelo editor para a pasta /app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val client = FirebaseSyncManager.getGoogleSignInClient(context)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Google Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Entrar com Conta Google",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // --- LOGADO ---
                // Card de Usuário
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar estilizado com gradiente
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = (user.displayName ?: user.email ?: "?")
                                .take(1)
                                .uppercase()
                            Text(
                                text = initial,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = user.displayName ?: "Motorista Parceiro",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = user.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Text(
                    text = "Selecione uma ação para sincronizar as informações locais do aplicativo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
                )

                // Botão de Backup para Nuvem
                Button(
                    onClick = {
                        coroutineScope.launch {
                            FirebaseSyncManager.backupToCloud(shifts, fixedExpenses, categories)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 10.dp)
                        .testTag("backup_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Fazer Backup"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Fazer Backup (Salvar na Nuvem)",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Botão de Restauração da Nuvem
                OutlinedButton(
                    onClick = { showRestoreWarning = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = "Recuperar Backup"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recuperar Backup da Nuvem",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logout
                TextButton(
                    onClick = { FirebaseSyncManager.logout() },
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Sair",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sair da Conta Google",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal de confirmação para restauração
    if (showRestoreWarning) {
        AlertDialog(
            onDismissRequest = { showRestoreWarning = false },
            title = {
                Text(
                    text = "Atenção: Substituir Dados?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Ao recuperar o backup da nuvem, TODOS os turnos, custos lançados e tipos registrados neste celular serão apagados e substituídos pelos dados da nuvem. Deseja continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreWarning = false
                        coroutineScope.launch {
                            FirebaseSyncManager.restoreFromCloud(repository)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sim, Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreWarning = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

object LogUtils {
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        Log.e(tag, msg, tr)
    }
}
