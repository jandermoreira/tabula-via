package edu.jm.tabulavia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.R
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.viewmodel.CourseViewModel

private enum class GroupUiState {
    LOADING, NO_GROUPS, SHOW_GROUPS, CONFIGURE
}

private data class DraggedStudent(
    val student: Student, val from: Location
)

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
            uiState = if (groups.isEmpty()) GroupUiState.NO_GROUPS else GroupUiState.SHOW_GROUPS
            viewModel.groupingCriterion = if (groups.isEmpty()) "Aleatório" else "Manual"
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
                            Icon(Icons.Default.Edit, null)
                        }
                    }
                }
            )
        }) { padding ->

        if (loadedActivityId != activityId) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                GroupUiState.LOADING -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                GroupUiState.NO_GROUPS, GroupUiState.CONFIGURE -> {
                    ConfigurationView(
                        viewModel = viewModel,
                        onCancel = {},
                        onGroupsCreated = { uiState = GroupUiState.SHOW_GROUPS }
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
    viewModel: CourseViewModel, onCancel: () -> Unit, onGroupsCreated: () -> Unit
) {
    val groupingCriteria = listOf("Aleatório", "Manual")
    val formationOptions = listOf("Número de grupos", "Alunos por grupo")
    val isCriterionCompact = viewModel.groupingCriterion == "Manual"

    Column(modifier = Modifier.padding(16.dp)) {
        AnimatedVisibility(
            visible = !isCriterionCompact,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Text("Critério de Agrupamento", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (viewModel.groupingCriterion == "Manual") {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = viewModel.groupingCriterion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Critério") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    groupingCriteria.forEach { criterion ->
                        DropdownMenuItem(text = { Text(criterion) }, onClick = {
                            viewModel.groupingCriterion = criterion
                            expanded = false
                        })
                    }
                }
            }
        } else {
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
                                onClick = { viewModel.groupingCriterion = criterion })
                            .padding(12.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.groupingCriterion == criterion) Icon(
                            Icons.Default.Check,
                            null
                        )
                        else Spacer(Modifier.width(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(criterion)
                    }
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
                        ) { Text(option, textAlign = TextAlign.Center) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.groupFormationValue,
                    onValueChange = { viewModel.groupFormationValue = it.filter(Char::isDigit) },
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
                    ) { Text("Cancelar") }
                    Button(
                        onClick = { viewModel.createBalancedGroups(); onGroupsCreated() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Criar Grupos") }
                }
            }

            "Manual" -> {
                LaunchedEffect(Unit) { viewModel.enterManualMode() }
                ManualGroupEditorView(
                    groups = viewModel.manualGroups,
                    unassignedStudents = viewModel.unassignedStudents,
                    onMoveStudent = { s, from, to -> viewModel.moveStudent(s, from, to) }
                )
            }
        }
    }
}

