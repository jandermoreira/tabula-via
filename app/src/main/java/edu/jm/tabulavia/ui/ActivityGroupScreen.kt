package edu.jm.tabulavia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
            TopAppBar(title = { Text(activity?.title ?: "Montar Grupos") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }, actions = {
                if (uiState == GroupUiState.SHOW_GROUPS) {
                    IconButton(onClick = { uiState = GroupUiState.CONFIGURE }) {
                        Icon(Icons.Default.Edit, null)
                    }
                }
            })
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
                        onGroupsCreated = { uiState = GroupUiState.SHOW_GROUPS })
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
    val groupingCriteria = listOf("Aleatório", "Balanceado por habilidade", "Manual")
    val formationOptions = listOf("Número de grupos", "Alunos por grupo")
    var draggedStudent by remember { mutableStateOf<DraggedStudent?>(null) }
    var activeDropTarget by remember { mutableStateOf<DropTarget?>(null) }

    val isCriterionCompact = viewModel.groupingCriterion == "Manual"

    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        AnimatedVisibility(
            visible = !isCriterionCompact, enter = expandVertically(), exit = shrinkVertically()
        ) {
            Column {
                Text(
                    "Critério de Agrupamento", style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (viewModel.groupingCriterion == "Manual") {

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = viewModel.groupingCriterion,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Critério") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false }) {
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
                                index = index, count = formationOptions.size
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
                        onClick = onCancel, modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            viewModel.createBalancedGroups()
                            onGroupsCreated()
                        }, modifier = Modifier.weight(1f)
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
                    onDragStart = { draggedStudent = it },
                    onDragEnd = { dropTarget ->
                        if (draggedStudent != null && dropTarget != null) {
                            viewModel.moveStudent(
                                draggedStudent!!.student, draggedStudent!!.from, dropTarget
                            )
                        }
                        draggedStudent = null
                    })

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
    groups: List<List<Student>>, onGroupClicked: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(groups) { index, group ->
            GroupCard(
                groupNumber = index + 1, studentCount = group.size
            ) {
                onGroupClicked(index + 1)
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupNumber: Int, studentCount: Int, onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Group, null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("Grupo $groupNumber", fontWeight = FontWeight.Bold)
            Text(
                "$studentCount componentes", style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ManualGroupEditorView(
    groups: List<Group>,
    unassignedStudents: List<Student>,
    onDragStart: (DraggedStudent) -> Unit,
    onDragEnd: (DropTarget?) -> Unit
) {
    // Estado da posição (Coordenadas da tela/Root)
    var dragPositionRoot by remember { mutableStateOf<Offset?>(null) }

    // Estado do item sendo arrastado (para desenhar o "fantasma")
    var draggedItem by remember { mutableStateOf<Student?>(null) }

    // Bounds das áreas de soltura
    var unassignedBounds by remember { mutableStateOf<Rect?>(null) }
    var newGroupBounds by remember { mutableStateOf<Rect?>(null) }
    val groupBounds = remember { mutableStateMapOf<Long, Rect>() }

    fun detectDropTarget(): DropTarget? {
        val pos = dragPositionRoot ?: return null
        if (unassignedBounds?.contains(pos) == true) return DropTarget.Unassigned
        if (newGroupBounds?.contains(pos) == true) return DropTarget.NewGroup
        groupBounds.forEach { (id, rect) ->
            if (rect.contains(pos)) return DropTarget.ExistingGroup(id.toInt())
        }
        return null
    }

    // 1. Envolvemos tudo em um Box para permitir camadas (z-index)
    Box(modifier = Modifier.fillMaxSize()) {

        // Camada de Conteúdo (Listas)
        Row(
            Modifier
                .fillMaxSize()
        ) {
            // --- COLUNA: NÃO ALOCADOS ---
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .onGloballyPositioned { unassignedBounds = it.boundsInRoot() }
            ) {
                Text("Não alocados", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))

                unassignedStudents.forEach { student ->
                    DraggableStudentItem(
                        student = student,
                        isBeingDragged = (draggedItem == student), // Para ocultar o original
                        onDragStart = { startPos ->
                            dragPositionRoot = startPos
                            draggedItem = student // Define quem estamos arrastando visualmente
                            onDragStart(DraggedStudent(student, Location.Unassigned)) // Lógica de negócio
                        },
                        onDrag = { dragAmount ->
                            dragPositionRoot = dragPositionRoot?.plus(dragAmount)
                        },
                        onDragEnd = {
                            onDragEnd(detectDropTarget())
                            dragPositionRoot = null
                            draggedItem = null // Limpa o visual
                        }
                    )
                }
            }

            // --- COLUNA: GRUPOS ---
            Column(
                Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .onGloballyPositioned { newGroupBounds = it.boundsInRoot() }
                ) {
                    Text("+ Novo grupo")
                }

                groups.forEach { group ->
                    Card(
                        Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .onGloballyPositioned { groupBounds[group.id.toLong()] = it.boundsInRoot() }
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("Grupo ${group.id}", fontWeight = FontWeight.Bold)

                            group.students.forEach { student ->
                                DraggableStudentItem(
                                    student = student,
                                    isBeingDragged = (draggedItem == student),
                                    onDragStart = { startPos ->
                                        dragPositionRoot = startPos
                                        draggedItem = student
                                        onDragStart(DraggedStudent(student, Location.Group(group.id)))
                                    },
                                    onDrag = { dragAmount ->
                                        dragPositionRoot = dragPositionRoot?.plus(dragAmount)
                                    },
                                    onDragEnd = {
                                        onDragEnd(detectDropTarget())
                                        dragPositionRoot = null
                                        draggedItem = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. O "Fantasma" (Renderizado por cima de tudo)
        if (dragPositionRoot != null && draggedItem != null) {
            FloatingDragItem(
                text = draggedItem!!.name,
                offset = dragPositionRoot!!
            )
        }
    }
}

@Composable
private fun FloatingDragItem(text: String, offset: Offset) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .sizeIn(minWidth = 150.dp) // Garante que o card tenha um tamanho bom para ver
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    x = offset.x.toInt() - 100, // Centraliza um pouco mais horizontalmente
                    y = offset.y.toInt() - 120  // Posiciona acima do dedo para visibilidade
                )
            }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DraggableStudentItem(
    student: Student,
    isBeingDragged: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var itemCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    Card(
        modifier = Modifier
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .fillMaxWidth()
            .alpha(if (isBeingDragged) 0.0f else 1f), // Esconde o original enquanto arrasta
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // O texto não tem pointerInput, logo não bloqueia o scroll da Column
            Text(
                text = student.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )

            // O ícone de alça é o único que inicia o Drag
            val context = LocalContext.current
            val iconIndex = (student.studentId.mod(80L) + 1).toInt()
            val iconName = "student_${iconIndex}"
            val drawableResId =
                context.resources.getIdentifier(iconName, "drawable", context.packageName)
            student.studentId to (drawableResId.takeIf { it != 0 } ?: R.drawable.student_0)
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.DragHandle, // Ou DragHandle se disponível
                contentDescription = "Arrastar",
                modifier = Modifier
                    .size(24.dp)
                    .onGloballyPositioned { itemCoordinates = it }
                    .pointerInput(student) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Convertemos a posição local do ícone para a posição global da tela
                                val rootOffset = itemCoordinates?.localToRoot(offset)
                                if (rootOffset != null) {
                                    onDragStart(rootOffset)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}