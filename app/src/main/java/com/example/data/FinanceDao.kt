package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // ---- Métodos de Turnos Diários (RideShift) ----
    @Query("SELECT * FROM ride_shifts ORDER BY date DESC")
    fun getAllShifts(): Flow<List<RideShift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftRaw(shift: RideShift)

    @Transaction
    suspend fun insertShift(shift: RideShift) {
        val updatedShift = shift.withUpdatedInDriveFee()
        insertShiftRaw(updatedShift)
    }

    @Query("DELETE FROM ride_shifts WHERE id = :id")
    suspend fun deleteShift(id: Int)

    // ---- Métodos de Despesas Fixas (FixedExpense) ----
    @Query("SELECT * FROM fixed_expenses ORDER BY dueDay ASC")
    fun getAllFixedExpenses(): Flow<List<FixedExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpense(expense: FixedExpense)

    @Update
    suspend fun updateFixedExpense(expense: FixedExpense)

    @Query("DELETE FROM fixed_expenses WHERE id = :id")
    suspend fun deleteFixedExpense(id: Int)

    // ---- Métodos de Categorias Dinâmicas (FinanceCategory) ----
    @Query("SELECT * FROM finance_categories ORDER BY type DESC, name ASC")
    fun getAllCategories(): Flow<List<FinanceCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FinanceCategory)

    @Update
    suspend fun updateCategory(category: FinanceCategory)

    @Query("DELETE FROM finance_categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    @Query("SELECT COUNT(*) FROM finance_categories")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM ride_shifts")
    suspend fun clearShifts()

    @Query("DELETE FROM fixed_expenses")
    suspend fun clearFixedExpenses()

    @Query("DELETE FROM finance_categories")
    suspend fun clearCategories()
}
