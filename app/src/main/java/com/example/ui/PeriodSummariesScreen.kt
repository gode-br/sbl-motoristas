package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RideShift
import com.example.data.FixedExpense
import java.text.SimpleDateFormat
import java.util.*

data class PeriodSummary(
    val title: String,
    val dateKey: String,
    val totalEarnings: Double,
    val totalCosts: Double,
    val netProfit: Double,
    val totalKm: Double,
    val totalHours: Double,
    val earningsByCategory: Map<String, Double>,
    val costsByCategory: Map<String, Double>,
    val fuelCosts: Double,
    val variableCosts: Double,
    val fixedCosts: Double,
    val shifts: List<RideShift> = emptyList()
)

@Composable
fun PeriodSummariesScreen(
    shifts: List<RideShift>,
    fixedExpenses: List<FixedExpense>,
    modifier: Modifier = Modifier
) {
    // 0: Diário, 1: Semanal, 2: Mensal
    var selectedTab by remember { mutableStateOf(0) }

    val dailyList = remember(shifts, fixedExpenses) { getDailySummaries(shifts, fixedExpenses) }
    val weeklyList = remember(shifts, fixedExpenses) { getWeeklySummaries(shifts, fixedExpenses) }
    val monthlyList = remember(shifts, fixedExpenses) { getMonthlySummaries(shifts, fixedExpenses) }

    val activeList = when (selectedTab) {
        0 -> dailyList
        1 -> weeklyList
        else -> monthlyList
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Seletor de Períodos Stylized (Tabs Modificadas para Combinação com visual Neon)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.testTag("period_tab_row")
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Diário", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("period_tab_daily").padding(vertical = 8.dp)
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Semanal", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                icon = { Icon(Icons.Default.ViewWeek, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("period_tab_weekly").padding(vertical = 8.dp)
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Mensal", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                icon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("period_tab_monthly").padding(vertical = 8.dp)
            )
        }

        if (activeList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Nenhum histórico",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sem faturamento registrado",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Os dados compilados aparecerão aqui assim que você registrar turnos de corrida.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("period_summaries_list")
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Header descritivo do relatório selecionado
                item {
                    Column {
                        val headerText = when (selectedTab) {
                            0 -> "Análise diária consolidada das suas madrugadas de trabalho."
                            1 -> "Resumos agrupados de segunda a domingo para acompanhamento de metas."
                            else -> "Visão geral mensal sobre a rentabilidade absoluta do motorista."
                        }
                        Text(
                            text = when (selectedTab) {
                                0 -> "Faturamento Diário"
                                1 -> "Faturamento Semanal"
                                else -> "Faturamento Mensal"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(activeList, key = { it.dateKey }) { summary ->
                    PeriodSummaryItem(summary = summary, isDaily = (selectedTab == 0))
                }
            }
        }
    }
}

@Composable
fun PeriodSummaryItem(summary: PeriodSummary, isDaily: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val isPositive = summary.netProfit >= 0
    val netColor = if (isPositive) Color(0xFF00FF66) else Color(0xFFFF007A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("period_summary_card_${summary.dateKey}")
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header do card: Título do período e lucro real consolidado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lucro Líquido",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatCurrency(summary.netProfit),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = netColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (expanded) "Fechar detalhes" else "Ver detalhes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-grid rápida de métricas do período: Ganhos, Custos e Distância
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Bruto
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ganhos Brutos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(FormatUtils.formatCurrency(summary.totalEarnings), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00FF66))
                }
                // Custos
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Custos Operacionais", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(FormatUtils.formatCurrency(summary.totalCosts), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF007A))
                }
                // Km
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Distância", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(FormatUtils.formatKm(summary.totalKm), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            // Exposição de faturamentos e gastos detalhados
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Canal de faturamentos (Ganhos)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "faturamentos por canal".uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00FF66),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (summary.earningsByCategory.isEmpty()) {
                                Text("Nenhum faturamento registrado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                summary.earningsByCategory.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(FormatUtils.formatCurrency(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Detalhamento de gastos (Custos)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "gastos por tipo".uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF007A),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (summary.costsByCategory.isEmpty()) {
                                Text("Nenhum custo registrado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                summary.costsByCategory.entries.sortedByDescending { it.value }.forEach { (name, amount) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(FormatUtils.formatCurrency(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ANÁLISE ADICIONAL DE CUSTOS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bloco de Combustível
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalGasStation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Combustível",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = FormatUtils.formatCurrency(summary.fuelCosts),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (summary.fuelCosts > 0) {
                                    val fuelPercentage = if (summary.totalEarnings > 0) (summary.fuelCosts / summary.totalEarnings) * 100 else 0.0
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${String.format(Locale("pt", "BR"), "%.1f", fuelPercentage)}% do bruto",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (summary.totalKm > 0) {
                                        val fuelPerKm = summary.fuelCosts / summary.totalKm
                                        Text(
                                            text = "${FormatUtils.formatCurrency(fuelPerKm)}/km",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Sem gastos registrados",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Bloco de Custos Fixos e Variáveis
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Variáveis vs Fixos",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // Variáveis
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Var. (Turnos):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(FormatUtils.formatCurrency(summary.variableCosts), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                // Fixos
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val isEst = summary.title.startsWith("Dia") || summary.title.startsWith("Semana")
                                    val fixoLabel = if (isEst) "Fixos (Pro-rata):" else "Fixos (Mensal):"
                                    Text(fixoLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(FormatUtils.formatCurrency(summary.fixedCosts), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                if (summary.fixedCosts > 0 && (summary.title.startsWith("Dia") || summary.title.startsWith("Semana"))) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val estimatedNet = summary.totalEarnings - summary.variableCosts - summary.fixedCosts
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Líq. Estimado:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = FormatUtils.formatCurrency(estimatedNet),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (estimatedNet >= 0) Color(0xFF00FF66) else Color(0xFFFF007A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isDaily) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = "DETALHAMENTO DE TURNOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        summary.shifts.forEachIndexed { sIdx, sh ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Turno #${sIdx + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Ganhos: ${FormatUtils.formatCurrency(sh.totalEarnings)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00FF66)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Km Rodados: ${FormatUtils.formatKm(sh.kmDriven)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val startingAccountsText = if (sh.startingAccounts > 0.0) " • Contas ${FormatUtils.formatCurrency(sh.startingAccounts)}" else ""
                                    val startingOdometerText = if (sh.startOdometer > 0.0) " • Odômetro Inicial: ${sh.startOdometer} km" else ""
                                    Text(
                                        text = "Caixa Inicial: Dinheiro/Troco ${FormatUtils.formatCurrency(sh.startingCash)}$startingAccountsText$startingOdometerText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Caixa Final: Dinheiro em Mãos ${FormatUtils.formatCurrency(sh.endCash)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (sh.notes.isNotEmpty()) {
                                        Text(
                                            text = "Nota: ${sh.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = "DETALHAMENTO DE CORRIDAS (INDRIVE)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val allDailyIndriveRides = summary.shifts.flatMap { sh -> sh.getInDriveRides() }
                        
                        if (allDailyIndriveRides.isEmpty()) {
                            Text(
                                text = "Nenhuma corrida inDrive registrada para este dia.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allDailyIndriveRides.forEachIndexed { i, ride ->
                                    val formattedTime = try {
                                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        sdf.format(Date(ride.timestamp))
                                    } catch (e: Exception) {
                                        "--:--"
                                    }
                                    
                                    val statusLabel = if (ride.isCancelled) "Cancelada" else "Concluída"
                                    val statusColor = if (ride.isCancelled) Color(0xFFFF0055) else Color(0xFF00FF66)
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(statusColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#${i + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "InDrive - $statusLabel",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Capturada às $formattedTime",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF0055)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = FormatUtils.formatCurrency(ride.value),
                                                style = MaterialTheme.typography.bodySmall,
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
        }
    }
}

// Grouping helpers
fun getDailySummaries(shifts: List<RideShift>, fixedExpenses: List<FixedExpense>): List<PeriodSummary> {
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val grouped = shifts.groupBy { fmt.format(Date(it.date)) }
    val totalFixedMonthly = fixedExpenses.sumOf { it.amount }
    val dailyFixedProRata = totalFixedMonthly / 30.0

    return grouped.map { (dateStr, shiftsInDay) ->
        val earningsCat = mutableMapOf<String, Double>()
        val costsCat = mutableMapOf<String, Double>()
        var tk = 0.0
        var th = 0.0
        var te = 0.0
        var tc = 0.0
        for (sh in shiftsInDay) {
            tk += sh.kmDriven
            th += sh.hoursWorked
            te += sh.totalEarnings
            tc += sh.totalCosts
            sh.getEarningsMap().forEach { (k, v) ->
                earningsCat[k] = (earningsCat[k] ?: 0.0) + v
            }
            sh.getCostsMap().forEach { (k, v) ->
                costsCat[k] = (costsCat[k] ?: 0.0) + v
            }
        }
        val fuelCostsVal = costsCat.entries
            .filter { it.key.equals("combustível", ignoreCase = true) || it.key.equals("combustivel", ignoreCase = true) }
            .sumOf { it.value }

        PeriodSummary(
            title = "Dia $dateStr",
            dateKey = dateStr,
            totalEarnings = te,
            totalCosts = tc,
            netProfit = te - tc,
            totalKm = tk,
            totalHours = th,
            earningsByCategory = earningsCat,
            costsByCategory = costsCat,
            fuelCosts = fuelCostsVal,
            variableCosts = tc,
            fixedCosts = dailyFixedProRata,
            shifts = shiftsInDay
        )
    }.sortedByDescending { it.dateKey }
}

fun getWeeklySummaries(shifts: List<RideShift>, fixedExpenses: List<FixedExpense>): List<PeriodSummary> {
    val cal = Calendar.getInstance(Locale("pt", "BR"))
    val fmtKey = SimpleDateFormat("yyyy-'W'ww", Locale("pt", "BR"))
    val totalFixedMonthly = fixedExpenses.sumOf { it.amount }
    val weeklyFixedProRata = (totalFixedMonthly / 30.0) * 7.0
    
    val grouped = shifts.groupBy { sh ->
        cal.timeInMillis = sh.date
        fmtKey.format(Date(sh.date))
    }
    
    return grouped.map { (weekKey, shiftsInWeek) ->
        val earningsCat = mutableMapOf<String, Double>()
        val costsCat = mutableMapOf<String, Double>()
        var tk = 0.0
        var th = 0.0
        var te = 0.0
        var tc = 0.0
        
        val firstOfWeekCal = Calendar.getInstance(Locale("pt", "BR"))
        val lastOfWeekCal = Calendar.getInstance(Locale("pt", "BR"))
        
        if (shiftsInWeek.isNotEmpty()) {
            val oldestShiftDate = shiftsInWeek.minOf { it.date }
            firstOfWeekCal.timeInMillis = oldestShiftDate
            firstOfWeekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            lastOfWeekCal.timeInMillis = firstOfWeekCal.timeInMillis
            lastOfWeekCal.add(Calendar.DAY_OF_MONTH, 6)
        }
        
        val dateFmt = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
        val title = "Semana de ${dateFmt.format(firstOfWeekCal.time)} a ${dateFmt.format(lastOfWeekCal.time)}"

        for (sh in shiftsInWeek) {
            tk += sh.kmDriven
            th += sh.hoursWorked
            te += sh.totalEarnings
            tc += sh.totalCosts
            sh.getEarningsMap().forEach { (k, v) ->
                earningsCat[k] = (earningsCat[k] ?: 0.0) + v
            }
            sh.getCostsMap().forEach { (k, v) ->
                costsCat[k] = (costsCat[k] ?: 0.0) + v
            }
        }
        val fuelCostsVal = costsCat.entries
            .filter { it.key.equals("combustível", ignoreCase = true) || it.key.equals("combustivel", ignoreCase = true) }
            .sumOf { it.value }
        
        PeriodSummary(
            title = title,
            dateKey = weekKey,
            totalEarnings = te,
            totalCosts = tc,
            netProfit = te - tc,
            totalKm = tk,
            totalHours = th,
            earningsByCategory = earningsCat,
            costsByCategory = costsCat,
            fuelCosts = fuelCostsVal,
            variableCosts = tc,
            fixedCosts = weeklyFixedProRata,
            shifts = shiftsInWeek
        )
    }.sortedByDescending { it.dateKey }
}

fun getMonthlySummaries(shifts: List<RideShift>, fixedExpenses: List<FixedExpense>): List<PeriodSummary> {
    val fmtMonth = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
    val fmtKey = SimpleDateFormat("yyyy-MM", Locale("pt", "BR"))
    
    val grouped = shifts.groupBy { sh ->
        fmtKey.format(Date(sh.date))
    }
    
    return grouped.map { (monthKey, shiftsInMonth) ->
        val earningsCat = mutableMapOf<String, Double>()
        val costsCat = mutableMapOf<String, Double>()
        var tk = 0.0
        var th = 0.0
        var te = 0.0
        var tc = 0.0
        
        val monthLabel = if (shiftsInMonth.isNotEmpty()) {
            fmtMonth.format(Date(shiftsInMonth[0].date)).replaceFirstChar { it.uppercase() }
        } else monthKey

        for (sh in shiftsInMonth) {
            tk += sh.kmDriven
            th += sh.hoursWorked
            te += sh.totalEarnings
            tc += sh.totalCosts
            sh.getEarningsMap().forEach { (k, v) ->
                earningsCat[k] = (earningsCat[k] ?: 0.0) + v
            }
            sh.getCostsMap().forEach { (k, v) ->
                costsCat[k] = (costsCat[k] ?: 0.0) + v
            }
        }

        val fuelCostsVal = costsCat.entries
            .filter { it.key.equals("combustível", ignoreCase = true) || it.key.equals("combustivel", ignoreCase = true) }
            .sumOf { it.value }

        // Soma custos fixos mensais cadastrados para este mês
        val totalFixed = fixedExpenses.sumOf { it.amount }
        fixedExpenses.forEach { fe ->
            costsCat[fe.name] = (costsCat[fe.name] ?: 0.0) + fe.amount
        }

        val totalCostsWithFixed = tc + totalFixed

        PeriodSummary(
            title = monthLabel,
            dateKey = monthKey,
            totalEarnings = te,
            totalCosts = totalCostsWithFixed,
            netProfit = te - totalCostsWithFixed,
            totalKm = tk,
            totalHours = th,
            earningsByCategory = earningsCat,
            costsByCategory = costsCat,
            fuelCosts = fuelCostsVal,
            variableCosts = tc,
            fixedCosts = totalFixed,
            shifts = shiftsInMonth
        )
    }.sortedByDescending { it.dateKey }
}
