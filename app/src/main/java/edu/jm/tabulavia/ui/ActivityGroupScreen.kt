package edu.jm.tabulavia.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGroupScreen(
    activityId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit,
    onGroupClicked: (Int) -> Unit
) {
    val activity by viewModel.selectedActivity.collectAsState()
    val generatedGroups by viewModel.generatedGroups.collectAsState()
    val groupingCriteria = listOf("Aleatório", "Manual", "Balanceado por habilidade")
    val groupFormationOptions = listOf("Número de grupos", "Alunos por grupo")

    LaunchedEffect(activityId) {
        viewModel.loadActivityDetails(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Montar Grupos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (generatedGroups.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearGroups() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refazer Grupos")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (generatedGroups.isEmpty()) {
                // --- Configuration Mode ---
                ConfigurationView(viewModel, groupingCriteria, groupFormationOptions)
            } else {
                // --- Display Mode ---
                GroupsView(generatedGroups, onGroupClicked)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationView(
    viewModel: CourseViewModel,
    groupingCriteria: List<String>,
    groupFormationOptions: List<String>
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // --- Grouping Criterion ---
        Text("Critério de Agrupamento", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column {
                groupingCriteria.forEach { criterion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (criterion == viewModel.groupingCriterion),
                                onClick = { viewModel.groupingCriterion = criterion }
                            )
                            .background(
                                if (criterion == viewModel.groupingCriterion) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconSize = 24.dp
                        if (criterion == viewModel.groupingCriterion) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selecionado",
                                modifier = Modifier.size(iconSize),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Spacer(modifier = Modifier.width(iconSize))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(criterion)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // --- Conditional Options ---
        when (viewModel.groupingCriterion) {
            "Aleatório" -> {
                RandomGroupOptions(viewModel, groupFormationOptions)
            }
            "Manual" -> {
                // TODO: Implementar UI para agrupamento manual
                Text("A funcionalidade de agrupamento manual será implementada em breve.")
            }
            "Balanceado por habilidade" -> {
                // TODO: Implementar UI para agrupamento por habilidade
                Text("A funcionalidade de agrupamento por habilidade será implementada em breve.")
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RandomGroupOptions(viewModel: CourseViewModel, groupFormationOptions: List<String>) {
    Column {
        Text("Opções de Formação", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            groupFormationOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = groupFormationOptions.size),
                    onClick = { viewModel.groupFormationType = option },
                    selected = option == viewModel.groupFormationType
                ) {
                    Text(option)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = viewModel.groupFormationValue,
            onValueChange = { viewModel.groupFormationValue = it.filter { char -> char.isDigit() } },
            label = { Text(viewModel.groupFormationType) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { viewModel.createBalancedGroups() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Criar Grupos")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupsView(groups: List<List<Student>>, onGroupClicked: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(groups, key = { index, _ -> index }) { index, group ->
            val groupNumber = index + 1
            GroupCard(groupNumber = groupNumber, studentCount = group.size) {
                onGroupClicked(groupNumber)
            }
        }
    }
}

@Composable
fun GroupCard(groupNumber: Int, studentCount: Int, onClick: () -> Unit) {
    GridItemCard(modifier = Modifier.clickable(onClick = onClick)) {
        Icon(
            imageVector = Icons.Default.Group,
            contentDescription = "Ícone de Grupo",
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Grupo $groupNumber",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$studentCount componentes",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
