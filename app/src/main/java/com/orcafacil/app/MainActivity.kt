package com.orcafacil.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orcafacil.app.data.BudgetEntity
import com.orcafacil.app.data.MaterialEntity
import com.orcafacil.app.data.ProjectEntity
import com.orcafacil.app.pdf.PdfGenerator
import com.orcafacil.app.ui.theme.OrcaFacilTheme
import com.orcafacil.app.viewmodel.DraftBudgetItem
import com.orcafacil.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

enum class AppTab { HOME, PROJETOS, MATERIAIS, ORCAMENTOS, NOVO }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = AppContainer.repository(this)
        setContent {
            OrcaFacilTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModel.factory(repository))
                AppScreen(vm)
            }
        }
    }
}

@Composable
fun AppScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == AppTab.HOME, onClick = { tab = AppTab.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
                NavigationBarItem(selected = tab == AppTab.PROJETOS, onClick = { tab = AppTab.PROJETOS }, icon = { Icon(Icons.Default.Work, null) }, label = { Text("Projetos") })
                NavigationBarItem(selected = tab == AppTab.MATERIAIS, onClick = { tab = AppTab.MATERIAIS }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Materiais") })
                NavigationBarItem(selected = tab == AppTab.ORCAMENTOS, onClick = { tab = AppTab.ORCAMENTOS }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Orçamentos") })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            SearchField(vm)
            Spacer(Modifier.height(12.dp))
            when (tab) {
                AppTab.HOME -> HomeScreen(onNovo = { tab = AppTab.NOVO }, onProjetos = { tab = AppTab.PROJETOS }, onMateriais = { tab = AppTab.MATERIAIS }, onRecentes = { tab = AppTab.ORCAMENTOS })
                AppTab.PROJETOS -> ProjectsScreen(vm)
                AppTab.MATERIAIS -> MaterialsScreen(vm, onUseMaterial = {
                    vm.selectMaterialForBudget(it)
                    tab = AppTab.NOVO
                })
                AppTab.ORCAMENTOS -> BudgetsScreen(vm, onNovo = { tab = AppTab.NOVO }, onShare = { uri ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setPackage("com.whatsapp")
                    }
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            val generic = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(generic, "Compartilhar orçamento"))
                        }
                }, showMsg = { msg -> scope.launch { snackbar.showSnackbar(msg) } })
                AppTab.NOVO -> CreateBudgetScreen(vm, showMsg = { msg -> scope.launch { snackbar.showSnackbar(msg) } }, onDone = {
                    scope.launch { snackbar.showSnackbar("Orçamento salvo com sucesso") }
                    tab = AppTab.ORCAMENTOS
                })
            }
        }
    }
}

@Composable
fun SearchField(vm: MainViewModel) {
    val q by vm.search.collectAsState()
    OutlinedTextField(value = q, onValueChange = { vm.search.value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Buscar projeto, cliente ou material") })
}

@Composable
fun HomeScreen(onNovo: () -> Unit, onProjetos: () -> Unit, onMateriais: () -> Unit, onRecentes: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BigButton("Novo Orçamento", onNovo)
        BigButton("Projetos", onProjetos)
        BigButton("Equipamentos / Materiais", onMateriais)
        BigButton("Orçamentos Recentes", onRecentes)
        Text("Fluxo simples: 1) escolha projeto, 2) adicione itens, 3) gere PDF.")
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(14.dp)) { Text(text) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LocationField(value: String, onValueChange: (String) -> Unit, onMessage: (String) -> Unit = {}) {
    val context = LocalContext.current
    val cityOptions = remember {
        listOf(
            "Goiânia - GO",
            "São Paulo - SP",
            "Brasília - DF"
        )
    }
    var cityExpanded by remember { mutableStateOf(false) }
    var suggestedCurrentLocation by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) {
            suggestedCurrentLocation = resolveCurrentLocationText(context)
            if (suggestedCurrentLocation == null) onMessage("Não foi possível obter a localização atual.")
        } else {
            onMessage("Permita a localização para sugerir automaticamente.")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { cityExpanded = it }) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("Local*") },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) }
            )
            ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                cityOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        onValueChange(option)
                        cityExpanded = false
                    })
                }
            }
        }
        Button(onClick = {
            val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                suggestedCurrentLocation = resolveCurrentLocationText(context)
                if (suggestedCurrentLocation == null) onMessage("Não foi possível obter a localização atual.")
            } else {
                permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }) { Text("Usar localização atual") }

        suggestedCurrentLocation?.let { localAtual ->
            Text("Local detectado: $localAtual. Esse é o local da obra/projeto?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onValueChange(localAtual)
                    suggestedCurrentLocation = null
                }) { Text("Sim, usar este") }
                Button(onClick = { suggestedCurrentLocation = null }) { Text("Outro local") }
            }
        }
    }
}

