package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FinanceCategory

@Composable
fun CategoriesScreen(
    categories: List<FinanceCategory>,
    onAddCategory: (String, String) -> Unit,
    onEditCategory: (FinanceCategory) -> Unit,
    onDeleteCategory: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<FinanceCategory?>(null) }

    val earningCategories = categories.filter { it.type == "EARNING" }
    val costCategories = categories.filter { it.type == "COST" }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("categories_list")
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Tipos de Lançamento",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize suas fontes de corrida e tipos de gastos do veículo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ganhos
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00FF66).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Canais de Faturamento (Ganhos)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(earningCategories, key = { "earn_${it.id}" }) { category ->
                CategoryItemCard(
                    category = category,
                    onEditClick = { categoryToEdit = category },
                    onDeleteClick = { onDeleteCategory(category.id) }
                )
            }

            // Custos
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF007A).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = Color(0xFFFF007A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Categorias de Gasto (Custos)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(costCategories, key = { "cost_${it.id}" }) { category ->
                CategoryItemCard(
                    category = category,
                    onEditClick = { categoryToEdit = category },
                    onDeleteClick = { onDeleteCategory(category.id) }
                )
            }
        }

        // Botão FAB para Adicionar Nova Categoria
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_category_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Nova Categoria",
                modifier = Modifier.size(28.dp)
            )
        }

        // Dialog de Adicionar
        if (showAddDialog) {
            AddCategoryDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type ->
                    onAddCategory(name, type)
                    showAddDialog = false
                }
            )
        }

        // Dialog de Editar
        if (categoryToEdit != null) {
            EditCategoryDialog(
                category = categoryToEdit!!,
                onDismiss = { categoryToEdit = null },
                onConfirm = { updatedCategory ->
                    onEditCategory(updatedCategory)
                    categoryToEdit = null
                }
            )
        }
    }
}

@Composable
fun CategoryItemCard(
    category: FinanceCategory,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEarning = category.type == "EARNING"
    val accentColor = if (isEarning) Color(0xFF00FF66) else Color(0xFFFF007A)
    val cardTag = "category_item_${category.id}"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(cardTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Indicador de cor neon lateral
                Box(
                    modifier = Modifier
                        .size(depth = 24.dp, width = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEarning) "Canal de Ganho" else "Tipo de Custo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Editar
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("edit_category_btn_${category.id}"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Nome",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Deletar (somente se não for padrão de sistema de forma crítica, ou permitir deletar todos com aviso)
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("delete_category_btn_${category.id}"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Categoria",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun Modifier.size(depth: androidx.compose.ui.unit.Dp, width: androidx.compose.ui.unit.Dp): Modifier {
    return this.height(depth).width(width)
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EARNING") } // "EARNING" ou "COST"
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier.testTag("add_category_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Categoria",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showError) {
                    Text(
                        text = "Por favor, digite um nome válido para o tipo.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Tipo (Ex: Uber Jack)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                Column {
                    Text(
                        text = "Tipo de Classificação",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Radio Ganho
                        ElevatedCard(
                            onClick = { selectedType = "EARNING" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (selectedType == "EARNING") {
                                    Color(0xFF00FF66).copy(alpha = 0.2f)
                                } else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF00FF66))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Ganho", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        // Radio Custo
                        ElevatedCard(
                            onClick = { selectedType = "COST" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (selectedType == "COST") {
                                    Color(0xFFFF007A).copy(alpha = 0.2f)
                                } else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.TrendingDown, null, tint = Color(0xFFFF007A))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Custo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedType)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_add_category_btn")
            ) {
                Text("Salvar Categoria")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_add_category_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditCategoryDialog(
    category: FinanceCategory,
    onDismiss: () -> Unit,
    onConfirm: (FinanceCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(category.name) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = modifier.testTag("edit_category_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Tipo de Lançamento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showError) {
                    Text(
                        text = "Por favor, digite um nome válido.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Tipo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_edit_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(category.copy(name = name.trim()))
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_edit_category_btn")
            ) {
                Text("Salvar Alteração")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_edit_category_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}
