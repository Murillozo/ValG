package com.orcafacil.app.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val projects: Flow<List<ProjectEntity>> = db.projectDao().getAll()
    val materials: Flow<List<MaterialEntity>> = db.materialDao().getAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgetDao().getAll()


    suspend fun seedIfEmpty() {
        if (db.projectDao().count() == 0) {
            val p1 = db.projectDao().insert(
                ProjectEntity(nome = "Hospital Vida", cliente = "Clínica Vida", telefone = "(11) 99999-0000", local = "São Paulo", data = "11/04/2026", descricao = "Expansão da central de gases")
            )
            db.budgetDao().insert(BudgetEntity(projectId = p1, titulo = "Pré-orçamento Hospital Vida", cliente = "Clínica Vida", localObra = "São Paulo", data = "11/04/2026", total = 0.0))
        }
        if (db.materialDao().count() == 0) {
            val base = listOf(
                MaterialEntity(nome = "Tubo cobre 28 mm", categoria = "Tubulação", unidade = "m", valorUnitario = 89.9),
                MaterialEntity(nome = "Cotovelo 15 mm", categoria = "Conexão", unidade = "un", valorUnitario = 12.5),
                MaterialEntity(nome = "Válvula esfera 1\"", categoria = "Válvulas", unidade = "un", valorUnitario = 78.0)
            )
            base.forEach { db.materialDao().insert(it) }
        }
    }

    suspend fun saveProject(project: ProjectEntity) =
        if (project.id == 0L) db.projectDao().insert(project) else {
            db.projectDao().update(project)
            project.id
        }

    suspend fun deleteProject(project: ProjectEntity) = db.projectDao().delete(project)

    suspend fun saveMaterial(material: MaterialEntity) =
        if (material.id == 0L) db.materialDao().insert(material) else {
            db.materialDao().update(material)
            material.id
        }

    suspend fun deleteMaterial(material: MaterialEntity) = db.materialDao().delete(material)

    suspend fun saveBudget(budget: BudgetEntity, items: List<BudgetItemEntity>): Long {
        val budgetId = if (budget.id == 0L) db.budgetDao().insert(budget) else {
            db.budgetDao().update(budget)
            budget.id
        }
        db.budgetItemDao().deleteByBudget(budgetId)
        db.budgetItemDao().insertAll(items.map { it.copy(id = 0, budgetId = budgetId) })
        return budgetId
    }

    suspend fun duplicateBudget(budget: BudgetEntity): Long {
        val newBudget = budget.copy(id = 0, titulo = budget.titulo + " (Cópia)", status = "Rascunho")
        val newId = db.budgetDao().insert(newBudget)
        val items = db.budgetItemDao().getByBudgetOnce(budget.id)
        db.budgetItemDao().insertAll(items.map { it.copy(id = 0, budgetId = newId) })
        return newId
    }

    suspend fun deleteBudget(budget: BudgetEntity) = db.budgetDao().delete(budget)

    suspend fun updateBudgetPdf(budgetId: Long, path: String) {
        db.budgetDao().findById(budgetId)?.let { db.budgetDao().update(it.copy(pdfPath = path, status = "Enviado")) }
    }

    suspend fun getBudget(id: Long): BudgetEntity? = db.budgetDao().findById(id)
    suspend fun getProject(id: Long): ProjectEntity? = db.projectDao().findById(id)
    suspend fun getBudgetItems(id: Long): List<BudgetItemEntity> = db.budgetItemDao().getByBudgetOnce(id)
}
