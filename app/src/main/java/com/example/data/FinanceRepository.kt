package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {

    // --- Turnos Diários ---
    val allShifts: Flow<List<RideShift>> = financeDao.getAllShifts()

    suspend fun insertShift(shift: RideShift) {
        financeDao.insertShift(shift)
    }

    suspend fun deleteShift(id: Int) {
        financeDao.deleteShift(id)
    }

    // --- Despesas Fixas ---
    val allFixedExpenses: Flow<List<FixedExpense>> = financeDao.getAllFixedExpenses()

    suspend fun insertFixedExpense(expense: FixedExpense) {
        financeDao.insertFixedExpense(expense)
    }

    suspend fun updateFixedExpense(expense: FixedExpense) {
        financeDao.updateFixedExpense(expense)
    }

    suspend fun deleteFixedExpense(id: Int) {
        financeDao.deleteFixedExpense(id)
    }

    // --- Categorias de Ganhos e Custos ---
    val allCategories: Flow<List<FinanceCategory>> = financeDao.getAllCategories()

    suspend fun insertCategory(category: FinanceCategory) {
        financeDao.insertCategory(category)
    }

    suspend fun updateCategory(category: FinanceCategory) {
        financeDao.updateCategory(category)
    }

    suspend fun deleteCategory(id: Int) {
        financeDao.deleteCategory(id)
    }

    suspend fun prePopulateDefaultsIfEmpty() {
        val count = financeDao.getCategoryCount()
        if (count == 0) {
            // Ganhos padrão
            val earnings = listOf("Uber", "99", "InDrive", "Particular")
            for (earn in earnings) {
                financeDao.insertCategory(FinanceCategory(name = earn, type = "EARNING", isDefault = true))
            }
            // Custos padrão
            val costs = listOf("Combustível", "Alimentação", "Manutenção", "Taxa inDrive", "Outros")
            for (cost in costs) {
                financeDao.insertCategory(FinanceCategory(name = cost, type = "COST", isDefault = true))
            }
        }
    }

    suspend fun clearLocalData() {
        financeDao.clearShifts()
        financeDao.clearFixedExpenses()
        financeDao.clearCategories()
    }
}
