package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FixedExpense

@Composable
fun FixedExpensesScreen(
    fixedExpenses: List<FixedExpense>,
    onAddExpense: (String, Double, Int, Boolean) -> Unit,
    onToggleExpensePaid: (FixedExpense) -> Unit,
    onDeleteExpense: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var filterSelection by remember { mutableStateOf("TODAS") } // "TODAS", "ABERTO", "PAGAS"

    val totalFixedAmount = fixedExpenses.sumOf { it.amount }
    val totalOpenAmount = fixedExpenses.filter { !it.isPaid }.sumOf { it.amount }
    val totalPaidAmount = fixedExpenses.filter { it.isPaid }.sumOf { it.amount }

    val filteredExpenses = remember(fixedExpenses, filterSelection) {
        when (filterSelection) {
            "PAGAS" -> fixedExpenses.filter { it.isPaid }
            "ABERTO" -> fixedExpenses.filter { !it.isPaid }
            else -> fixedExpenses
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("fixed_expenses_list")
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Títulos e cabeçalhos
            item {
                Column {
                    Text(
                        text = "Custos Fixos Mensais",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Registre os custos que ocorrem todo mês (ex: aluguel, seguro, internet).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Card resumo de despesa fixa total e situação (Pagas vs Em Aberto)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fixed_summary_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL DE COMPROMISSOS FIXOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = FormatUtils.formatCurrency(totalFixedAmount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Despesas Fixas",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        if (fixedExpenses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Em Aberto",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(totalOpenAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF007A)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Já Pagas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(totalPaidAmount),
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

            // Caixa de Seleção / Seletor Customizado de Filtro (Todas / Em Aberto / Já Pagas)
            item {
                Column {
                    Text(
                        text = "Situação das Contas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val options = listOf("TODAS" to "Todas", "ABERTO" to "Em Aberto", "PAGAS" to "Já Pagas")
                        options.forEach { (key, label) ->
                            val isSelected = filterSelection == key
                            val count = when (key) {
                                "TODAS" -> fixedExpenses.size
                                "PAGAS" -> fixedExpenses.count { it.isPaid }
                                else -> fixedExpenses.count { !it.isPaid }
                            }
                            val bgCol = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                            val textCol = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bgCol)
                                    .clickable { filterSelection = key }
                                    .padding(vertical = 10.dp)
                                    .testTag("filter_fixed_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$label ($count)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            }
                        }
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    val emptyMessage = when (filterSelection) {
                        "PAGAS" -> "Nenhuma conta marcada como paga."
                        "ABERTO" -> "Tudo em dia! Sem contas em aberto."
                        else -> "Nenhum compromisso mensal cadastrado."
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (filterSelection == "TODAS") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("empty_add_fixed_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adicionar Conta Mensal")
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Lista Selecionada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(filteredExpenses, key = { it.id }) { expense ->
                    FixedExpenseItemCard(
                        expense = expense,
                        onTogglePaidClick = { onToggleExpensePaid(expense) },
                        onDeleteClick = { onDeleteExpense(expense.id) }
                    )
                }
            }
        }

        // Botão FAB para adicionar despesa fixa
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_fixed_fab"),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar Despesa Fixa",
                modifier = Modifier.size(28.dp)
            )
        }

        // Diálogo de Adicionar Nova Despesa Fixa
        if (showAddDialog) {
            AddFixedExpenseDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, amount, dueDay, isPaid ->
                    onAddExpense(name, amount, dueDay, isPaid)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun FixedExpenseItemCard(
    expense: FixedExpense,
    onTogglePaidClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fixed_item_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox interativo no lugar do ícone estático para facilitar a troca rápida
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (expense.isPaid) Color(0xFF114B3E) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                    .clickable { onTogglePaidClick() }
                    .testTag("fixed_toggle_paid_${expense.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expense.isPaid) Icons.Default.CheckCircle else {
                        when {
                            expense.name.contains("aluguel", ignoreCase = true) || expense.name.contains("carro", ignoreCase = true) -> Icons.Default.DirectionsCar
                            expense.name.contains("seguro", ignoreCase = true) -> Icons.Default.Shield
                            expense.name.contains("net", ignoreCase = true) || expense.name.contains("celular", ignoreCase = true) || expense.name.contains("plano", ignoreCase = true) -> Icons.Default.NetworkCell
                            expense.name.contains("mei", ignoreCase = true) || expense.name.contains("imposto", ignoreCase = true) -> Icons.Default.ReceiptLong
                            else -> Icons.Default.AccountBalanceWallet
                        }
                    },
                    contentDescription = if (expense.isPaid) "Paga" else "Em Aberto",
                    tint = if (expense.isPaid) Color(0xFF00FF66) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (expense.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge Interativa ou Pequena Caixa de Seleção / Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (expense.isPaid) Color(0xFF114B3E) else Color(0xFF3A1C2C)
                            )
                            .clickable { onTogglePaidClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (expense.isPaid) "CONTA PAGA" else "EM ABERTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = if (expense.isPaid) Color(0xFF00FF66) else Color(0xFFFF007A)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Dia de Vencimento",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Todo dia ${expense.dueDay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (expense.isPaid) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.tertiary
                )
                
                // Botão de excluir
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("delete_fixed_btn_${expense.id}"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Despesa Fixa",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFixedExpensesState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = "Nenhum custo fixo",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Nenhum compromisso mensal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Cadastre custos fixos para ajudarmos você a deduzir seus gastos do seu lucro total líquido.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("empty_add_fixed_btn")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Conta Mensal")
        }
    }
}

@Composable
fun AddFixedExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf(TextFieldValue("")) }
    var isPaidPreSelect by remember { mutableStateOf(false) }

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
    val initialMonth = calendar.get(java.util.Calendar.MONTH) // 0 to 11

    var selectedDay by remember { mutableStateOf(initialDay) }
    var selectedMonthIndex by remember { mutableStateOf(initialMonth) } // 0 to 11

    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier.testTag("add_fixed_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Despesa Fixa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showError) {
                    Text(
                        text = "Por favor, insira o nome, valor de vencimento válidos.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Despesa (Ex: Aluguel do Carro) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fixed_name_input")
                )

                OutlinedTextField(
                    value = amountValue,
                    onValueChange = { newValue ->
                        val formatted = FormatUtils.formatInputAsCurrency(newValue.text)
                        amountValue = TextFieldValue(
                            text = formatted,
                            selection = TextRange(formatted.length)
                        )
                    },
                    label = { Text("Valor Mensal (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fixed_amount_input"),
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF4CAF50)) }
                )

                Text(
                    text = "Vencimento Mensal *",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

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
                        .testTag("fixed_calendar_trigger_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Calendário",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Vence todo dia",
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Opção de já marcar como pago ao cadastrar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isPaidPreSelect = !isPaidPreSelect }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaidPreSelect) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isPaidPreSelect) Color(0xFF00FF66) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Já está paga?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = isPaidPreSelect,
                        onCheckedChange = { isPaidPreSelect = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FF66),
                            checkedTrackColor = Color(0xFF114B3E)
                        ),
                        modifier = Modifier.testTag("fixed_dialog_paid_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = FormatUtils.parseCurrency(amountValue.text)
                    val finalDueDay = selectedDay

                    if (name.isNotBlank() && finalAmount > 0.0 && finalDueDay in 1..31) {
                        onConfirm(name, finalAmount, finalDueDay, isPaidPreSelect)
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_add_fixed_btn")
            ) {
                Text("Salvar Despesa")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_add_fixed_btn")
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
    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

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
