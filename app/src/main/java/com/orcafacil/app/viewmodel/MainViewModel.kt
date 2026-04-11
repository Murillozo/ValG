package com.orcafacil.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.orcafacil.app.data.AppRepository
import com.orcafacil.app.data.BudgetEntity
import com.orcafacil.app.data.BudgetItemEntity
import com.orcafacil.app.data.MaterialEntity
import com.orcafacil.app.data.ProjectEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DraftBudgetItem(
    val descricao: String,
    val quantidade: String,
    val unidade: String,
    val valorUnitario: String
)

class MainViewModel(private val repository: AppRepository) : ViewModel() {
    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }
    private val brFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val materials = repository.materials.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val search = MutableStateFlow("")
    val budgetFilterStatus = MutableStateFlow("Todos")

    val filteredProjects = combine(projects, search) { list, q ->
        list.filter { it.nome.contains(q, true) || it.cliente.contains(q, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMaterials = combine(materials, search) { list, q ->
        list.filter { it.nome.contains(q, true) || it.categoria.contains(q, true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBudgets = combine(budgets, search, budgetFilterStatus) { list, q, status ->
        list.filter {
            (status == "Todos" || it.status == status) &&
                (it.titulo.contains(q, true) || it.cliente.contains(q, true) || it.data.contains(q, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val draftItems = MutableStateFlow(listOf<DraftBudgetItem>())

    fun addDraftItem(item: DraftBudgetItem) {
        draftItems.value = draftItems.value + item
    }

    fun removeDraftItem(index: Int) {
        draftItems.value = draftItems.value.toMutableList().also { if (index in it.indices) it.removeAt(index) }
    }

    fun clearDraft() {
        draftItems.value = emptyList()
    }

    fun moeda(valor: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
    fun hojeBr(): String = LocalDate.now().format(brFormatter)

    fun saveProject(project: ProjectEntity) = viewModelScope.launch { repository.saveProject(project) }
    fun deleteProject(project: ProjectEntity) = viewModelScope.launch { repository.deleteProject(project) }
    fun saveMaterial(material: MaterialEntity) = viewModelScope.launch { repository.saveMaterial(material) }
    fun deleteMaterial(material: MaterialEntity) = viewModelScope.launch { repository.deleteMaterial(material) }
    fun deleteBudget(budget: BudgetEntity) = viewModelScope.launch { repository.deleteBudget(budget) }
    fun duplicateBudget(budget: BudgetEntity) = viewModelScope.launch { repository.duplicateBudget(budget) }

    fun saveBudget(
        budget: BudgetEntity,
        onSaved: (Long) -> Unit = {}
    ) = viewModelScope.launch {
        val mapped = draftItems.value.map {
            BudgetItemEntity(
                budgetId = budget.id,
                descricao = it.descricao,
                quantidade = it.quantidade.toDoubleOrNull() ?: 0.0,
                unidade = it.unidade,
                valorUnitario = it.valorUnitario.toDoubleOrNull() ?: 0.0
            )
        }
        val totalItens = mapped.sumOf { it.valorTotal }
        val total = totalItens + budget.maoDeObra
        val budgetId = repository.saveBudget(budget.copy(total = total), mapped)
        onSaved(budgetId)
    }

    suspend fun getBudgetWithItems(id: Long): Pair<BudgetEntity?, List<BudgetItemEntity>> {
        return repository.getBudget(id) to repository.getBudgetItems(id)
    }

    suspend fun getProject(id: Long): ProjectEntity? = repository.getProject(id)
    fun markBudgetPdf(id: Long, path: String) = viewModelScope.launch { repository.updateBudgetPdf(id, path) }

    companion object {
        fun factory(repository: AppRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
        }
    }
}
