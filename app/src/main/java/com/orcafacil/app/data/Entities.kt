package com.orcafacil.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cliente: String,
    val telefone: String = "",
    val local: String,
    val data: String,
    val descricao: String,
    val status: String = "Em andamento"
)

@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val categoria: String,
    val unidade: String,
    val valorUnitario: Double = 0.0,
    val observacao: String = ""
)

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val titulo: String,
    val cliente: String,
    val localObra: String,
    val data: String,
    val observacoes: String = "",
    val maoDeObra: Double = 0.0,
    val total: Double = 0.0,
    val pdfPath: String = "",
    val status: String = "Rascunho"
)

@Entity(
    tableName = "budget_items",
    foreignKeys = [
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("budgetId")]
)
data class BudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val descricao: String,
    val quantidade: Double,
    val unidade: String,
    val valorUnitario: Double
) {
    val valorTotal: Double get() = quantidade * valorUnitario
}
