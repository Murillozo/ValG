package com.orcafacil.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT COUNT(*) FROM projects")
    suspend fun count(): Int

    @Query("SELECT * FROM projects ORDER BY data DESC")
    fun getAll(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun findById(id: Long): ProjectEntity?
}

@Dao
interface MaterialDao {
    @Query("SELECT COUNT(*) FROM materials")
    suspend fun count(): Int

    @Query("SELECT * FROM materials ORDER BY nome ASC")
    fun getAll(): Flow<List<MaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: MaterialEntity): Long

    @Update
    suspend fun update(material: MaterialEntity)

    @Delete
    suspend fun delete(material: MaterialEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY data DESC")
    fun getAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun findById(id: Long): BudgetEntity?
}

@Dao
interface BudgetItemDao {
    @Query("SELECT * FROM budget_items WHERE budgetId = :budgetId")
    fun getByBudget(budgetId: Long): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items WHERE budgetId = :budgetId")
    suspend fun getByBudgetOnce(budgetId: Long): List<BudgetItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BudgetItemEntity>)

    @Query("DELETE FROM budget_items WHERE budgetId = :budgetId")
    suspend fun deleteByBudget(budgetId: Long)
}
