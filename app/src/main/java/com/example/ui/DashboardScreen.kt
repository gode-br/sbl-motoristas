package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import android.os.Build
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect

@Composable
fun DashboardScreen(
    summary: FinancialSummary,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_column")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Design Personalizado para o Nome com Vibe Neon "Motoristas da Madrugada"
        item {
            MidnightBrandHeader()
        }

        // Card de permissão de otimização de bateria e explicação de acessibilidade desativando
        item {
            BatteryOptimizationCard()
        }

        // Card de Faturamento de Média Diária referente ao dia da semana de hoje
        item {
            val isDark = isSystemInDarkTheme()
            val gradientColors = if (isDark) {
                listOf(Color(0xFF111522), Color(0xFF1D2436))
            } else {
                listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
            }
            val textColor = if (isDark) Color.White else Color(0xFF0F172A)
            val subtextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
            val iconTint = MaterialTheme.colorScheme.secondary
            val weekdayLabel = if (summary.currentWeekdayName.isNotEmpty()) summary.currentWeekdayName.uppercase() else "ESTE DIA DA SEMANA"
            val descriptionLabel = if (summary.currentWeekdayName.isNotEmpty()) "Média diária de receitas brutas obtidas em dias de ${summary.currentWeekdayName.lowercase()}." else "Sua média de faturamento para o dia da semana atual."

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(gradientColors))
                    .border(
                        width = 1.dp,
                        color = iconTint.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
                    .testTag("net_profit_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MÉDIA DIÁRIA: $weekdayLabel",
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Média de Faturamento",
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = FormatUtils.formatCurrency(summary.weekdayAverageEarnings),
                        style = MaterialTheme.typography.headlineLarge,
                        color = textColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        modifier = Modifier.testTag("net_profit_text")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = descriptionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtextColor
                    )
                }
            }
        }

        // Linha com Lucro Líquido e Despesas Totais em Cards Espaciais
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card Lucro Líquido (Dynamic Accent depending on positive/negative)
                val isNetPositive = summary.totalNetProfit >= 0
                val netColor = if (isNetPositive) Color(0xFF00FF66) else Color(0xFFFF007A)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gross_earnings_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isNetPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = "Lucro Líquido",
                                tint = netColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lucro Líquido",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = FormatUtils.formatCurrency(summary.totalNetProfit),
                            style = MaterialTheme.typography.titleMedium,
                            color = netColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Card Despesas Totais (Neon Pink Accent)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("total_expenses_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Despesas Totais",
                                tint = Color(0xFFFF007A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custos Totais",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = FormatUtils.formatCurrency(summary.totalDailyCost + summary.totalFixedExpenses),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF007A)
                        )
                    }
                }
            }
        }

        // Linha com Lucro Bruto (Até o Período) e Gastos Variáveis em Cards Espaciais
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card Lucro Bruto (Green Accent)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gross_profit_period_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = "Lucro Bruto",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lucro Bruto",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = FormatUtils.formatCurrency(summary.totalEarnings),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF00FF66),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Card Gastos Variáveis (Cyan Accent)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("variable_expenses_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = "Gastos Variáveis",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gastos Variáveis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = FormatUtils.formatCurrency(summary.totalDailyCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }

        // Subtitulo Metricas
        item {
            Text(
                text = "Métricas de Eficiência Noturna",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Grids de Metricas de Eficiência Noturna
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Proporcao de Combustivel
                EfficiencyMetricRow(
                    title = "Combustível / Faturamento",
                    value = String.format(java.util.Locale.getDefault(), "%.1f%%", summary.fuelCostPercentage),
                    icon = Icons.Default.LocalGasStation,
                    color = MaterialTheme.colorScheme.tertiary,
                    description = "Proporção gasta com combustível no mês"
                )

                // Valor gasto até o momento com combustível no período mensal
                EfficiencyMetricRow(
                    title = "Gasto Mensal com Combustível",
                    value = FormatUtils.formatCurrency(summary.totalFuelCost),
                    icon = Icons.Default.EvStation,
                    color = MaterialTheme.colorScheme.primary,
                    description = "Total gasto com combustível no mês atual"
                )
            }
        }


    }
}

@Composable
fun MidnightBrandHeader() {
    val isDark = isSystemInDarkTheme()
    val brandGradient = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF030508), Color(0xFF181D29))
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF334155))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(brandGradient)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 18.dp, horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Nome da marca com design incrível
                Text(
                    text = "Midnight Driver",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "O faturamento real do piloto da noite",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Ícone do Midnight Driver App em Alta Resolução
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_car_night),
                    contentDescription = "Logo Midnight Driver",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun EfficiencyMetricRow(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun BatteryOptimizationCard() {
    val context = LocalContext.current
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(true) }
    
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pm.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!isIgnoringBatteryOptimizations) {
        var showDetails by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("battery_optimization_warning_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alerta de Bateria",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acessibilidade Desativando?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Para manter o monitoramento de corridas ativo, você deve desativar a otimização de bateria.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDetails = !showDetails }
                    ) {
                        Text(
                            text = if (showDetails) "Ocultar Ajuda" else "Saber Por Que Desativa",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    // fallback
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Ajustar Bateria", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (showDetails) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Por que a acessibilidade desativa sozinha?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Economia de Bateria: O Android desliga serviços em segundo plano para poupar energia, prejudicando o monitor de tela.\n" +
                               "2. Gestão de RAM: Celulares limpam apps inativos para liberar memória.\n" +
                               "3. Bloqueios de Fabricantes (Xiaomi, Samsung, Motorola): Possuem limpadores automáticos de permissões se o app ficar ocioso.\n\n" +
                               "Como resolver definitivamente:\n" +
                               "• Clique em 'Ajustar Bateria' e mude para 'Sem restrições' (ou desligue otimização).\n" +
                               "• Bloqueie o app na tela de aplicativos recentes (toque e segure no app e ative o cadeado).\n" +
                               "• Habilite 'Início Automático' nas configurações do celular.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}


