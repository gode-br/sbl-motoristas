package com.example.ui

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.FinanceDatabase
import com.example.data.RideShift
import com.example.data.InDriveRide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class ScreenMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    // Máquina de estado para rastreamento da corrida em andamento e conclusão segura
    private var ongoingRidePrice = 0.0
    private var ongoingRideTime = 0L
    private var hasRegisteredCurrentRide = false
    private var hasSeenCompletionStage = false
    private var hasNotifiedOngoing = false

    private fun loadState() {
        val prefs = getSharedPreferences("indrive_tracking_prefs", Context.MODE_PRIVATE)
        ongoingRidePrice = prefs.getFloat("ongoing_price", 0.0f).toDouble()
        ongoingRideTime = prefs.getLong("ongoing_time", 0L)
        hasRegisteredCurrentRide = prefs.getBoolean("registered", false)
        hasSeenCompletionStage = prefs.getBoolean("completion_stage_seen", false)
        hasNotifiedOngoing = prefs.getBoolean("notified_ongoing", false)
    }

    private fun updateState(price: Double, time: Long, registered: Boolean, completionStageSeen: Boolean = false, notifiedOngoing: Boolean = false) {
        ongoingRidePrice = price
        ongoingRideTime = time
        hasRegisteredCurrentRide = registered
        hasSeenCompletionStage = completionStageSeen
        hasNotifiedOngoing = notifiedOngoing
        
        val prefs = getSharedPreferences("indrive_tracking_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("ongoing_price", price.toFloat())
            .putLong("ongoing_time", time)
            .putBoolean("registered", registered)
            .putBoolean("completion_stage_seen", completionStageSeen)
            .putBoolean("notified_ongoing", notifiedOngoing)
            .apply()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()
        loadState()
        startPersistentForeground()
    }

    private fun startPersistentForeground() {
        val channelId = "indrive_screen_notif"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            888,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Monitoramento de Corridas Ativo")
            .setContentText("O monitor financeiro está rodando de forma persistente para registrar seus ganhos.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                888, 
                notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(888, notification)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        // Aceita monitoramento do inDrive ("sinet.startup", "indrive", etc.)
        val isInDrive = pkgName.contains("indrive", ignoreCase = true) || 
                        pkgName.contains("indriver", ignoreCase = true) ||
                        pkgName.contains("sinet", ignoreCase = true)

        if (!isInDrive) return

        val textList = mutableListOf<String>()
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            extractTexts(rootNode, textList)
        }
        
        // Também captura textos de event.source para garantir respostas ultra rápidas a popups/bottomsheets/diálogos!
        val eventSource = event.source
        if (eventSource != null) {
            val eventTexts = mutableListOf<String>()
            extractTexts(eventSource, eventTexts)
            for (t in eventTexts) {
                if (!textList.contains(t)) {
                    textList.add(t)
                }
            }
        }

        if (textList.isEmpty()) return

        val now = System.currentTimeMillis()

        // Reseta o estado antigo apenas se passaram mais de 4 horas sem alteração útil
        if (ongoingRidePrice > 0.0 && (now - ongoingRideTime > 4 * 60 * 60 * 1000)) {
            updateState(0.0, 0L, false, false)
        }

        // 1. Detecta se estamos na etapa de conclusão de viagem ("A viagem foi concluída?")
        val isCompletionStage = textList.any { text ->
            val textLower = text.lowercase(Locale.getDefault())
            textLower.contains("viagem foi concluída") ||
            textLower.contains("viagem foi concluida") ||
            textLower.contains("viagem concluída") ||
            textLower.contains("viagem concluida") ||
            textLower.contains("a viagem foi concluída") ||
            textLower.contains("a viagem foi concluida") ||
            textLower.contains("concluir viagem") ||
            textLower.contains("finalizar viagem") ||
            textLower.contains("corrida concluída") ||
            textLower.contains("corrida concluida")
        }

        // 2. Detecta se estamos estritamente na tela de "corrida em andamento" (corrida aceita e ativa)
        val isOngoing = textList.any { text ->
            val textLower = text.lowercase(Locale.getDefault())
            textLower.contains("cancelar viagem") ||
            textLower.contains("estou aqui") ||
            textLower.contains("navegar") ||
            textLower.contains("passageiro a bordo") ||
            textLower.contains("iniciar viagem") ||
            textLower.contains("a caminho") ||
            textLower.contains("cheguei")
        }

        // 3. Detecta se voltamos ao feed ou tela de ofertas (tela inicial inDrive)
        val isNewOfferOrMainFeed = textList.any { text ->
            val textLower = text.lowercase(Locale.getDefault())
            textLower.contains("pedido de viagem") ||
            textLower.contains("aceitar por") ||
            textLower.contains("ofereça sua tarifa") ||
            textLower.contains("oferecer preço") ||
            textLower.contains("oferecer tarifa") ||
            textLower.contains("preço justo") ||
            textLower.contains("procurando") ||
            textLower.contains("solicitações") ||
            textLower.contains("online") ||
            textLower.contains("offline") ||
            textLower.contains("pular")
        }

        // Sempre gerar corrida concluída quando chegar na tela de conclusão
        if (isCompletionStage) {
            var tempPrice = ongoingRidePrice
            var tempTime = ongoingRideTime
            if (tempPrice <= 0.0) {
                val fallbackPrice = findPriceInTexts(textList)
                if (fallbackPrice != null && fallbackPrice > 0.0) {
                    tempPrice = fallbackPrice
                    tempTime = now
                }
            }

            if (tempPrice > 0.0 && !hasRegisteredCurrentRide) {
                updateState(tempPrice, tempTime, true, true)
                registerDetectedRide(tempPrice, "Conclusão detectada")
            }
            return
        }

        // Mantém monitoramento de corrida em andamento
        if (isOngoing) {
            val price = findPriceInTexts(textList)
            if (price != null && price > 0.0) {
                val isNewPrice = price != ongoingRidePrice
                if (isNewPrice || !hasNotifiedOngoing) {
                    updateState(price, now, false, false, true)
                    showOngoingRideNotification(price)
                }
            }
            return
        }

        // Só cancelar se voltar à tela inicial do inDrive sem passar pela tela de finalização/conclusão
        if (isNewOfferOrMainFeed && !isOngoing && !isCompletionStage) {
            if (ongoingRidePrice > 0.0 && !hasRegisteredCurrentRide) {
                if (!hasSeenCompletionStage) {
                    // Voltou ao feed sem passar pela tela de finalização/conclusão = cancelada
                    registerCancelledRide(ongoingRidePrice)
                } else {
                    // Passou pela tela anteriormente, já está devidamente concluída
                    updateState(0.0, 0L, false, false)
                }
            } else if (hasRegisteredCurrentRide || hasSeenCompletionStage || (now - ongoingRideTime > 10 * 60 * 1000)) {
                updateState(0.0, 0L, false, false)
            }
        }
    }

    private fun showOngoingRideNotification(gross: Double) {
        val channelId = "indrive_screen_notif"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            778,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Corrida em Andamento Detectada!")
            .setContentText("Valor coletado: R$ ${String.format(Locale.US, "%.2f", gross)} (Pronto para ser faturado depois)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)

        notificationManager.notify(778, builder.build())
    }

    private fun extractTexts(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        node.text?.let {
            val s = it.toString().trim()
            if (s.isNotEmpty()) {
                list.add(s)
            }
        }
        node.contentDescription?.let {
            val s = it.toString().trim()
            if (s.isNotEmpty()) {
                list.add(s)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                extractTexts(child, list)
            }
        }
    }

    private fun findPriceInTexts(textList: List<String>): Double? {
        // 1. Tenta encontrar um texto único que já contém o símbolo monetário e o valor
        for (text in textList) {
            val price = parsePrice(text)
            if (price != null && price > 0.0) {
                return price
            }
        }

        // 2. Trata elementos divididos (ex: uma tag de texto "R$" e outra separada com "35,00")
        val numberRegex = """^\s*(\d+[\.,]\d{2}|\d+)\s*$""".toRegex()
        for (i in textList.indices) {
            val text = textList[i].trim()
            if (text == "R$" || text == "$" || text.lowercase(Locale.getDefault()) == "r$") {
                // Checa itens adjacentes
                val candidateIndices = listOf(i + 1, i + 2, i - 1)
                for (idx in candidateIndices) {
                    if (idx in textList.indices) {
                        val candidate = textList[idx].trim()
                        val match = numberRegex.find(candidate)
                        if (match != null) {
                            val valStr = match.groupValues[1].replace(",", ".")
                            val parsed = valStr.toDoubleOrNull()
                            if (parsed != null && parsed > 0.0) {
                                return parsed
                            }
                        }
                    }
                }
            }
        }

        // 3. Fallback genérico: busca qualquer trecho com símbolo monetário seguido de números
        val genericRegex = """R\$\s*(\d+[\.,]\d{2}|\d+)""".toRegex(RegexOption.IGNORE_CASE)
        for (text in textList) {
            val clean = text.replace("\n", " ").replace("\r", " ").trim()
            val match = genericRegex.find(clean)
            if (match != null) {
                val valStr = match.groupValues[1].replace(",", ".")
                val parsed = valStr.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }

        return null
    }

    private fun parsePrice(text: String): Double? {
        // Suporta formatos comuns como "R$ 35,50", "R$35", "$ 35.50", "R$ 15.00"
        val regexCents = """(?:R\$|\$)\s*(\d+[\.,]\d{2})""".toRegex(RegexOption.IGNORE_CASE)
        val regexNoCents = """(?:R\$|\$)\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)

        val matchCents = regexCents.find(text)
        if (matchCents != null) {
            val valueStr = matchCents.groupValues[1].replace(",", ".")
            return valueStr.toDoubleOrNull()
        }

        val matchNoCents = regexNoCents.find(text)
        if (matchNoCents != null) {
            val valueStr = matchNoCents.groupValues[1]
            return valueStr.toDoubleOrNull()
        }

        return null
    }

    private fun registerDetectedRide(grossValue: Double, rawText: String) {
        val database = FinanceDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()

        serviceScope.launch {
            try {
                val shifts = dao.getAllShifts().first()
                val activeShift = shifts.find { it.isOpen }
                if (activeShift != null) {
                    val currentRides = activeShift.getInDriveRides()
                    val nowMs = System.currentTimeMillis()
                    
                    // Deduplicação estrita: ignora se houver corrida de mesmo valor salva há menos de 3 min,
                    // ou qualquer corrida salva há menos de 45 segundos (fisicamente impossível faturar tão rápido).
                    val lastSavedRide = currentRides.lastOrNull { !it.isCancelled }
                    if (lastSavedRide != null) {
                        val timeDiff = nowMs - lastSavedRide.timestamp
                        val isSamePriceDuplicate = lastSavedRide.value == grossValue && timeDiff < 3 * 60 * 1000
                        val isTooFastForAnyRide = timeDiff < 45 * 1000
                        if (isSamePriceDuplicate || isTooFastForAnyRide) {
                            return@launch
                        }
                    }

                    // Adiciona ganho à categoria InDrive
                    val currentMap = activeShift.getEarningsMap().toMutableMap()
                    val category = "InDrive"
                    currentMap[category] = (currentMap[category] ?: 0.0) + grossValue
                    val serializedEarnings = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }

                    // Salva a corrida individualmente com timestamp
                    val newRideString = "$grossValue,${System.currentTimeMillis()}"
                    val updatedInDriveRidesStr = if (activeShift.indriveRidesString.isBlank()) {
                        newRideString
                    } else {
                        activeShift.indriveRidesString + "|" + newRideString
                    }

                    // Atualiza o turno no banco de dados
                    dao.insertShift(activeShift.copy(
                        earningsMapString = serializedEarnings,
                        indriveRidesString = updatedInDriveRidesStr
                    ))

                    // Feedback de sistema via notificação
                    showRideNotification(grossValue)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showRideNotification(gross: Double) {
        val channelId = "indrive_screen_notif"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Remove a notificação temporária de corrida em andamento
        notificationManager.cancel(778)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Corrida Contabilizada com Sucesso!")
            .setContentText("A corrida de R$ ${String.format(Locale.US, "%.2f", gross)} foi registrada e somada ao balanço do seu turno.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(777, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "indrive_screen_notif"
            val name = "Leitura de Tela inDrive"
            val desc = "Notificações de corridas capturadas da tela em tempo real"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun handleRideCancellation() {
        // Obsoleto: Retorna imediatamente para evitar qualquer risco de modificar ou deletar faturamentos anteriores
        return
    }

    private fun handleRideCancellationObsolete() {
        val database = FinanceDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()

        serviceScope.launch {
            try {
                val shifts = dao.getAllShifts().first()
                val activeShift = shifts.find { it.isOpen }
                if (activeShift != null) {
                    val list = activeShift.getInDriveRides().toMutableList()
                    if (list.isNotEmpty()) {
                        // Buscamos a última corrida que ainda NÃO está marcada como cancelada
                        val lastIndex = list.indexOfLast { !it.isCancelled }
                        if (lastIndex != -1) {
                            val lastRide = list[lastIndex]
                            val originalVal = lastRide.value
                            
                            // Marca como cancelada (zerando o valor a ser contabilizado)
                            val updatedRide = lastRide.copy(value = 0.0, isCancelled = true, originalValue = originalVal)
                            list[lastIndex] = updatedRide
                            
                            // Re-serializa
                            val updatedRidesString = list.joinToString("|") { 
                                "${it.value},${it.timestamp},${it.isCancelled},${it.originalValue}" 
                            }
                            
                            // Deduz o valor dos ganhos da categoria InDrive
                            val currentMap = activeShift.getEarningsMap().toMutableMap()
                            val category = "InDrive"
                            val prevVal = currentMap[category] ?: 0.0
                            val newVal = maxOf(0.0, prevVal - originalVal)
                            currentMap[category] = newVal
                            val serializedEarnings = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
                            
                            // Atualiza no banco de dados
                            dao.insertShift(activeShift.copy(
                                earningsMapString = serializedEarnings,
                                indriveRidesString = updatedRidesString
                            ))
                            
                            // Reseta o estado local do rastreador para que ignore o valor anterior
                            updateState(0.0, 0L, false)
                            
                            // Mostra notificação amigável de cancelamento
                            showCancellationNotification(originalVal)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun registerCancelledRide(originalVal: Double) {
        val database = FinanceDatabase.getDatabase(applicationContext)
        val dao = database.financeDao()

        serviceScope.launch {
            try {
                val shifts = dao.getAllShifts().first()
                val activeShift = shifts.find { it.isOpen }
                if (activeShift != null) {
                    val currentRides = activeShift.getInDriveRides()
                    val nowMs = System.currentTimeMillis()
                    
                    // Deduplicação de cancelamento: impede salvar o mesmo cancelamento repetidamente nos últimos 90 segundos
                    val lastSavedRide = currentRides.lastOrNull()
                    if (lastSavedRide != null && lastSavedRide.isCancelled && lastSavedRide.originalValue == originalVal) {
                        if (nowMs - lastSavedRide.timestamp < 90 * 1000) {
                            return@launch
                        }
                    }

                    val list = currentRides.toMutableList()
                    val newRide = InDriveRide(value = 0.0, timestamp = nowMs, isCancelled = true, originalValue = originalVal)
                    list.add(newRide)

                    val updatedRidesString = list.joinToString("|") { 
                        "${it.value},${it.timestamp},${it.isCancelled},${it.originalValue}" 
                    }

                    // Atualiza no banco de dados (não altera o mapa de ganhos pois não foi concluída)
                    dao.insertShift(activeShift.copy(
                        indriveRidesString = updatedRidesString
                    ))

                    // Reseta o estado local do rastreador
                    updateState(0.0, 0L, false)

                    // Mostra notificação amigável de cancelamento / não finalização
                    showCancellationNotification(originalVal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCancellationNotification(gross: Double) {
        val channelId = "indrive_screen_notif"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancela notificações anteriores de corrida
        notificationManager.cancel(777)
        notificationManager.cancel(778)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Corrida Cancelada / Não Finalizada")
            .setContentText("A corrida de R$ ${String.format(Locale.US, "%.2f", gross)} foi cancelada pelo motorista e os ganhos foram removidos.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(779, builder.build())
    }

    override fun onInterrupt() {}
}