@Composable
private fun ManualGroupEditorView(
    groups: List<Group>,
    unassignedStudents: List<Student>,
    onMoveStudent: (Student, Location, DropTarget) -> Unit
) {
    var draggedStudent by remember { mutableStateOf<DraggedStudent?>(null) }
    var dragPositionInContainer by remember { mutableStateOf<Offset?>(null) }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var unassignedBounds by remember { mutableStateOf<Rect?>(null) }
    var newGroupBounds by remember { mutableStateOf<Rect?>(null) }
    val groupBounds = remember { mutableStateMapOf<Long, Rect>() }

    fun detectDropTarget(): DropTarget? {
        val container = containerCoords ?: return null
        val dragPos = dragPositionInContainer ?: return null
        // Converter a posição relativa do container de volta para Root para bater com os Bounds capturados
        val rootPos = container.localToRoot(dragPos)

        if (unassignedBounds?.contains(rootPos) == true) return DropTarget.Unassigned
        if (newGroupBounds?.contains(rootPos) == true) return DropTarget.NewGroup
        groupBounds.forEach { (id, rect) ->
            if (rect.contains(rootPos)) return DropTarget.ExistingGroup(id.toInt())
        }
        return null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerCoords = it }) {
        Row(modifier = Modifier.fillMaxSize()) {
            // COLUNA: NÃO ALOCADOS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { unassignedBounds = it.boundsInRoot() }) {
                Text(
                    "Não alocados",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    unassignedStudents.sortedBy { it.displayName }.forEach { student ->
                        DraggableStudentWrapper(
                            student = student,
                            isDragging = draggedStudent?.student == student,
                            onStart = { localOffset, itemCoords ->
                                draggedStudent = DraggedStudent(student, Location.Unassigned)
                                dragPositionInContainer =
                                    containerCoords?.localPositionOf(itemCoords, localOffset)
                            },
                            onMove = { delta ->
                                dragPositionInContainer = dragPositionInContainer?.plus(delta)
                            },
                            onEnd = {
                                val target = detectDropTarget()
                                if (target != null && draggedStudent != null) {
                                    onMoveStudent(
                                        draggedStudent!!.student,
                                        draggedStudent!!.from,
                                        target
                                    )
                                }
                                draggedStudent = null
                                dragPositionInContainer = null
                            }
                        )
                    }
                }
            }

            // COLUNA: GRUPOS
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .onGloballyPositioned { newGroupBounds = it.boundsInRoot() }
                ) { Text("+ Novo grupo") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    groups.forEach { group ->
                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                                .onGloballyPositioned {
                                    groupBounds[group.id.toLong()] = it.boundsInRoot()
                                }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Grupo ${group.id}", fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), maxItemsInEachRow = 3) {
                                    group.students.sortedBy { it.displayName }.forEach { student ->
                                        DraggableStudentWrapper(
                                            student = student,
                                            isDragging = draggedStudent?.student == student,
                                            onStart = { localOffset, itemCoords ->
                                                draggedStudent = DraggedStudent(
                                                    student,
                                                    Location.Group(group.id)
                                                )
                                                dragPositionInContainer =
                                                    containerCoords?.localPositionOf(
                                                        itemCoords,
                                                        localOffset
                                                    )
                                            },
                                            onMove = { delta ->
                                                dragPositionInContainer =
                                                    dragPositionInContainer?.plus(delta)
                                            },
                                            onEnd = {
                                                val target = detectDropTarget()
                                                if (target != null && draggedStudent != null) {
                                                    onMoveStudent(
                                                        draggedStudent!!.student,
                                                        draggedStudent!!.from,
                                                        target
                                                    )
                                                }
                                                draggedStudent = null
                                                dragPositionInContainer = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // OVERLAY DO ITEM ARRASTADO (FANTASMA)
        if (draggedStudent != null && dragPositionInContainer != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = dragPositionInContainer!!.x.toInt() - 120,
                            y = dragPositionInContainer!!.y.toInt() - 120
                        )
                    }
                    .size(90.dp)
            ) {
                Surface(
                    tonalElevation = 16.dp,
                    shadowElevation = 16.dp,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    StudentVisualContent(student = draggedStudent!!.student)
                }
            }
        }
    }
}

@Composable
private fun DraggableStudentWrapper(
    student: Student,
    isDragging: Boolean,
    onStart: (Offset, LayoutCoordinates) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit
) {
    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .padding(4.dp)
            .width(85.dp)
            .onGloballyPositioned { itemCoords = it }
            .alpha(if (isDragging) 0f else 1f)
            .pointerInput(student) {
                detectDragGestures(
                    onDragStart = { offset -> itemCoords?.let { onStart(offset, it) } },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(dragAmount)
                    },
                    onDragEnd = onEnd,
                    onDragCancel = onEnd
                )
            }
    ) {
        StudentVisualContent(student = student)
    }
}

@Composable
private fun StudentVisualContent(student: Student) {
    val context = LocalContext.current
    val iconIndex = (student.studentId.mod(80L) + 1).toInt()
    val iconName = "student_${iconIndex}"
    val drawableResId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
    val finalResId = if (drawableResId != 0) drawableResId else R.drawable.student_0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = finalResId),
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            tint = Color.Unspecified
        )
        Text(
            text = student.displayName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 3,
            softWrap = true
        )
    }
}

@Composable
private fun GroupsView(groups: List<List<Student>>, onGroupClicked: (Int) -> Unit) {
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
            ) { onGroupClicked(index + 1) }
        }
    }
}

@Composable
private fun GroupCard(groupNumber: Int, studentCount: Int, onClick: () -> Unit) {
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
            Text("$studentCount componentes", style = MaterialTheme.typography.bodySmall)
        }
    }
}