package com.example.ui

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.FinanceDatabase
import com.example.data.RideShift
import kotlinx.coroutines.flow.first
import java.util.Calendar

object ExpenseNotificationHelper {
    private const val CHANNEL_ID = "fixed_expense_notifications"
    private const val SHIFT_CHANNEL_ID = "shift_notifications"
    private const val PREFS_NAME = "expense_notif_prefs"
    private const val ACTIVE_SHIFT_NOTIFICATION_ID = 999903

    fun updateActiveShiftNotification(context: Context, activeShift: RideShift?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (activeShift == null || !activeShift.isOpen) {
            notificationManager.cancel(ACTIVE_SHIFT_NOTIFICATION_ID)
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ACTIVE_SHIFT_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalCurrentEarnings = activeShift.totalEarnings
        val totalCurrentCosts = activeShift.totalCosts
        val balance = totalCurrentEarnings - totalCurrentCosts

        val formattedEarnings = FormatUtils.formatCurrency(totalCurrentEarnings)
        val formattedCosts = FormatUtils.formatCurrency(totalCurrentCosts)
        val formattedBalance = FormatUtils.formatCurrency(balance)

        val builder = NotificationCompat.Builder(context, SHIFT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Turno em Andamento 🚗")
            .setContentText("Ganhos: $formattedEarnings | Despesas: $formattedCosts | Saldo: $formattedBalance")
            .setPriority(NotificationCompat.PRIORITY_LOW) // Usa Priority Low para não incomodar toda hora com som a cada mudança
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)

        notificationManager.notify(ACTIVE_SHIFT_NOTIFICATION_ID, builder.build())
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Vencimento de Contas"
            val descriptionText = "Notificações de vencimento de compromissos fixos"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val shiftName = "Alertas de Turnos"
            val shiftDescription = "Alertas do status de turnos abertos e inatividade"
            val shiftChannel = NotificationChannel(SHIFT_CHANNEL_ID, shiftName, importance).apply {
                description = shiftDescription
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(shiftChannel)
        }
    }

    suspend fun checkAndNotifyExpenses(context: Context) {
        try {
            checkAndNotifyShiftActivity(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val database = FinanceDatabase.getDatabase(context)
        val expenses = database.financeDao().getAllFixedExpenses().first()
        val now = Calendar.getInstance()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        expenses.forEach { expense ->
            // Não notificar se a conta estiver marcada como paga
            if (expense.isPaid) return@forEach

            // Data base para o dia de vencimento no mês atual
            val dueCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, expense.dueDay)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Lista de triggers e offsets relativos ao dia de vencimento:
            // 1. Um dia antes, às 22:00
            // 2. No dia, às 06:00
            // 3. No dia, às 15:00
            val targetTriggers = listOf(
                Triple("BEFORE_22", -1, 22),
                Triple("DUE_06", 0, 6),
                Triple("DUE_15", 0, 15)
            )

            targetTriggers.forEach { (triggerType, dayOffset, hour) ->
                val targetCal = (dueCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, dayOffset)
                    set(Calendar.HOUR_OF_DAY, hour)
                }

                val diffMs = now.timeInMillis - targetCal.timeInMillis
                val twoHoursMs = 120 * 60 * 1000L // Janela de ativação de 2 horas

                if (diffMs in 0..twoHoursMs) {
                    val year = targetCal.get(Calendar.YEAR)
                    val month = targetCal.get(Calendar.MONTH)
                    val prefKey = "sent_${expense.id}_${triggerType}_${year}_${month}"
                    val alreadySent = prefs.getBoolean(prefKey, false)

                    if (!alreadySent) {
                        val formattedAmount = FormatUtils.formatCurrency(expense.amount)
                        val title = when (triggerType) {
                            "BEFORE_22" -> "Vence Amanhã: ${expense.name}"
                            else -> "Vence Hoje: ${expense.name}"
                        }
                        val message = when (triggerType) {
                            "BEFORE_22" -> "Seu compromisso mensal ${expense.name} com valor de $formattedAmount vence amanhã (às 22h)."
                            "DUE_06" -> "Aviso Importante: $formattedAmount referente a ${expense.name} vence hoje (às 06h)."
                            else -> "Aviso de Vencimento: ${expense.name} no valor de $formattedAmount vence hoje (às 15h)."
                        }

                        sendNotification(context, expense.id * 100 + triggerType.hashCode(), title, message)

                        prefs.edit().putBoolean(prefKey, true).apply()
                    }
                }
            }
        }
    }

    private fun sendNotification(context: Context, id: Int, title: String, message: String, channel: String = CHANNEL_ID) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    suspend fun checkAndNotifyShiftActivity(context: Context) {
        val database = FinanceDatabase.getDatabase(context)
        val shifts = database.financeDao().getAllShifts().first()
        val now = System.currentTimeMillis()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Assegura que app_install_time está definido para primeira checagem
        val appInstallTime = prefs.getLong("app_install_time", 0L)
        if (appInstallTime == 0L) {
            prefs.edit().putLong("app_install_time", now).apply()
        }

        val openShift = shifts.find { it.isOpen }
        if (openShift != null) {
            // Caso 1: Turno aberto há 6 horas sem acessar o app
            var lastAppAccess = prefs.getLong("last_app_access_time", 0L)
            if (lastAppAccess == 0L) {
                lastAppAccess = openShift.date
                prefs.edit().putLong("last_app_access_time", lastAppAccess).apply()
            }

            val timeWithoutAccess = now - lastAppAccess
            val sixHoursMs = 6 * 60 * 60 * 1000L

            if (timeWithoutAccess >= sixHoursMs) {
                val lastCloseNotif = prefs.getLong("last_close_shift_notif_time_${openShift.id}", 0L)
                if (now - lastCloseNotif >= sixHoursMs) {
                    sendNotification(
                        context = context,
                        id = 999901,
                        title = "Turno em Andamento",
                        message = "Você está com o turno aberto há mais de 6 horas sem acessar o app. Deseja fechar o turno?",
                        channel = SHIFT_CHANNEL_ID
                    )
                    prefs.edit().putLong("last_close_shift_notif_time_${openShift.id}", now).apply()
                }
            }
        } else {
            // Caso 2: 12 horas sem abrir um turno
            val latestShift = shifts.maxByOrNull { it.date }
            val lastShiftTime = latestShift?.date ?: prefs.getLong("app_install_time", now)

            val timeWithoutShift = now - lastShiftTime
            val twelveHoursMs = 12 * 60 * 60 * 1000L

            if (timeWithoutShift >= twelveHoursMs) {
                val lastIdleNotif = prefs.getLong("last_idle_notif_time", 0L)
                if (now - lastIdleNotif >= twelveHoursMs) {
                    sendNotification(
                        context = context,
                        id = 999902,
                        title = "Hora de trabalhar!",
                        message = "Ta com a vida ganha? não vai trabalhar não?",
                        channel = SHIFT_CHANNEL_ID
                    )
                    prefs.edit().putLong("last_idle_notif_time", now).apply()
                }
            }
        }
    }

    fun schedulePeriodicCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ExpenseAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intervalo de verificação de 15 minutos para assegurar detecção dos horários específicos
        val interval = 15 * 60 * 1000L
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5000,
            interval,
            pendingIntent
        )
    }
}
