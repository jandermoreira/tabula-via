package edu.jm.tabulavia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.viewmodel.CourseViewModel

private enum class GroupUiState {
    LOADING,
    NO_GROUPS,
    SHOW_GROUPS,
    CONFIGURE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGroupScreen(
    activityId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit,
    onGroupClicked: (Int) -> Unit
) {
    val activity by viewModel.selectedActivity.collectAsState()
    val groups by viewModel.generatedGroups.collectAsState()
    val groupsLoaded by viewModel.groupsLoaded.collectAsState()
    val loadedActivityId by viewModel.loadedActivityId.collectAsState()

    var uiState by remember(activityId) { mutableStateOf(GroupUiState.LOADING) }

    LaunchedEffect(activityId) {
        viewModel.clearActivityState()
        viewModel.loadActivityDetails(activityId)
        uiState = GroupUiState.LOADING
    }

    LaunchedEffect(groupsLoaded) {
        if (groupsLoaded && loadedActivityId == activityId) {
            uiState = if (groups.isEmpty())
                GroupUiState.NO_GROUPS
            else
                GroupUiState.SHOW_GROUPS
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Montar Grupos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (uiState == GroupUiState.SHOW_GROUPS) {
                        IconButton(onClick = { uiState = GroupUiState.CONFIGURE }) {
                            Icon(Icons.Default.Refresh, null)
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (loadedActivityId != activityId) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aguarde…")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {

                GroupUiState.LOADING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aguarde…")
                    }
                }

                GroupUiState.NO_GROUPS -> {
                    ConfigurationView(
                        viewModel = viewModel,
                        onCancel = {},
                        onGroupsCreated = {
                            uiState = GroupUiState.SHOW_GROUPS
                        }
                    )
                }

                GroupUiState.CONFIGURE -> {
                    ConfigurationView(
                        viewModel = viewModel,
                        onCancel = {
                            uiState = if (groups.isEmpty())
                                GroupUiState.NO_GROUPS
                            else
                                GroupUiState.SHOW_GROUPS
                        },
                        onGroupsCreated = {
                            uiState = GroupUiState.SHOW_GROUPS
                        }
                    )
                }

                GroupUiState.SHOW_GROUPS -> {
                    GroupsView(groups, onGroupClicked)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationView(
    viewModel: CourseViewModel,
    onCancel: () -> Unit,
    onGroupsCreated: () -> Unit
) {
    val groupingCriteria = listOf("Aleatório", "Balanceado por habilidade", "Manual")
    val formationOptions = listOf("Número de grupos", "Alunos por grupo")

    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedFrom by remember { mutableStateOf<Location?>(null) }
    val isCriterionCompact = viewModel.groupingCriterion == "Manual"

    Column(modifier = Modifier.padding(16.dp)) {

        AnimatedVisibility(
            visible = !isCriterionCompact,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Text(
                    "Critério de Agrupamento",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            groupingCriteria.forEach { criterion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = viewModel.groupingCriterion == criterion,
                            onClick = {
                                viewModel.groupingCriterion = criterion
                            }
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.groupingCriterion == criterion) {
                        Icon(Icons.Default.Check, null)
                    } else {
                        Spacer(Modifier.width(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(criterion)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (viewModel.groupingCriterion) {

            "Aleatório" -> {

                Text("Opções", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    formationOptions.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = viewModel.groupFormationType == option,
                            onClick = { viewModel.groupFormationType = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = formationOptions.size
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(option, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = viewModel.groupFormationValue,
                    onValueChange = {
                        viewModel.groupFormationValue = it.filter(Char::isDigit)
                    },
                    label = { Text(viewModel.groupFormationType) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            viewModel.createBalancedGroups()
                            onGroupsCreated()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Criar Grupos")
                    }
                }
            }

            "Manual" -> {

                LaunchedEffect(Unit) {
                    viewModel.enterManualMode()
                }

                ManualGroupEditorView(
                    groups = viewModel.manualGroups,
                    unassignedStudents = viewModel.unassignedStudents,
                    onStudentSelected = { student, from ->
                        selectedStudent = student
                        selectedFrom = from
                    },
                    onDropTargetSelected = { target ->
                        val student = selectedStudent
                        val from = selectedFrom
                        if (student != null && from != null) {
                            viewModel.moveStudent(student, from, target)
                        }
                        selectedStudent = null
                        selectedFrom = null
                    }
                )
            }

            else -> {
                Text("Ainda não implementado")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupsView(
    groups: List<List<Student>>,
    onGroupClicked: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(groups) { index, group ->
            GroupCard(
                groupNumber = index + 1,
                studentCount = group.size
            ) {
                onGroupClicked(index + 1)
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupNumber: Int,
    studentCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Group, null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Grupo $groupNumber", fontWeight = FontWeight.Bold)
            Text(
                "$studentCount componentes",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ManualGroupEditorView(
    groups: List<Group>,
    unassignedStudents: List<Student>,
    onStudentSelected: (Student, Location) -> Unit,
    onDropTargetSelected: (DropTarget) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {

        Column(Modifier.weight(1f)) {
            Text("Não alocados")
            unassignedStudents.forEach { student ->
                Text(
                    student.name,
                    modifier = Modifier
                        .clickable {
                            onStudentSelected(student, Location.Unassigned)
                        }
                        .padding(8.dp)
                )
            }
        }

        Column(Modifier.weight(2f)) {

            OutlinedButton(
                onClick = { onDropTargetSelected(DropTarget.NewGroup) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Criar grupo")
            }

            groups.forEach { group ->
                Card(Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Grupo")

                        group.students.forEach { student ->
                            Text(
                                student.name,
                                modifier = Modifier
                                    .clickable {
                                        onStudentSelected(
                                            student,
                                            Location.Group(group.id)
                                        )
                                    }
                                    .padding(4.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onDropTargetSelected(
                                    DropTarget.ExistingGroup(group.id)
                                )
                            }
                        ) {
                            Text("Mover selecionado aqui")
                        }
                    }
                }
            }
        }
    }
}
