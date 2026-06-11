package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class InDriveRide(
    val value: Double,
    val timestamp: Long,
    val isCancelled: Boolean = false,
    val originalValue: Double = 0.0
)

@Entity(tableName = "ride_shifts")
data class RideShift(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // timestamp em milissegundos
    val kmDriven: Double, // quilômetros percorridos
    val hoursWorked: Double, // horas trabalhadas
    val earningsMapString: String = "", // ex: "Uber:120.0|99:50.0"
    val costsMapString: String = "", // ex: "Combustível:40.0|Alimentação:15.0"
    val notes: String = "", // observações opcionais
    val isOpen: Boolean = false,
    val startingCash: Double = 0.0,
    val startingAccounts: Double = 0.0,
    val startOdometer: Double = 0.0,
    val endOdometer: Double = 0.0,
    val indriveRidesString: String = "", // ex: "35.5,1718035200000|24.0,1718035400000"
    val endCash: Double = 0.0
) {
    // Parser utilitário para converter as corridas individuais em uma lista de objetos
    fun getInDriveRides(): List<InDriveRide> {
        if (indriveRidesString.isBlank()) return emptyList()
        return try {
            indriveRidesString.split("|")
                .mapNotNull { item ->
                    val parts = item.split(",")
                    if (parts.size >= 2) {
                        val value = parts[0].toDoubleOrNull() ?: 0.0
                        val ts = parts[1].toLongOrNull() ?: 0L
                        val isCancelled = if (parts.size >= 3) parts[2] == "true" else false
                        val origVal = if (parts.size >= 4) parts[3].toDoubleOrNull() ?: value else value
                        InDriveRide(value, ts, isCancelled, origVal)
                    } else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parser utilitário para converter a string "Uber:120.0|99:50.0" de faturamentos de volta em Map
    fun getEarningsMap(): Map<String, Double> {
        if (earningsMapString.isBlank()) return emptyMap()
        return try {
            earningsMapString.split("|")
                .mapNotNull { item ->
                    val parts = item.split(":")
                    if (parts.size >= 2) {
                        parts[0] to (parts[1].toDoubleOrNull() ?: 0.0)
                    } else null
                }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // Parser utilitário para converter a string de custos
    fun getCostsMap(): Map<String, Double> {
        if (costsMapString.isBlank()) return emptyMap()
        return try {
            costsMapString.split("|")
                .mapNotNull { item ->
                    val parts = item.split(":")
                    if (parts.size >= 2) {
                        parts[0] to (parts[1].toDoubleOrNull() ?: 0.0)
                    } else null
                }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun withUpdatedInDriveFee(): RideShift {
        val completedRides = getInDriveRides().filter { !it.isCancelled }
        val trackedGross = completedRides.sumOf { it.value }
        val manualGross = getEarningsMap()["InDrive"] ?: 0.0
        val finalGross = maxOf(trackedGross, manualGross)
        
        val totalFee = (finalGross * 11.3) / 100.0
        val roundedFee = Math.round(totalFee * 100.0) / 100.0
        val currentCosts = getCostsMap().toMutableMap()
        
        if (roundedFee > 0.0) {
            currentCosts["Taxa inDrive"] = roundedFee
        } else {
            currentCosts.remove("Taxa inDrive")
        }
        
        val serializedCosts = currentCosts.entries.filter { it.value > 0 }.joinToString("|") { "${it.key}:${it.value}" }
        return this.copy(costsMapString = serializedCosts)
    }

    val totalEarnings: Double
        get() = getEarningsMap().values.sum()

    val totalCosts: Double
        get() = getCostsMap().values.sum()
}

@Entity(tableName = "fixed_expenses")
data class FixedExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // ex: Aluguel, Seguro, MEI
    val amount: Double, // valor mensal
    val dueDay: Int, // dia de vencimento (1 a 31)
    val isPaid: Boolean = false
)

@Entity(tableName = "finance_categories")
data class FinanceCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // Nome da categoria: Uber, Combústivel, etc.
    val type: String, // "EARNING" para Ganhos, "COST" para Custos
    val isDefault: Boolean = false // Se é um registro padrão inalterável de sistema
)