private fun resolveCurrentLocationText(context: Context): String? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = manager.getProviders(true)
    val bestLocation = providers
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time } ?: return null
    return bestLocation.toReadableAddress(context)
}

private fun Location.toReadableAddress(context: Context): String {
    return runCatching {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        @Suppress("DEPRECATION")
        val result = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        val city = result?.subAdminArea ?: result?.locality
        val state = result?.adminArea
        listOfNotNull(city, state).joinToString(" - ").ifBlank { "$latitude, $longitude" }
    }.getOrElse { "$latitude, $longitude" }
}

@Composable
fun ProjectsScreen(vm: MainViewModel) {
    val list by vm.filteredProjects.collectAsState()
    var nome by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Projetos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome do projeto*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cliente, { cliente = it }, label = { Text("Cliente*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(telefone, { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())
            LocationField(value = local, onValueChange = { local = it })
            BigButton("Salvar Projeto") {
                if (nome.isBlank() || cliente.isBlank() || local.isBlank()) return@BigButton
                vm.saveProject(ProjectEntity(nome = nome, cliente = cliente, telefone = telefone, local = local, data = vm.hojeBr(), descricao = ""))
                nome = ""; cliente = ""; telefone = ""; local = ""
            }
        }
        items(list) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(it.nome, fontWeight = FontWeight.Bold)
                    Text("Cliente: ${it.cliente} • ${it.status}")
                    Text("Local: ${it.local}")
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { vm.deleteProject(it) }) { Text("Excluir") }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MaterialsScreen(vm: MainViewModel, onUseMaterial: (MaterialEntity) -> Unit) {
    val list by vm.filteredMaterials.collectAsState()
    val categories = list.map { it.categoria }.distinct().sorted()
    val commonUnits = listOf("un", "m", "m²", "m³", "kg", "g", "l", "ml", "cx", "pct", "rolo", "kit")
    var nome by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("") }
    var un by remember { mutableStateOf("un") }
    var valor by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Equipamentos / Materiais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(nome, { nome = it }, label = { Text("Nome do item*") }, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(cat, { cat = it }, label = { Text("Categoria*") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) })
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    categories.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            cat = option
                            catExpanded = false
                        })
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
                OutlinedTextField(un, { un = it }, label = { Text("Unidade") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) })
                ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                    commonUnits.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            un = option
                            unitExpanded = false
                        })
                    }
                }
            }
            OutlinedTextField(valor, { valor = it }, label = { Text("Valor unitário (R$)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            BigButton("Salvar Material") {
                if (nome.isBlank() || cat.isBlank()) return@BigButton
                vm.saveMaterial(MaterialEntity(nome = nome, categoria = cat, unidade = un, valorUnitario = valor.replace(',', '.').toDoubleOrNull() ?: 0.0))
                nome = ""; cat = ""; un = "un"; valor = ""
            }
        }
        items(list) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(it.nome, fontWeight = FontWeight.Bold)
                        Text("${it.categoria} • ${it.unidade} • ${vm.moeda(it.valorUnitario)}")
                    }
                    Button(onClick = { onUseMaterial(it) }) { Text("Usar") }
                    IconButton(onClick = { vm.deleteMaterial(it) }) { Text("🗑") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBudgetScreen(vm: MainViewModel, showMsg: (String) -> Unit, onDone: () -> Unit) {
    val projects by vm.projects.collectAsState()
    val materials by vm.materials.collectAsState()
    val items by vm.draftItems.collectAsState()
    val selectedMaterial by vm.selectedMaterialForBudget.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var materialExpanded by remember { mutableStateOf(false) }
    var projectId by remember { mutableStateOf<Long?>(null) }
    var titulo by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var maoDeObra by remember { mutableStateOf("0") }
    var obs by remember { mutableStateOf("") }
    var itemDesc by remember { mutableStateOf("") }
    var qtd by remember { mutableStateOf("1") }
    var un by remember { mutableStateOf("un") }
    var vUnit by remember { mutableStateOf("0") }
    var selectedMaterialId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedMaterial?.id) {
        selectedMaterial?.let {
            selectedMaterialId = it.id
            itemDesc = it.nome
            un = it.unidade
            vUnit = it.valorUnitario.toString()
        }
    }

    val totalItens = items.sumOf { (it.quantidade.toDoubleOrNull() ?: 0.0) * (it.valorUnitario.toDoubleOrNull() ?: 0.0) }
    val total = totalItens + (maoDeObra.toDoubleOrNull() ?: 0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Novo Orçamento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = projects.firstOrNull { it.id == projectId }?.nome ?: "Selecione o projeto",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Projeto*") }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.nome) }, onClick = {
                            projectId = p.id
                            titulo = "Orçamento ${p.nome}"
                            cliente = p.cliente
                            local = p.local
                            expanded = false
                        })
                    }
                }
            }
            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título*") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cliente, { cliente = it }, label = { Text("Cliente*") }, modifier = Modifier.fillMaxWidth())
            LocationField(value = local, onValueChange = { local = it }, onMessage = showMsg)
            OutlinedTextField(
                maoDeObra,
                { maoDeObra = it },
                label = { Text("Mão de obra (R$)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(obs, { obs = it }, label = { Text("Observações") }, modifier = Modifier.fillMaxWidth())

            Text("Adicionar item", fontWeight = FontWeight.Bold)
            ExposedDropdownMenuBox(expanded = materialExpanded, onExpandedChange = { materialExpanded = it }) {
                OutlinedTextField(
                    value = materials.firstOrNull { it.id == selectedMaterialId }?.nome ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(materialExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Material vinculado") }
                )
                ExposedDropdownMenu(expanded = materialExpanded, onDismissRequest = { materialExpanded = false }) {
                    materials.forEach { m ->
                        DropdownMenuItem(text = { Text("${m.nome} (${vm.moeda(m.valorUnitario)}/${m.unidade})") }, onClick = {
                            selectedMaterialId = m.id
                            itemDesc = m.nome
                            un = m.unidade
                            vUnit = m.valorUnitario.toString()
                            materialExpanded = false
                        })
                    }
                }
            }
            OutlinedTextField(itemDesc, { itemDesc = it }, label = { Text("Descrição*") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(qtd, { qtd = it }, label = { Text("Qtd") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(un, { un = it }, label = { Text("Un") }, modifier = Modifier.weight(1f))
                OutlinedTextField(vUnit, { vUnit = it }, label = { Text("Unitário") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            BigButton("Adicionar Item") {
                if (itemDesc.isBlank()) return@BigButton
                vm.addDraftItem(DraftBudgetItem(itemDesc, qtd, un, vUnit.replace(',', '.')))
                itemDesc = ""; qtd = "1"; un = "un"; vUnit = "0"; selectedMaterialId = null
            }
        }

        items(items.indices.toList()) { i ->
            val it = items[i]
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(it.descricao, fontWeight = FontWeight.Bold)
                        val subtotal = (it.quantidade.toDoubleOrNull() ?: 0.0) * (it.valorUnitario.toDoubleOrNull() ?: 0.0)
                        Text("${it.quantidade} ${it.unidade} x ${vm.moeda(it.valorUnitario.toDoubleOrNull() ?: 0.0)} = ${vm.moeda(subtotal)}")
                    }
                    Text("Remover", modifier = Modifier.clickable { vm.removeDraftItem(i) })
                }
            }
        }

        item {
            Text("Total: ${vm.moeda(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            BigButton("Salvar orçamento") {
                if (projectId == null || titulo.isBlank() || cliente.isBlank() || local.isBlank() || items.isEmpty()) return@BigButton
                vm.saveBudget(
                    BudgetEntity(projectId = projectId!!, titulo = titulo, cliente = cliente, localObra = local, data = vm.hojeBr(), observacoes = obs, maoDeObra = maoDeObra.toDoubleOrNull() ?: 0.0)
                ) {
                    vm.clearDraft()
                    onDone()
                }
            }
        }
    }
}

@Composable
fun BudgetsScreen(vm: MainViewModel, onNovo: () -> Unit, onShare: (Uri) -> Unit, showMsg: (String) -> Unit) {
    val budgets by vm.filteredBudgets.collectAsState()
    val context = LocalContext.current
    var status by remember { mutableStateOf("Todos") }
    LaunchedEffect(status) { vm.budgetFilterStatus.value = status }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Orçamentos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Rascunho", "Enviado").forEach {
                Button(onClick = { status = it }, modifier = Modifier.weight(1f)) { Text(it) }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(budgets) { b ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(b.titulo, fontWeight = FontWeight.Bold)
                        Text("${b.cliente} • ${b.data} • ${b.status}")
                        Text("Total: ${vm.moeda(b.total)}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { vm.duplicateBudget(b) }) { Text("Duplicar") }
                            Button(onClick = { vm.deleteBudget(b) }) { Text("Excluir") }
                            Button(onClick = {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    val (budget, items) = vm.getBudgetWithItems(b.id)
                                    val proj = vm.getProject(b.projectId)
                                    if (budget != null) {
                                        val file = PdfGenerator.gerar(context, budget, proj, items)
                                        vm.markBudgetPdf(b.id, file.absolutePath)
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        onShare(uri)
                                        showMsg("PDF gerado com sucesso")
                                    }
                                }
                            }) { Text("Gerar PDF") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = onNovo, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.Add, "Novo") }
    }
}
