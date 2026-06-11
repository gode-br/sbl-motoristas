package com.example.ui

import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceCategory
import com.example.data.RideShift
import com.example.data.InDriveRide
import java.util.Locale
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke



@Composable
fun ShiftsScreen(
    shifts: List<RideShift>,
    categories: List<FinanceCategory>,
    onAddShift: (Map<String, Double>, Map<String, Double>, Double, Double, String, Long) -> Unit,
    onDeleteShift: (Int) -> Unit,
    onStartActiveShift: (Double, Double, Double, Long) -> Unit,
    onAddEarningToActiveShift: (Int, String, Double) -> Unit,
    onAddCostToActiveShift: (Int, String, Double) -> Unit,
    onUpdateShiftDetails: (Int, Double, Double, String, Long?) -> Unit,
    onCloseShift: (Int, Double, Double, String, Double, Double) -> Unit,
    onRemoveEarningFromActiveShift: (Int, String) -> Unit = { _, _ -> },
    onEditEarningInActiveShift: (Int, String, Double) -> Unit = { _, _, _ -> },
    onRemoveCostFromActiveShift: (Int, String) -> Unit = { _, _ -> },
    onEditCostInActiveShift: (Int, String, Double) -> Unit = { _, _, _ -> },
    onRemoveInDriveRideFromActiveShift: (Int, Long) -> Unit = { _, _ -> },
    onEditInDriveRideInActiveShift: (Int, Long, Double, Boolean) -> Unit = { _, _, _, _ -> },
    onEditStartingCashInShift: (Int, Double) -> Unit = { _, _ -> },
    onEditEndCashInShift: (Int, Double) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showHistoricalAddDialog by remember { mutableStateOf(false) }
    var shiftToEdit by remember { mutableStateOf<RideShift?>(null) }
    var shiftToDelete by remember { mutableStateOf<RideShift?>(null) }

    val activeShift = remember(shifts) { shifts.firstOrNull { it.isOpen } }

    Box(modifier = modifier.fillMaxSize()) {
        if (activeShift != null) {
            ActiveShiftDashboard(
                shift = activeShift,
                categories = categories,
                onAddEarning = { cat, valAmount -> onAddEarningToActiveShift(activeShift.id, cat, valAmount) },
                onAddCost = { cat, valAmount -> onAddCostToActiveShift(activeShift.id, cat, valAmount) },
                onUpdateDetails = { km, hr, notes, date -> onUpdateShiftDetails(activeShift.id, km, hr, notes, date) },
                onCloseShift = { km, hr, notes, endOdm, endCash -> onCloseShift(activeShift.id, km, hr, notes, endOdm, endCash) },
                onRemoveEarning = { cat -> onRemoveEarningFromActiveShift(activeShift.id, cat) },
                onEditEarning = { cat, valAmount -> onEditEarningInActiveShift(activeShift.id, cat, valAmount) },
                onRemoveCost = { cat -> onRemoveCostFromActiveShift(activeShift.id, cat) },
                onEditCost = { cat, valAmount -> onEditCostInActiveShift(activeShift.id, cat, valAmount) },
                onRemoveInDriveRide = { ts -> onRemoveInDriveRideFromActiveShift(activeShift.id, ts) },
                onEditInDriveRide = { ts, valAmount, cancelled -> onEditInDriveRideInActiveShift(activeShift.id, ts, valAmount, cancelled) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("shifts_list")
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "Controle de Turnos",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gerencie seus turnos de corrida. Inicie um turno ativo para abrir o caixa e registrar corridas em tempo real.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            onClick = { showOpenShiftDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_new_shift_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF101935)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF00FF66).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Iniciar",
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Iniciar Turno",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Abre caixa hoje",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Card(
                            onClick = { showHistoricalAddDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_historical_shift_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddModerator,
                                        contentDescription = "Lançamento Avulso",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Turno Avulso",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Registrar anterior",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Turnos Finalizados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val closedShifts = shifts.filter { !it.isOpen }
                if (closedShifts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhum histórico disponível",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(closedShifts, key = { it.id }) { shift ->
                        ClosedShiftItemCard(
                            shift = shift,
                            onEditClick = { shiftToEdit = shift },
                            onDeleteClick = { shiftToDelete = shift },
                            onRemoveEarning = onRemoveEarningFromActiveShift,
                            onEditEarning = onEditEarningInActiveShift,
                            onRemoveCost = onRemoveCostFromActiveShift,
                            onEditCost = onEditCostInActiveShift,
                            onRemoveInDriveRide = onRemoveInDriveRideFromActiveShift,
                            onEditInDriveRide = onEditInDriveRideInActiveShift,
                            onEditStartingCash = onEditStartingCashInShift,
                            onEditEndCash = onEditEndCashInShift
                        )
                    }
                }
            }
        }

        if (showOpenShiftDialog) {
            val lastClosedShift = remember(shifts) {
                shifts.filter { !it.isOpen }.maxByOrNull { it.date }
            }
            val defaultStartingCash = lastClosedShift?.endCash ?: 0.0

            OpenShiftDialog(
                defaultStartingCash = defaultStartingCash,
                onDismiss = { showOpenShiftDialog = false },
                onConfirm = { cash, accounts, startOdm, date ->
                    onStartActiveShift(cash, accounts, startOdm, date)
                    showOpenShiftDialog = false
                }
            )
        }

        if (showHistoricalAddDialog) {
            AddShiftDialog(
                categories = categories,
                onDismiss = { showHistoricalAddDialog = false },
                onConfirm = { earnings, costs, km, hours, notes, date ->
                    onAddShift(earnings, costs, km, hours, notes, date)
                    showHistoricalAddDialog = false
                }
            )
        }

        if (shiftToEdit != null) {
            val shift = shiftToEdit!!
            EditShiftDialog(
                shift = shift,
                categories = categories,
                onDismiss = { shiftToEdit = null },
                onConfirm = { updatedShift ->
                    onDeleteShift(shift.id)
                    onAddShift(
                        updatedShift.getEarningsMap(),
                        updatedShift.getCostsMap(),
                        updatedShift.kmDriven,
                        updatedShift.hoursWorked,
                        updatedShift.notes,
                        updatedShift.date
                    )
                    shiftToEdit = null
                },
                onDelete = {
                    onDeleteShift(shift.id)
                    shiftToEdit = null
                }
            )
        }

        if (shiftToDelete != null) {
            val shift = shiftToDelete!!
            AlertDialog(
                onDismissRequest = { shiftToDelete = null },
                modifier = Modifier.testTag("delete_shift_confirm_dialog"),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Excluir Turno",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Text(
                        text = "Tem certeza de que deseja apagar o registro desse turno do histórico? Essa operação removerá o faturamento e os gastos correspondentes permanentemente e não poderá ser desfeita.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteShift(shift.id)
                            shiftToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_delete_shift_btn")
                    ) {
                        Text("Excluir Turno")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { shiftToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ActiveShiftDashboard(
    shift: RideShift,
    categories: List<FinanceCategory>,
    onAddEarning: (String, Double) -> Unit,
    onAddCost: (String, Double) -> Unit,
    onUpdateDetails: (Double, Double, String, Long?) -> Unit,
    onCloseShift: (Double, Double, String, Double, Double) -> Unit,
    onRemoveEarning: (String) -> Unit = {},
    onEditEarning: (String, Double) -> Unit = { _, _ -> },
    onRemoveCost: (String) -> Unit = {},
    onEditCost: (String, Double) -> Unit = { _, _ -> },
    onRemoveInDriveRide: (Long) -> Unit = {},
    onEditInDriveRide: (Long, Double, Boolean) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val earningsMap = shift.getEarningsMap()
    val costsMap = shift.getCostsMap()

    var selectedEarningCat by remember { mutableStateOf(categories.firstOrNull { it.type == "EARNING" }?.name ?: "Corrida") }
    var selectedCostCat by remember { mutableStateOf(categories.firstOrNull { it.type == "COST" }?.name ?: "Combustível") }

    var earningInput by remember { mutableStateOf(TextFieldValue("")) }
    var costInput by remember { mutableStateOf(TextFieldValue("")) }

    var kmText by remember { mutableStateOf(if (shift.kmDriven > 0.0) shift.kmDriven.toString() else "") }
    var hrText by remember { mutableStateOf(if (shift.hoursWorked > 0.0) shift.hoursWorked.toString() else "") }
    var notesText by remember { mutableStateOf(shift.notes) }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    var editingEarningCat by remember { mutableStateOf<String?>(null) }
    var editingEarningValue by remember { mutableStateOf(TextFieldValue("")) }

    var editingCostCat by remember { mutableStateOf<String?>(null) }
    var editingCostValue by remember { mutableStateOf(TextFieldValue("")) }

    var editingRide by remember { mutableStateOf<InDriveRide?>(null) }
    var editingRideValue by remember { mutableStateOf(TextFieldValue("")) }
    var editingRideIsCancelled by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isSyncingInDrive by remember { mutableStateOf(false) }
    var inDriveSyncStatus by remember { mutableStateOf("") }
    
    var syncedInDriveRideValue by remember { mutableStateOf<Double?>(null) }
    var syncedInDriveRoute by remember { mutableStateOf<String?>(null) }
    var syncedInDriveKm by remember { mutableStateOf<Double?>(null) }
    var syncedInDriveTime by remember { mutableStateOf<Double?>(null) }
    var selectedInDriveSyncType by remember { mutableStateOf("") } // "API" or "CLIPBOARD"
    
    var clipboardFetchMessage by remember { mutableStateOf("") }
    var clipboardDetectedValue by remember { mutableStateOf<Double?>(null) }
    
    var showImportSuccessDialog by remember { mutableStateOf(false) }
    var lastImportedValue by remember { mutableStateOf(0.0) }

    var shouldAccrueDistanceAndTime by remember { mutableStateOf(true) }
    val inDriveFeePercentage = 11.3
    var isAutoImportActive by remember { mutableStateOf(true) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var endCashInput by remember { mutableStateOf(TextFieldValue("")) }
    var endOdometerInput by remember { mutableStateOf(TextFieldValue("")) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAccessibilityEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isAccessibilityEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val totalCurrentEarnings = earningsMap.values.sum()
    val totalCurrentCosts = costsMap.values.sum()
    val currentBalance = shift.startingCash + shift.startingAccounts + totalCurrentEarnings - totalCurrentCosts

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_shift_dashboard")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101935))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF00FF66))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TURNO ATIVO",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF66)
                            )
                        }

                        IconButton(
                            onClick = { showDatePickerDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Alterar Data do Turno",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Iniciado em: ${FormatUtils.formatDate(shift.date)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "DINHEIRO/TROCO INICIAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(shift.startingCash),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (shift.startOdometer > 0.0 || shift.startingAccounts == 0.0) {
                            Column {
                                Text(
                                    text = "KM INICIAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "${shift.startOdometer} km",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Column {
                                Text(
                                    text = "CONTAS INICIAIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(shift.startingAccounts),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PROJEÇÃO SALDO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = FormatUtils.formatCurrency(currentBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (currentBalance >= (shift.startingCash + shift.startingAccounts)) Color(0xFF00FF66) else Color(0xFFFF3B30)
                            )
                        }
                    }
                }
            }
        }

        item {
            if (!isAccessibilityEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("accessibility_permission_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E14)),
                    border = BorderStroke(1.dp, Color(0xFFFF9500))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Aviso",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rastreamento Automático Inativo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9500)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ative o serviço de acessibilidade 'ScreenMonitorAccessibilityService' nas configurações para faturar corridas do inDrive automaticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("activate_accessibility_btn")
                            ) {
                                Text("ATIVAR NAS CONFIGURAÇÕES", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("accessibility_active_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2418)),
                    border = BorderStroke(1.dp, Color(0xFF00FF66))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Ativo",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Rastreamento Automático Ativo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF66)
                            )
                            Text(
                                text = "Monitorando corridas em andamento do inDrive em tempo real.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lançamentos Rápidos no Caixa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column {
                        Text(
                            text = "REGISTRAR GANHO OBTIDO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                var expanded by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = selectedEarningCat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    categories.filter { it.type == "EARNING" }.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                selectedEarningCat = cat.name
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = earningInput,
                                onValueChange = { earningInput = TextFieldValue(FormatUtils.formatInputAsCurrency(it.text), selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)) },
                                placeholder = { Text("Valor R$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1.2f).testTag("quick_earning_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            IconButton(
                                onClick = {
                                    val amount = FormatUtils.parseCurrency(earningInput.text)
                                    if (amount > 0.0) {
                                        onAddEarning(selectedEarningCat, amount)
                                        earningInput = TextFieldValue("")
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF00FF66))
                                    .testTag("quick_earning_submit")
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        Text(
                            text = "REGISTRAR DESPESA / GASTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF007A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                var expanded by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = selectedCostCat,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    categories.filter { it.type == "COST" }.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.name) },
                                            onClick = {
                                                selectedCostCat = cat.name
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = costInput,
                                onValueChange = { costInput = TextFieldValue(FormatUtils.formatInputAsCurrency(it.text), selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)) },
                                placeholder = { Text("Valor R$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1.2f).testTag("quick_cost_input"),
                                shape = RoundedCornerShape(8.dp)
                            )

                            IconButton(
                                onClick = {
                                    val amount = FormatUtils.parseCurrency(costInput.text)
                                    if (amount > 0.0) {
                                        onAddCost(selectedCostCat, amount)
                                        costInput = TextFieldValue("")
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF007A))
                                    .testTag("quick_cost_submit")
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (earningsMap.isNotEmpty() || costsMap.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Fluxo Acumulado no Turno",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque em um item para editar ou excluir",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (earningsMap.isNotEmpty()) {
                            Text(
                                text = "Ganhos Acumulados:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF66)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            earningsMap.forEach { (cat, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            editingEarningCat = cat
                                            val rawCentsStr = String.format(java.util.Locale.US, "%.2f", amount).replace(".", "")
                                            val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                            editingEarningValue = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color(0xFF00FF66).copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        text = "+ ${FormatUtils.formatCurrency(amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF66)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (costsMap.isNotEmpty()) {
                            Text(
                                text = "Custos Acumulados:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF007A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            costsMap.forEach { (cat, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            editingCostCat = cat
                                            val rawCentsStr = String.format(java.util.Locale.US, "%.2f", amount).replace(".", "")
                                            val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                            editingCostValue = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color(0xFFFF007A).copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        text = "- ${FormatUtils.formatCurrency(amount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF007A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val inDriveRides = shift.getInDriveRides()
            val completedRides = inDriveRides.filter { !it.isCancelled }
            val totalGross = completedRides.sumOf { it.value }
            val totalFee = (totalGross * 11.3) / 100.0
            val totalNet = totalGross - totalFee

            Card(
                modifier = Modifier.fillMaxWidth().testTag("indrive_balance_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_car_night),
                                contentDescription = "inDrive",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Balanço Diário - inDrive",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00FF66).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${completedRides.size} concluídas",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF66)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "VALOR BRUTO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(totalGross),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "TAXAS (11.3%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "- ${FormatUtils.formatCurrency(totalFee)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF007A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00FF66).copy(alpha = 0.12f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VALOR LÍQUIDO ACUMULADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                        Text(
                            text = FormatUtils.formatCurrency(totalNet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00FF66)
                        )
                    }
                }
            }
        }

        item {
            val inDriveRides = shift.getInDriveRides()
            Card(
                modifier = Modifier.fillMaxWidth().testTag("indrive_rides_made_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Histórico de Corridas Coletadas (Turno)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (inDriveRides.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Aguardando",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma corrida coletada ainda.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Inicie corridas no aplicativo inDrive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            inDriveRides.reversed().forEachIndexed { index, ride ->
                                val formattedTime = try {
                                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(ride.timestamp))
                                } catch (e: Exception) {
                                    "--:--"
                                }

                                val cardBgColor = if (ride.isCancelled) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }

                                val accentColor = if (ride.isCancelled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color(0xFF00FF66)
                                }

                                val iconBgColor = if (ride.isCancelled) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                } else {
                                    Color(0xFF00FF66).copy(alpha = 0.15f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cardBgColor)
                                        .clickable {
                                            editingRide = ride
                                            val rawCentsStr = String.format(java.util.Locale.US, "%.2f", ride.value).replace(".", "")
                                            val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                            editingRideValue = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                            editingRideIsCancelled = ride.isCancelled
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(iconBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${inDriveRides.size - index}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (ride.isCancelled) "Corrida Cancelada / Não Finalizada" else "Corrida Detectada pelo Monitor",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (ride.isCancelled) "Cancelada às $formattedTime" else "Coletada às $formattedTime",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (ride.isCancelled) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (ride.isCancelled) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = FormatUtils.formatCurrency(ride.originalValue),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "R$ 0,00",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = FormatUtils.formatCurrency(ride.value),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FF66)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    showCloseShiftDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("close_active_shift_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Stop, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENCERRAR TURNO",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }

    if (editingEarningCat != null) {
        val cat = editingEarningCat!!
        AlertDialog(
            onDismissRequest = { editingEarningCat = null },
            modifier = Modifier.testTag("edit_earning_dialog"),
            title = { Text("Editar Ganho", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajuste o valor para a categoria $cat:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingEarningValue,
                        onValueChange = { 
                            editingEarningValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor Acumulado ($cat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_earning_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRemoveEarning(cat)
                            editingEarningCat = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingEarningCat = null }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingEarningValue.text)
                                onEditEarning(cat, value)
                                editingEarningCat = null
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (editingCostCat != null) {
        val cat = editingCostCat!!
        AlertDialog(
            onDismissRequest = { editingCostCat = null },
            modifier = Modifier.testTag("edit_cost_dialog"),
            title = { Text("Editar Custo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajuste o valor para a despesa $cat:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingCostValue,
                        onValueChange = { 
                            editingCostValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor Acumulado ($cat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_cost_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRemoveCost(cat)
                            editingCostCat = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingCostCat = null }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingCostValue.text)
                                onEditCost(cat, value)
                                editingCostCat = null
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (editingRide != null) {
        val ride = editingRide!!
        AlertDialog(
            onDismissRequest = { editingRide = null },
            modifier = Modifier.testTag("edit_ride_dialog"),
            title = { Text("Editar Corrida inDrive", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ajuste o valor desta corrida inDrive ou mude seu status de cancelamento:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingRideValue,
                        onValueChange = { 
                            editingRideValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor da Corrida (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_ride_value_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marcar como Cancelada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = editingRideIsCancelled,
                            onCheckedChange = { editingRideIsCancelled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRemoveInDriveRide(ride.timestamp)
                            editingRide = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingRide = null }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingRideValue.text)
                                onEditInDriveRide(ride.timestamp, value, editingRideIsCancelled)
                                editingRide = null
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (showDatePickerDialog) {
        val calendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = shift.date } }
        CustomCalendarDialog(
            initialDay = calendar.get(java.util.Calendar.DAY_OF_MONTH),
            initialMonthIndex = calendar.get(java.util.Calendar.MONTH),
            onDateSelected = { day, monthIndex ->
                val newCal = java.util.Calendar.getInstance().apply {
                    timeInMillis = shift.date
                    set(java.util.Calendar.DAY_OF_MONTH, day)
                    set(java.util.Calendar.MONTH, monthIndex)
                }
                val d = 0.0
                val h = hrText.toDoubleOrNull() ?: 0.0
                onUpdateDetails(d, h, "", newCal.timeInMillis)
                showDatePickerDialog = false
            },
            onDismissRequest = { showDatePickerDialog = false }
        )
    }

    if (showCloseShiftDialog) {
        val isCloseFormValid = endCashInput.text.isNotBlank() && 
                endOdometerInput.text.isNotBlank() && 
                endOdometerInput.text.toDoubleOrNull() != null

        AlertDialog(
            onDismissRequest = { showCloseShiftDialog = false },
            modifier = Modifier.testTag("close_shift_dialog"),
            title = {
                Text(
                    text = "Encerrar Turno com Caixa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Informe o valor em dinheiro final que você possui em mãos e a quilometragem final (km) para fechar o caixa e o rendimento deste turno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = endCashInput,
                        onValueChange = { 
                            endCashInput = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor em Dinheiro Final (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("end_cash_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = endOdometerInput,
                        onValueChange = { endOdometerInput = it },
                        label = { Text("Kilometragem / Odômetro Final (km) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("end_odometer_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isCloseFormValid) {
                        Text(
                            text = "* Dinheiro final e Odômetro final são obrigatórios.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hoursVal = hrText.toDoubleOrNull() ?: 0.0
                        val cashVal = FormatUtils.parseCurrency(endCashInput.text)
                        val odometerVal = endOdometerInput.text.toDoubleOrNull() ?: 0.0
                        onCloseShift(0.0, hoursVal, "", odometerVal, cashVal)
                        showCloseShiftDialog = false
                    },
                    enabled = isCloseFormValid,
                    modifier = Modifier.testTag("confirm_close_shift_btn")
                ) {
                    Text("Encerrar Turno")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseShiftDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showImportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showImportSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Sucesso",
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Unidade Concluída!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                val lastFee = (lastImportedValue * inDriveFeePercentage) / 100.0
                val lastNet = lastImportedValue - lastFee
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A corrida no valor bruto de " + FormatUtils.formatCurrency(lastImportedValue) + " foi adicionada com sucesso aos ganhos da categoria 'InDrive' neste turno ativo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Também registramos a taxa de " + String.format(java.util.Locale.US, "%.1f", inDriveFeePercentage) + "% (" + FormatUtils.formatCurrency(lastFee) + ") como custo do turno em 'Taxa inDrive'.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Faturamento Líquido desta Corrida: " + FormatUtils.formatCurrency(lastNet),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF66)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showImportSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black)
                ) {
                    Text(text = "Excelente!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun ClosedShiftItemCard(
    shift: RideShift,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRemoveEarning: (Int, String) -> Unit,
    onEditEarning: (Int, String, Double) -> Unit,
    onRemoveCost: (Int, String) -> Unit,
    onEditCost: (Int, String, Double) -> Unit,
    onRemoveInDriveRide: (Int, Long) -> Unit,
    onEditInDriveRide: (Int, Long, Double, Boolean) -> Unit,
    onEditStartingCash: (Int, Double) -> Unit,
    onEditEndCash: (Int, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    var editingEarningCat by remember { mutableStateOf<String?>(null) }
    var editingEarningValue by remember { mutableStateOf(TextFieldValue("")) }

    var editingCostCat by remember { mutableStateOf<String?>(null) }
    var editingCostValue by remember { mutableStateOf(TextFieldValue("")) }

    var editingRide by remember { mutableStateOf<InDriveRide?>(null) }
    var editingRideValue by remember { mutableStateOf(TextFieldValue("")) }
    var editingRideIsCancelled by remember { mutableStateOf(false) }

    var showEditStartingCashDialog by remember { mutableStateOf(false) }
    var editingStartingCashValue by remember { mutableStateOf(TextFieldValue("")) }

    var showEditEndCashDialog by remember { mutableStateOf(false) }
    var editingEndCashValue by remember { mutableStateOf(TextFieldValue("")) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shift_item_${shift.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = FormatUtils.formatDate(shift.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("edit_shift_btn_${shift.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar turno",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            if (shift.startingCash > 0.0 || shift.startingAccounts > 0.0 || shift.startOdometer > 0.0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val startingAccountsText = if (shift.startingAccounts > 0.0) " • Contas ${FormatUtils.formatCurrency(shift.startingAccounts)}" else ""
                    val startingOdometerText = if (shift.startOdometer > 0.0) " • Odômetro Inicial: ${shift.startOdometer} km" else ""
                    Text(
                        text = "Caixa Inicial: Dinheiro/Troco ${FormatUtils.formatCurrency(shift.startingCash)}$startingAccountsText$startingOdometerText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (shift.endCash > 0.0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Caixa Final: Dinheiro ${FormatUtils.formatCurrency(shift.endCash)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "R$ GANHOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = FormatUtils.formatCurrency(shift.totalEarnings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00FF66))
                }

                Column {
                    Text(text = "R$ CUSTOS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = FormatUtils.formatCurrency(shift.totalCosts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF007A))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Ocultar Lançamentos" else "Ver Lançamentos e Editar",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val earningsMap = shift.getEarningsMap()
                    val costsMap = shift.getCostsMap()
                    val inDriveRides = shift.getInDriveRides()

                    // Segment of earnings
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "GANHOS / FATURAMENTOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                        HorizontalDivider(color = Color(0xFF00FF66).copy(alpha = 0.2f))

                        if (earningsMap.isEmpty()) {
                            Text(
                                text = "Nenhum ganho registrado neste turno.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            earningsMap.forEach { (cat, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            editingEarningCat = cat
                                            val rawCentsStr = String.format(java.util.Locale.US, "%.2f", amount).replace(".", "")
                                            val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                            editingEarningValue = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar faturamento",
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        text = FormatUtils.formatCurrency(amount),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FF66)
                                    )
                                }
                            }
                        }
                    }

                    // Segment of tracked inDrive rides
                    if (inDriveRides.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "CORRIDAS COLETADAS (INDRIVE)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                            inDriveRides.forEachIndexed { index, ride ->
                                val formattedTime = try {
                                    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                    sdf.format(java.util.Date(ride.timestamp))
                                } catch (e: Exception) {
                                    "--:--"
                                }

                                val cardBgColor = if (ride.isCancelled) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }

                                val accentColor = if (ride.isCancelled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    Color(0xFF00FF66)
                                }

                                val iconBgColor = if (ride.isCancelled) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                } else {
                                    Color(0xFF00FF66).copy(alpha = 0.15f)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cardBgColor)
                                        .clickable {
                                            editingRide = ride
                                            val rawCentsStr = String.format(java.util.Locale.US, "%.2f", ride.value).replace(".", "")
                                            val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                            editingRideValue = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                            editingRideIsCancelled = ride.isCancelled
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(iconBgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (ride.isCancelled) "Corrida Cancelada" else "Corrida Recebida",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (ride.isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Coletado às: $formattedTime",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (ride.isCancelled) "R$ 0.00" else FormatUtils.formatCurrency(ride.value),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar corrida",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Segment of costs
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DESPESAS / GASTOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF007A)
                        )
                        HorizontalDivider(color = Color(0xFFFF007A).copy(alpha = 0.2f))

                        if (costsMap.isEmpty()) {
                            Text(
                                text = "Nenhuma despesa ou custo registrado neste turno.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            costsMap.forEach { (cat, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            editingCostCat = cat
                                            val rawCentsStr = String.format(java.util.Locale.US, "%.2f", amount).replace(".", "")
                                            val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                            editingCostValue = TextFieldValue(
                                                text = formatted,
                                                selection = TextRange(formatted.length)
                                            )
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar despesa",
                                            tint = Color(0xFFFF007A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text(
                                        text = FormatUtils.formatCurrency(amount),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF007A)
                                    )
                                }
                            }
                        }
                    }

                    // Segment of cash flow details (Caixa Inicial e Final)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CONTROLE DE CAIXA (DINHEIRO/TROCO)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))

                        // Box for Caixa Inicial (Editable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    val rawCentsStr = String.format(java.util.Locale.US, "%.2f", shift.startingCash).replace(".", "")
                                    val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                    editingStartingCashValue = TextFieldValue(
                                        text = formatted,
                                        selection = TextRange(formatted.length)
                                    )
                                    showEditStartingCashDialog = true
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar Caixa Inicial",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Caixa Inicial (Troco)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = FormatUtils.formatCurrency(shift.startingCash),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        // Box for Caixa Final (Editable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable {
                                    val rawCentsStr = String.format(java.util.Locale.US, "%.2f", shift.endCash).replace(".", "")
                                    val formatted = FormatUtils.formatInputAsCurrency(rawCentsStr)
                                    editingEndCashValue = TextFieldValue(
                                        text = formatted,
                                        selection = TextRange(formatted.length)
                                    )
                                    showEditEndCashDialog = true
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar Caixa Final",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Caixa Final (Dinheiro em Mãos)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = FormatUtils.formatCurrency(shift.endCash),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingEarningCat != null) {
        val cat = editingEarningCat!!
        AlertDialog(
            onDismissRequest = { editingEarningCat = null },
            modifier = Modifier.testTag("edit_closed_earning_dialog"),
            title = { Text("Editar Ganho", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajuste o valor para a categoria $cat neste turno:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingEarningValue,
                        onValueChange = { 
                            editingEarningValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor Acumulado ($cat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_closed_earning_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRemoveEarning(shift.id, cat)
                            editingEarningCat = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingEarningCat = null }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingEarningValue.text)
                                onEditEarning(shift.id, cat, value)
                                editingEarningCat = null
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (editingCostCat != null) {
        val cat = editingCostCat!!
        AlertDialog(
            onDismissRequest = { editingCostCat = null },
            modifier = Modifier.testTag("edit_closed_cost_dialog"),
            title = { Text("Editar Custo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajuste o valor para a despesa $cat neste turno:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingCostValue,
                        onValueChange = { 
                            editingCostValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor Acumulado ($cat)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_closed_cost_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRemoveCost(shift.id, cat)
                            editingCostCat = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingCostCat = null }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingCostValue.text)
                                onEditCost(shift.id, cat, value)
                                editingCostCat = null
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (editingRide != null) {
        val ride = editingRide!!
        AlertDialog(
            onDismissRequest = { editingRide = null },
            modifier = Modifier.testTag("edit_closed_ride_dialog"),
            title = { Text("Editar Corrida inDrive", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ajuste o valor desta corrida inDrive ou mude seu status de cancelamento neste turno:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingRideValue,
                        onValueChange = { 
                            editingRideValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Valor da Corrida (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_closed_ride_value_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marcar como Cancelada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = editingRideIsCancelled,
                            onCheckedChange = { editingRideIsCancelled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onRemoveInDriveRide(shift.id, ride.timestamp)
                            editingRide = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editingRide = null }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingRideValue.text)
                                onEditInDriveRide(shift.id, ride.timestamp, value, editingRideIsCancelled)
                                editingRide = null
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (showEditStartingCashDialog) {
        AlertDialog(
            onDismissRequest = { showEditStartingCashDialog = false },
            modifier = Modifier.testTag("edit_starting_cash_dialog"),
            title = { Text("Editar Caixa Inicial", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajuste o valor do caixa inicial (dinheiro/troco) deste turno:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingStartingCashValue,
                        onValueChange = { 
                            editingStartingCashValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Troco Inicial (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_starting_cash_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onEditStartingCash(shift.id, 0.0)
                            showEditStartingCashDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Zerar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zerar")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showEditStartingCashDialog = false }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingStartingCashValue.text)
                                onEditStartingCash(shift.id, value)
                                showEditStartingCashDialog = false
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (showEditEndCashDialog) {
        AlertDialog(
            onDismissRequest = { showEditEndCashDialog = false },
            modifier = Modifier.testTag("edit_end_cash_dialog"),
            title = { Text("Editar Caixa Final", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajuste o valor do caixa final (dinheiro em mãos) deste turno:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingEndCashValue,
                        onValueChange = { 
                            editingEndCashValue = TextFieldValue(
                                FormatUtils.formatInputAsCurrency(it.text), 
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            ) 
                        },
                        label = { Text("Dinheiro Final (R$)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_end_cash_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onEditEndCash(shift.id, 0.0)
                            showEditEndCashDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Zerar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zerar")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showEditEndCashDialog = false }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingEndCashValue.text)
                                onEditEndCash(shift.id, value)
                                showEditEndCashDialog = false
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun OpenShiftDialog(
    defaultStartingCash: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Double, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialCashText = if (defaultStartingCash > 0.0) {
        val centsStr = String.format(java.util.Locale.US, "%.2f", defaultStartingCash).replace(".", "")
        FormatUtils.formatInputAsCurrency(centsStr)
    } else {
        ""
    }
    var cashInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialCashText,
                selection = TextRange(initialCashText.length)
            )
        )
    }
    var odometerInput by remember { mutableStateOf(TextFieldValue("")) }

    val calendar = remember { java.util.Calendar.getInstance() }
    var selectedDay by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }
    var selectedMonthIndex by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH)) }

    var showCalendar by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    val isFormValid = cashInput.text.isNotBlank() && odometerInput.text.isNotBlank() && odometerInput.text.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("open_shift_dialog"),
        title = {
            Text(
                text = "Abertura de Turno com Caixa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Defina o dinheiro/troco inicial e a quilometragem inicial (km) para controle deste turno, e escolha a data.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(
                    onClick = { showCalendar = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Data: $selectedDay de ${monthsList[selectedMonthIndex]}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { cashInput = TextFieldValue(FormatUtils.formatInputAsCurrency(it.text), selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)) },
                    label = { Text("Dinheiro/Troco Inicial (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("starting_cash_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = odometerInput,
                    onValueChange = { odometerInput = it },
                    label = { Text("Kilometragem / Odômetro Inicial (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("starting_odometer_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (!isFormValid) {
                    Text(
                        text = "* Dinheiro/Troco e Odômetro são obrigatórios.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cashVal = FormatUtils.parseCurrency(cashInput.text)
                    val accountsVal = 0.0
                    val odometerVal = odometerInput.text.toDoubleOrNull() ?: 0.0

                    val finalCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.MONTH, selectedMonthIndex)
                        set(java.util.Calendar.DAY_OF_MONTH, selectedDay)
                    }
                    onConfirm(cashVal, accountsVal, odometerVal, finalCal.timeInMillis)
                },
                enabled = isFormValid,
                modifier = Modifier.testTag("confirm_open_shift")
            ) {
                Text("Abrir Turno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showCalendar) {
        CustomCalendarDialog(
            initialDay = selectedDay,
            initialMonthIndex = selectedMonthIndex,
            onDateSelected = { d, m ->
                selectedDay = d
                selectedMonthIndex = m
                showCalendar = false
            },
            onDismissRequest = { showCalendar = false }
        )
    }
}

@Composable
fun EditShiftDialog(
    shift: RideShift,
    categories: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onConfirm: (RideShift) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hrText by remember { mutableStateOf(shift.hoursWorked.toString()) }

    val calendar = remember { java.util.Calendar.getInstance().apply { timeInMillis = shift.date } }
    var selectedDay by remember { mutableStateOf(calendar.get(java.util.Calendar.DAY_OF_MONTH)) }
    var selectedMonthIndex by remember { mutableStateOf(calendar.get(java.util.Calendar.MONTH)) }

    var showCalendar by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }
    var pendingUpdatedShift by remember { mutableStateOf<RideShift?>(null) }

    val editableEarnings = remember {
        val map = androidx.compose.runtime.mutableStateMapOf<String, Double>()
        map.putAll(shift.getEarningsMap())
        map
    }
    
    val editableCosts = remember {
        val map = androidx.compose.runtime.mutableStateMapOf<String, Double>()
        map.putAll(shift.getCostsMap())
        map
    }

    var showEditItemDialog by remember { mutableStateOf(false) }
    var editingCategoryName by remember { mutableStateOf("") }
    var editingIsEarning by remember { mutableStateOf(true) }
    var editingValue by remember { mutableStateOf(TextFieldValue("")) }

    var showAddEarningDialog by remember { mutableStateOf(false) }
    var addingEarningCategory by remember { mutableStateOf("") }
    var addingEarningValue by remember { mutableStateOf(TextFieldValue("")) }

    var showAddCostDialog by remember { mutableStateOf(false) }
    var addingCostCategory by remember { mutableStateOf("") }
    var addingCostValue by remember { mutableStateOf(TextFieldValue("")) }

    val monthsList = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("edit_shift_dialog"),
        title = {
            Text(
                text = "Alterar Informações do Turno",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedCard(
                        onClick = { showCalendar = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Data: $selectedDay de ${monthsList[selectedMonthIndex]}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ganhos Registrados:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF66)
                        )
                        IconButton(
                            onClick = {
                                addingEarningCategory = ""
                                addingEarningValue = TextFieldValue("")
                                showAddEarningDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Adicionar Ganho",
                                tint = Color(0xFF00FF66)
                            )
                        }
                    }
                }

                val earningsList = editableEarnings.toList()
                if (earningsList.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum ganho registrado neste turno.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    items(earningsList) { (catName, amount) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = catName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = FormatUtils.formatCurrency(amount), color = Color(0xFF00FF66), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(
                                onClick = {
                                    editingCategoryName = catName
                                    editingIsEarning = true
                                    val rawCStr = String.format(java.util.Locale.US, "%.2f", amount).replace(".", "")
                                    editingValue = TextFieldValue(
                                        text = FormatUtils.formatInputAsCurrency(rawCStr),
                                        selection = TextRange(FormatUtils.formatInputAsCurrency(rawCStr).length)
                                    )
                                    showEditItemDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar $catName", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gastos Registrados:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF007A)
                        )
                        IconButton(
                            onClick = {
                                addingCostCategory = ""
                                addingCostValue = TextFieldValue("")
                                showAddCostDialog = true
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Adicionar Gasto",
                                tint = Color(0xFFFF007A)
                            )
                        }
                    }
                }

                val costsList = editableCosts.toList()
                if (costsList.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum gasto registrado neste turno.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    items(costsList) { (catName, amount) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = catName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = FormatUtils.formatCurrency(amount), color = Color(0xFFFF007A), style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(
                                onClick = {
                                    editingCategoryName = catName
                                    editingIsEarning = false
                                    val rawCStr = String.format(java.util.Locale.US, "%.2f", amount).replace(".", "")
                                    editingValue = TextFieldValue(
                                        text = FormatUtils.formatInputAsCurrency(rawCStr),
                                        selection = TextRange(FormatUtils.formatInputAsCurrency(rawCStr).length)
                                    )
                                    showEditItemDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar $catName", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão de Excluir Turno dentro da página de Edição
                TextButton(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3B30)),
                    modifier = Modifier.testTag("delete_shift_from_edit_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Turno",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val finalCal = java.util.Calendar.getInstance().apply {
                                timeInMillis = shift.date
                                set(java.util.Calendar.DAY_OF_MONTH, selectedDay)
                                set(java.util.Calendar.MONTH, selectedMonthIndex)
                            }

                            val earningsSerialized = editableEarnings.toList()
                                .filter { it.second > 0.0 }
                                .joinToString("|") { "${it.first}:${it.second}" }

                            val costsSerialized = editableCosts.toList()
                                .filter { it.second > 0.0 }
                                .joinToString("|") { "${it.first}:${it.second}" }

                            val updated = shift.copy(
                                kmDriven = shift.kmDriven,
                                hoursWorked = shift.hoursWorked,
                                notes = shift.notes,
                                date = finalCal.timeInMillis,
                                earningsMapString = earningsSerialized,
                                costsMapString = costsSerialized
                            )
                            pendingUpdatedShift = updated
                            showEditConfirmDialog = true
                        }
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    )

    if (showCalendar) {
        CustomCalendarDialog(
            initialDay = selectedDay,
            initialMonthIndex = selectedMonthIndex,
            onDateSelected = { d, m ->
                selectedDay = d
                selectedMonthIndex = m
                showCalendar = false
            },
            onDismissRequest = { showCalendar = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            modifier = Modifier.testTag("delete_shift_edit_confirm_dialog"),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Excluir Turno",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja apagar permanentemente este turno de seu histórico? Essa exclusão afetará o cálculo estatístico e os relatórios financeiros de forma definitiva.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_from_edit_dialog_btn")
                ) {
                    Text("Excluir Turno")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditConfirmDialog && pendingUpdatedShift != null) {
        AlertDialog(
            onDismissRequest = { showEditConfirmDialog = false },
            modifier = Modifier.testTag("edit_shift_confirm_dialog"),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informação",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Salvar Alterações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Deseja salvar as novas informações de faturamento, gastos e horas trabalhadas que você alterou para este turno?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEditConfirmDialog = false
                        onConfirm(pendingUpdatedShift!!)
                    },
                    modifier = Modifier.testTag("confirm_edit_from_edit_dialog_btn")
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditConfirmDialog = false }) {
                    Text("Revisar")
                }
            }
        )
    }

    if (showEditItemDialog) {
        AlertDialog(
            onDismissRequest = { showEditItemDialog = false },
            title = { Text(text = "Editar ${if (editingIsEarning) "Ganho" else "Gasto"}: $editingCategoryName", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Informe o novo valor para a categoria $editingCategoryName:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingValue,
                        onValueChange = {
                            editingValue = TextFieldValue(
                                text = FormatUtils.formatInputAsCurrency(it.text),
                                selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                            )
                        },
                        label = { Text("Valor") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Text("R$ ", fontWeight = FontWeight.Bold) }
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (editingIsEarning) {
                                editableEarnings.remove(editingCategoryName)
                            } else {
                                editableCosts.remove(editingCategoryName)
                            }
                            showEditItemDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Zerar")
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showEditItemDialog = false }) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(editingValue.text)
                                if (editingIsEarning) {
                                    if (value > 0.0) {
                                        editableEarnings[editingCategoryName] = value
                                    } else {
                                        editableEarnings.remove(editingCategoryName)
                                    }
                                } else {
                                    if (value > 0.0) {
                                        editableCosts[editingCategoryName] = value
                                    } else {
                                        editableCosts.remove(editingCategoryName)
                                    }
                                }
                                showEditItemDialog = false
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        )
    }

    if (showAddEarningDialog) {
        val eligibleEarningCategories = categories.filter { it.type == "EARNING" && it.name !in editableEarnings.keys }
        
        AlertDialog(
            onDismissRequest = { showAddEarningDialog = false },
            title = { Text("Registrar Novo Ganho", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Selecione uma categoria de ganho e insira o valor correspondente:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (eligibleEarningCategories.isEmpty()) {
                        Text(
                            text = "Todas as categorias de ganho já estão registradas neste turno.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        if (addingEarningCategory.isEmpty() && eligibleEarningCategories.isNotEmpty()) {
                            addingEarningCategory = eligibleEarningCategories.first().name
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { expandedDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = addingEarningCategory, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar")
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                eligibleEarningCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            addingEarningCategory = cat.name
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = addingEarningValue,
                            onValueChange = {
                                addingEarningValue = TextFieldValue(
                                    text = FormatUtils.formatInputAsCurrency(it.text),
                                    selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                                )
                            },
                            label = { Text("Valor") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("R$ ", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAddEarningDialog = false }) {
                        Text("Cancelar")
                    }
                    if (eligibleEarningCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(addingEarningValue.text)
                                if (value > 0.0 && addingEarningCategory.isNotEmpty()) {
                                    editableEarnings[addingEarningCategory] = value
                                }
                                showAddEarningDialog = false
                            }
                        ) {
                            Text("Adicionar")
                        }
                    }
                }
            }
        )
    }

    if (showAddCostDialog) {
        val eligibleCostCategories = categories.filter { it.type == "COST" && it.name !in editableCosts.keys }
        
        AlertDialog(
            onDismissRequest = { showAddCostDialog = false },
            title = { Text("Registrar Novo Gasto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Selecione uma categoria de gasto e insira o valor correspondente:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (eligibleCostCategories.isEmpty()) {
                        Text(
                            text = "Todas as categorias de gasto já estão registradas neste turno.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF3B30),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        if (addingCostCategory.isEmpty() && eligibleCostCategories.isNotEmpty()) {
                            addingCostCategory = eligibleCostCategories.first().name
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { expandedDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = addingCostCategory, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar")
                                }
                            }
                            
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                eligibleCostCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            addingCostCategory = cat.name
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = addingCostValue,
                            onValueChange = {
                                addingCostValue = TextFieldValue(
                                    text = FormatUtils.formatInputAsCurrency(it.text),
                                    selection = TextRange(FormatUtils.formatInputAsCurrency(it.text).length)
                                )
                            },
                            label = { Text("Valor") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("R$ ", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAddCostDialog = false }) {
                        Text("Cancelar")
                    }
                    if (eligibleCostCategories.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = FormatUtils.parseCurrency(addingCostValue.text)
                                if (value > 0.0 && addingCostCategory.isNotEmpty()) {
                                    editableCosts[addingCostCategory] = value
                                }
                                showAddCostDialog = false
                            }
                        ) {
                            Text("Adicionar")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AddShiftDialog(
    categories: List<FinanceCategory>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Double>, Map<String, Double>, Double, Double, String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTypeEarning by remember { mutableStateOf(true) }

    val activeCategories = remember(isTypeEarning, categories) {
        if (isTypeEarning) {
            categories.filter { it.type == "EARNING" }
        } else {
            categories.filter { it.type == "COST" }
        }
    }

    var selectedCategory by remember(activeCategories) {
        mutableStateOf(activeCategories.firstOrNull())
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    var amountValue by remember { mutableStateOf(TextFieldValue("")) }
    var hoursWorked by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    val calendar = remember {
        java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
    }
    val initialDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val initialMonth = calendar.get(java.util.Calendar.MONTH)

    var selectedDay by remember { mutableStateOf(initialDay) }
    var selectedMonthIndex by remember { mutableStateOf(initialMonth) }

    AlertDialog(
        modifier = modifier.testTag("add_shift_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Lançamento Avulso",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    if (showError) {
                        Text(
                            text = "Por favor, preencha a categoria, valor válido maior que R$ 0,00 e quilometragem/horas válidas.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Text(
                        text = "Tipo de Movimentação *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isTypeEarning = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTypeEarning) Color(0xFF02C39A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (isTypeEarning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).testTag("type_earning_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ganho / Lucro", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isTypeEarning = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isTypeEarning) Color(0xFFFF0055) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = if (!isTypeEarning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).testTag("type_cost_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Custo / Gasto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Text(
                        text = if (isTypeEarning) "Fonte de Lucro / Receita *" else "Gasto / Despesa / Custo *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Selecione uma opção",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expandir"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("category_select_trigger"),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isTypeEarning) Icons.Default.AttachMoney else Icons.Default.LocalGasStation,
                                    contentDescription = null,
                                    tint = if (isTypeEarning) Color(0xFF00FF66) else Color(0xFFFF007A)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { isDropdownExpanded = !isDropdownExpanded }
                        )
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                        ) {
                            if (activeCategories.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhuma categoria d cadastrada") },
                                    onClick = { isDropdownExpanded = false }
                                )
                            } else {
                                activeCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(text = cat.name) },
                                        onClick = {
                                            selectedCategory = cat
                                            isDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("category_option_${cat.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Valor (R$) *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = amountValue,
                        onValueChange = { newValue ->
                            val formatted = FormatUtils.formatInputAsCurrency(newValue.text)
                            amountValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        },
                        label = { Text("Valor do Lançamento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_amount_input"),
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = if (isTypeEarning) Color(0xFF00FF66) else Color(0xFFFF007A)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Text(
                        text = "Data da Operação *",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    var showCalendarDialog by remember { mutableStateOf(false) }
                    
                    if (showCalendarDialog) {
                        CustomCalendarDialog(
                            initialDay = selectedDay,
                            initialMonthIndex = selectedMonthIndex,
                            onDateSelected = { day: Int, monthIndex: Int ->
                                selectedDay = day
                                selectedMonthIndex = monthIndex
                                showCalendarDialog = false
                            },
                            onDismissRequest = { showCalendarDialog = false }
                        )
                    }

                    OutlinedCard(
                        onClick = { showCalendarDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_trigger_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Calendário",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Dia / Mês selecionado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(java.util.Locale("pt", "BR"), "%02d / %s", selectedDay, monthsList[selectedMonthIndex]),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Alterar Data",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = FormatUtils.parseCurrency(amountValue.text)
                    val cat = selectedCategory
                    val finalKm = 0.0
                    val finalHours = 0.0

                    if (cat != null && finalAmount > 0.0) {
                        val finalEarnings = if (isTypeEarning) mapOf(cat.name to finalAmount) else emptyMap()
                        val finalCosts = if (!isTypeEarning) mapOf(cat.name to finalAmount) else emptyMap()

                        val finalCal = java.util.Calendar.getInstance()
                        finalCal.set(java.util.Calendar.MONTH, selectedMonthIndex)
                        finalCal.set(java.util.Calendar.DAY_OF_MONTH, selectedDay)
                        finalCal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                        finalCal.set(java.util.Calendar.MINUTE, 0)
                        finalCal.set(java.util.Calendar.SECOND, 0)
                        finalCal.set(java.util.Calendar.MILLISECOND, 0)
                        val selectedDateMs = finalCal.timeInMillis

                        onConfirm(finalEarnings, finalCosts, finalKm, finalHours, "", selectedDateMs)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_add_shift_btn")
            ) {
                Text("Confirmar Lançamento")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_add_shift_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CustomCalendarDialog(
    initialDay: Int,
    initialMonthIndex: Int,
    onDateSelected: (day: Int, monthIndex: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val monthsList = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    var currentMonthIndex by remember { mutableStateOf(initialMonthIndex) }
    var selectedDay by remember { mutableStateOf(initialDay) }

    val year = 2026

    val cal = remember(currentMonthIndex) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, currentMonthIndex)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
    }

    val maxDays = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)

    val daysList = remember(maxDays, firstDayOfWeek) {
        val list = mutableListOf<Int?>()
        val offset = firstDayOfWeek - 1
        for (i in 0 until offset) {
            list.add(null)
        }
        for (day in 1..maxDays) {
            list.add(day)
        }
        list
    }

    val chunkedWeeks = remember(daysList) { daysList.chunked(7) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("custom_calendar_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonthIndex > 0) {
                            currentMonthIndex--
                        } else {
                            currentMonthIndex = 11
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Mês Anterior"
                    )
                }

                Text(
                    text = "${monthsList[currentMonthIndex]} $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        if (currentMonthIndex < 11) {
                            currentMonthIndex++
                        } else {
                            currentMonthIndex = 0
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Próximo Mês"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val weekdaysHeader = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekdaysHeader.forEach { dayName ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                chunkedWeeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { dayValue ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayValue != null) {
                                    val isSelected = (dayValue == selectedDay)
                                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(bg)
                                            .clickable { selectedDay = dayValue }
                                            .testTag("day_button_$dayValue"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayValue.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                        if (week.size < 7) {
                            val leftover = 7 - week.size
                            for (j in 0 until leftover) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDateSelected(selectedDay, currentMonthIndex) }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val accessibilityEnabled = try {
        android.provider.Settings.Secure.getInt(
            context.contentResolver,
            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Exception) {
         0
    }
    if (accessibilityEnabled == 1) {
        val settingValue = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            val splitter = android.text.TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)
            while (splitter.hasNext()) {
                val accessService = splitter.next()
                if (accessService.contains(context.packageName) && accessService.contains("ScreenMonitorAccessibilityService")) {
                    return true
                }
            }
        }
    }
    return false
}
