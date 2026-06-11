package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FinanceCategory
import com.example.data.FinanceRepository
import com.example.data.FixedExpense
import com.example.data.RideShift
import com.example.data.FirebaseSyncManager
import com.example.data.SyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class FinancialSummary(
    val totalEarnings: Double = 0.0,
    val totalFuelCost: Double = 0.0,
    val totalOtherCost: Double = 0.0,
    val totalDailyCost: Double = 0.0,
    val totalFixedExpenses: Double = 0.0,
    val totalNetProfit: Double = 0.0,
    val totalKm: Double = 0.0,
    val totalHours: Double = 0.0,
    val earningsPerKm: Double = 0.0,
    val costPerKm: Double = 0.0,
    val netPerHour: Double = 0.0,
    val fuelCostPercentage: Double = 0.0,
    val weekdayAverageEarnings: Double = 0.0,
    val currentWeekdayName: String = ""
)

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    init {
        // Inicializa as categorias padrão e tenta sincronizar com a nuvem na inicialização se logado
        viewModelScope.launch {
            repository.prePopulateDefaultsIfEmpty()
            
            // Delay curto para que os StateFlows sejam povoados com dados do Room
            kotlinx.coroutines.delay(1200)
            val user = FirebaseSyncManager.currentUser.value
            if (user != null) {
                FirebaseSyncManager.syncOnLogin(
                    repository = repository,
                    localShifts = allShifts.value,
                    localFixed = allFixedExpenses.value,
                    localCategories = allCategories.value
                )
            }
        }
    }

    // Lista reativa dos turnos de corrida
    val allShifts: StateFlow<List<RideShift>> = repository.allShifts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista reativa de despesas mensais fixas
    val allFixedExpenses: StateFlow<List<FixedExpense>> = repository.allFixedExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lista reativa de categorias customizáveis de ganhos & despesas
    val allCategories: StateFlow<List<FinanceCategory>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Resumo financeiro de metas e faturamento dinâmicos baseado nos mapas customizados
    val summaryState: StateFlow<FinancialSummary> = combine(
        allShifts,
        allFixedExpenses
    ) { shifts, fixed ->
        val currentMonthCal = Calendar.getInstance()
        val currentYear = currentMonthCal.get(Calendar.YEAR)
        val currentMonth = currentMonthCal.get(Calendar.MONTH)

        // Primeiro dia do mês atual às 00:00:00.000
        val firstDayOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Último dia do mês atual às 23:59:59.999
        val lastDayOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Filtra os turnos para considerar apenas o início no dia 1 do mês atual até o último dia
        val currentMonthShifts = shifts.filter { shift ->
            shift.date in firstDayOfMonth..lastDayOfMonth
        }

        var totalEarnings = 0.0
        var totalFuel = 0.0
        var totalOther = 0.0
        var totalDailyCost = 0.0

        for (shift in currentMonthShifts) {
            val earningsMap = shift.getEarningsMap()
            val costsMap = shift.getCostsMap()

            totalEarnings += earningsMap.values.sum()
            totalDailyCost += costsMap.values.sum()

            // Filtro dinâmico para contabilizar combustível de maneira isolada nas estatísticas
            for ((catName, value) in costsMap) {
                if (catName.contains("combustível", ignoreCase = true) || catName.contains("combustivel", ignoreCase = true) || catName.contains("posto", ignoreCase = true) || catName.contains("gas", ignoreCase = true)) {
                    totalFuel += value
                } else {
                    totalOther += value
                }
            }
        }

        val totalFixed = fixed.sumOf { it.amount }
        val totalNet = totalEarnings - totalDailyCost - totalFixed
        val totalKm = currentMonthShifts.sumOf { it.kmDriven }
        val totalHours = currentMonthShifts.sumOf { it.hoursWorked }

        val earningsPerKm = if (totalKm > 0) totalEarnings / totalKm else 0.0
        val costPerKm = if (totalKm > 0) totalDailyCost / totalKm else 0.0
        val netPerHour = if (totalHours > 0) (totalEarnings - totalDailyCost) / totalHours else 0.0
        val fuelPercentage = if (totalEarnings > 0) (totalFuel / totalEarnings) * 100.0 else 0.0

        // Média diária referente ao dia da semana de hoje
        val todayCal = Calendar.getInstance()
        val todayDayOfWeek = todayCal.get(Calendar.DAY_OF_WEEK)
        val currentWeekdayName = SimpleDateFormat("EEEE", Locale("pt", "BR"))
            .format(todayCal.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }

        // Filtra turnos do dia de semana correspondente que tenham de fato sido trabalhados (excluindo dias não trabalhados)
        val sameWeekdayShifts = shifts.filter { shift ->
            val shiftCal = Calendar.getInstance().apply {
                timeInMillis = shift.date
            }
            val isSameDayOfWeek = shiftCal.get(Calendar.DAY_OF_WEEK) == todayDayOfWeek
            val isWorked = shift.totalEarnings > 0.0 || shift.hoursWorked > 0.0
            isSameDayOfWeek && isWorked
        }

        val weekdayAverageEarnings = if (sameWeekdayShifts.isNotEmpty()) {
            val earningsByDay = sameWeekdayShifts.groupBy { shift ->
                val cal = Calendar.getInstance().apply { timeInMillis = shift.date }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
            }
            val totalPeriodEarnings = sameWeekdayShifts.sumOf { it.totalEarnings }
            totalPeriodEarnings / earningsByDay.size
        } else {
            0.0
        }

        FinancialSummary(
            totalEarnings = totalEarnings,
            totalFuelCost = totalFuel,
            totalOtherCost = totalOther,
            totalDailyCost = totalDailyCost,
            totalFixedExpenses = totalFixed,
            totalNetProfit = totalNet,
            totalKm = totalKm,
            totalHours = totalHours,
            earningsPerKm = earningsPerKm,
            costPerKm = costPerKm,
            netPerHour = netPerHour,
            fuelCostPercentage = fuelPercentage,
            weekdayAverageEarnings = weekdayAverageEarnings,
            currentWeekdayName = currentWeekdayName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialSummary()
    )

    // --- Métodos de Ganhos e Custos Diários (RideShift) ---
    fun startNewActiveShift(startingCash: Double, startingAccounts: Double, startOdometer: Double, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val shift = RideShift(
                date = date,
                kmDriven = 0.0,
                hoursWorked = 0.0,
                earningsMapString = "",
                costsMapString = "",
                notes = "",
                isOpen = true,
                startingCash = startingCash,
                startingAccounts = startingAccounts,
                startOdometer = startOdometer,
                endOdometer = startOdometer
            )
            repository.insertShift(shift)
            triggerAutoBackup()
        }
    }

    fun addEarningToShift(shiftId: Int, category: String, amount: Double) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val currentMap = targetShift.getEarningsMap().toMutableMap()
            currentMap[category] = (currentMap[category] ?: 0.0) + amount
            val serialized = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            
            repository.insertShift(targetShift.copy(earningsMapString = serialized))
            triggerAutoBackup()
        }
    }

    fun addCostToShift(shiftId: Int, category: String, amount: Double) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val currentMap = targetShift.getCostsMap().toMutableMap()
            currentMap[category] = (currentMap[category] ?: 0.0) + amount
            val serialized = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            
            repository.insertShift(targetShift.copy(costsMapString = serialized))
            triggerAutoBackup()
        }
    }

    fun updateShiftDetails(shiftId: Int, kmDriven: Double, hoursWorked: Double, notes: String, date: Long? = null) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            repository.insertShift(targetShift.copy(
                kmDriven = kmDriven,
                hoursWorked = hoursWorked,
                notes = notes,
                date = date ?: targetShift.date
            ))
            triggerAutoBackup()
        }
    }

    fun closeShift(shiftId: Int, kmDriven: Double, hoursWorked: Double, notes: String, endOdometer: Double = 0.0, endCash: Double = 0.0) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val finalEndOdm = if (endOdometer > 0.0) endOdometer else targetShift.startOdometer + kmDriven
            val finalKmDriven = if (endOdometer > 0.0) (endOdometer - targetShift.startOdometer).coerceAtLeast(0.0) else kmDriven
            repository.insertShift(targetShift.copy(
                kmDriven = finalKmDriven,
                hoursWorked = hoursWorked,
                notes = notes,
                endOdometer = finalEndOdm,
                endCash = endCash,
                isOpen = false
            ))
            triggerAutoBackup()
        }
    }

    fun addShift(
        earningsMap: Map<String, Double>,
        costsMap: Map<String, Double>,
        kmDriven: Double,
        hoursWorked: Double,
        notes: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            // Serializa os mapas para strings fáceis de armazenar ("Uber:200.0|99:50.0")
            val earningsString = earningsMap.entries
                .filter { it.value > 0 }
                .joinToString("|") { "${it.key}:${it.value}" }
            
            val costsString = costsMap.entries
                .filter { it.value > 0 }
                .joinToString("|") { "${it.key}:${it.value}" }

            val shift = RideShift(
                date = date,
                kmDriven = kmDriven,
                hoursWorked = hoursWorked,
                earningsMapString = earningsString,
                costsMapString = costsString,
                notes = notes,
                isOpen = false
            )
            repository.insertShift(shift)
            triggerAutoBackup()
        }
    }

    fun deleteShift(id: Int) {
        viewModelScope.launch {
            repository.deleteShift(id)
            triggerAutoBackup()
        }
    }

    fun removeEarningFromShift(shiftId: Int, category: String) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val currentMap = targetShift.getEarningsMap().toMutableMap()
            currentMap.remove(category)
            val serialized = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            repository.insertShift(targetShift.copy(earningsMapString = serialized))
            triggerAutoBackup()
        }
    }

    fun editEarningInShift(shiftId: Int, category: String, newAmount: Double) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val currentMap = targetShift.getEarningsMap().toMutableMap()
            if (newAmount > 0.0) {
                currentMap[category] = newAmount
            } else {
                currentMap.remove(category)
            }
            val serialized = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            repository.insertShift(targetShift.copy(earningsMapString = serialized))
            triggerAutoBackup()
        }
    }

    fun removeCostFromShift(shiftId: Int, category: String) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val currentMap = targetShift.getCostsMap().toMutableMap()
            currentMap.remove(category)
            val serialized = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            repository.insertShift(targetShift.copy(costsMapString = serialized))
            triggerAutoBackup()
        }
    }

    fun editCostInShift(shiftId: Int, category: String, newAmount: Double) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val currentMap = targetShift.getCostsMap().toMutableMap()
            if (newAmount > 0.0) {
                currentMap[category] = newAmount
            } else {
                currentMap.remove(category)
            }
            val serialized = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            repository.insertShift(targetShift.copy(costsMapString = serialized))
            triggerAutoBackup()
        }
    }

    fun editStartingCashInShift(shiftId: Int, newAmount: Double) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            repository.insertShift(targetShift.copy(startingCash = newAmount))
            triggerAutoBackup()
        }
    }

    fun editEndCashInShift(shiftId: Int, newAmount: Double) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            repository.insertShift(targetShift.copy(endCash = newAmount))
            triggerAutoBackup()
        }
    }

    fun removeInDriveRideFromShift(shiftId: Int, timestamp: Long) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val rides = targetShift.getInDriveRides().toMutableList()
            rides.removeAll { it.timestamp == timestamp }
            val serialized = rides.joinToString("|") { "${it.value},${it.timestamp},${it.isCancelled},${it.originalValue}" }
            
            val completedRides = rides.filter { !it.isCancelled }
            val totalGross = completedRides.sumOf { it.value }
            val currentMap = targetShift.getEarningsMap().toMutableMap()
            if (totalGross > 0.0) {
                currentMap["InDrive"] = totalGross
            } else {
                currentMap.remove("InDrive")
            }
            val serializedEarnings = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            
            repository.insertShift(targetShift.copy(
                indriveRidesString = serialized,
                earningsMapString = serializedEarnings
            ))
            triggerAutoBackup()
        }
    }

    fun editInDriveRideInShift(shiftId: Int, timestamp: Long, newValue: Double, isCancelled: Boolean) {
        viewModelScope.launch {
            val shifts = allShifts.value
            val targetShift = shifts.find { it.id == shiftId } ?: return@launch
            val rides = targetShift.getInDriveRides().map { 
                if (it.timestamp == timestamp) {
                    val finalVal = if (isCancelled) 0.0 else newValue
                    val origVal = if (isCancelled) newValue else 0.0
                    it.copy(value = finalVal, isCancelled = isCancelled, originalValue = origVal)
                } else it
            }
            val serialized = rides.joinToString("|") { "${it.value},${it.timestamp},${it.isCancelled},${it.originalValue}" }
            
            val completedRides = rides.filter { !it.isCancelled }
            val totalGross = completedRides.sumOf { it.value }
            val currentMap = targetShift.getEarningsMap().toMutableMap()
            if (totalGross > 0.0) {
                currentMap["InDrive"] = totalGross
            } else {
                currentMap.remove("InDrive")
            }
            val serializedEarnings = currentMap.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
            
            repository.insertShift(targetShift.copy(
                indriveRidesString = serialized,
                earningsMapString = serializedEarnings
            ))
            triggerAutoBackup()
        }
    }

    // --- Métodos de Despesas Fixas (FixedExpense) ---
    fun addFixedExpense(name: String, amount: Double, dueDay: Int, isPaid: Boolean = false) {
        viewModelScope.launch {
            val expense = FixedExpense(
                name = name,
                amount = amount,
                dueDay = dueDay,
                isPaid = isPaid
            )
            repository.insertFixedExpense(expense)
            triggerAutoBackup()
        }
    }

    fun toggleFixedExpensePaid(expense: FixedExpense) {
        viewModelScope.launch {
            val updated = expense.copy(isPaid = !expense.isPaid)
            repository.updateFixedExpense(updated)
            triggerAutoBackup()
        }
    }

    fun deleteFixedExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteFixedExpense(id)
            triggerAutoBackup()
        }
    }

    // --- Métodos de Categorias Dinâmicas (FinanceCategory) ---
    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(
                FinanceCategory(
                    name = name,
                    type = type,
                    isDefault = false
                )
            )
            triggerAutoBackup()
        }
    }

    fun updateCategory(category: FinanceCategory) {
        viewModelScope.launch {
            repository.updateCategory(category)
            triggerAutoBackup()
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            triggerAutoBackup()
        }
    }

    // Sincronização automática silenciosa em segundo plano após alterações locais
    private fun triggerAutoBackup() {
        val user = FirebaseSyncManager.currentUser.value ?: return
        val status = FirebaseSyncManager.syncState.value
        
        // Se já está carregando dados da nuvem, evitamos a autosincronização para não sobrescrever em loop
        if (status is SyncStatus.Loading) return
        
        viewModelScope.launch {
            // Debounce curto para agrupamento caso haja alterações em rajada
            kotlinx.coroutines.delay(1500)
            // Se o usuário ainda estiver logado e nenhum download/sincronização ativa de carregamento esteja rodando
            if (FirebaseSyncManager.currentUser.value?.uid == user.uid && FirebaseSyncManager.syncState.value !is SyncStatus.Loading) {
                FirebaseSyncManager.backupToCloud(allShifts.value, allFixedExpenses.value, allCategories.value)
            }
        }
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
