/**
 * ActivityGroupScreen.kt
 *
 * This file contains the UI components for managing student groups in an activity.
 * It supports both random and manual grouping, including a drag-and-drop editor.
 */

package edu.jm.tabulavia.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.viewmodel.CourseViewModel
import edu.jm.tabulavia.ui.StudentEmojiColorHelper.mapStudentIdToEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class GroupUiState {
    LOADING, NO_GROUPS, SHOW_GROUPS, CONFIGURE
}

private data class DraggedStudent(
    val student: Student, val from: Location
)

/**
 * Main screen for activity grouping.
 * Handles the high-level state of group generation and assessment dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityGroupScreen(
    activityId: Long,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val activity by viewModel.selectedActivity.collectAsState()
    val groups by viewModel.generatedGroups.collectAsState()
    val groupsLoaded by viewModel.groupsLoaded.collectAsState()
    val loadedActivityId by viewModel.loadedActivityId.collectAsState()

    var showAssignSkillsDialog by remember { mutableStateOf(false) }
    var selectedStudentForSkillAssignment by remember { mutableStateOf<Student?>(null) }
    var showAssignGroupSkillsDialog by remember { mutableStateOf(false) }
    var selectedGroupForSkillAssignment by remember { mutableStateOf<List<Student>?>(null) }

    var uiState by remember(activityId) { mutableStateOf(GroupUiState.LOADING) }

    // Load activity details when the ID changes
    LaunchedEffect(activityId) {
        viewModel.clearActivityState()
        viewModel.loadActivityDetails(activityId)
        uiState = GroupUiState.LOADING
    }

    // Sync UI state with ViewModel data
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
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
                        onCancel = {
                            if (uiState == GroupUiState.CONFIGURE) uiState =
                                GroupUiState.SHOW_GROUPS
                        },
                        onGroupsCreated = { uiState = GroupUiState.SHOW_GROUPS }
                    )
                }

                GroupUiState.SHOW_GROUPS -> {
                    GroupsExpandedView(
                        groups = groups,
                        onStudentClick = { student ->
                            selectedStudentForSkillAssignment = student
                            viewModel.loadStudentDetails(student.studentId)
                            showAssignSkillsDialog = true
                        },
                        onGroupActionClick = { groupStudents ->
                            selectedGroupForSkillAssignment = groupStudents
                            showAssignGroupSkillsDialog = true
                        }
                    )
                }
            }
        }
    }

    // Individual skill assessment dialog
    if (showAssignSkillsDialog) {
        selectedStudentForSkillAssignment?.let { student ->
            AssignGroupSkillsDialog(student, viewModel, activityId) {
                showAssignSkillsDialog = false
                selectedStudentForSkillAssignment = null
            }
        }
    }

    // Bulk skill assessment dialog for a whole group
    if (showAssignGroupSkillsDialog) {
        selectedGroupForSkillAssignment?.let { students ->
            AssignGroupSkillsForAllDialog(students, viewModel, activityId) {
                showAssignGroupSkillsDialog = false
                selectedGroupForSkillAssignment = null
            }
        }
    }
}

/**
 * Displays groups in a vertical list.
 * Uses FlowRow to adapt member lists to tablet and landscape screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupsExpandedView(
    groups: List<List<Student>>,
    onStudentClick: (Student) -> Unit,
    onGroupActionClick: (List<Student>) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(groups) { index, groupStudents ->
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Grupo ${index + 1}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${groupStudents.size} alunos",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { onGroupActionClick(groupStudents) }) {
                            Icon(Icons.Default.Psychology, "Avaliar Grupo")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupStudents.sortedBy { it.displayName }.forEach { student ->
                            StudentItem(
                                student = student,
                                emoji = mapStudentIdToEmoji(student.studentId),
//                                circleSize = 56.dp,
                                modifier = Modifier
                                    .width(90.dp)
                                    .clickable { onStudentClick(student) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive drag-and-drop editor for manual group formation.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val groupBounds = remember { mutableStateMapOf<Int, Rect>() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dropTargetBackground = primaryColor.copy(alpha = 0.05f)
    val dashColorInactive = Color.Gray.copy(alpha = 0.4f)

    // Logic to determine where the dragged student is dropped
    fun detectDropTarget(): DropTarget? {
        val container = containerCoords ?: return null
        val dragPos = dragPositionInContainer ?: return null
        val rootPos = container.localToRoot(dragPos)

        if (unassignedBounds?.contains(rootPos) == true) return DropTarget.Unassigned
        if (newGroupBounds?.contains(rootPos) == true) return DropTarget.NewGroup

        groupBounds.forEach { (id, rect) ->
            if (rect.contains(rootPos)) return DropTarget.ExistingGroup(id)
        }
        return null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerCoords = it }) {
        Row(modifier = Modifier.fillMaxSize()) {
            // UNASSIGNED STUDENTS COLUMN
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { unassignedBounds = it.boundsInRoot() }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "Não alocados",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        unassignedStudents.forEach { student ->
                            DraggableStudentWrapper(
                                student = student,
                                isDragging = draggedStudent?.student?.studentId == student.studentId,
                                onStart = { offset, coords ->
                                    draggedStudent = DraggedStudent(student, Location.Unassigned)
                                    dragPositionInContainer =
                                        containerCoords?.localPositionOf(coords, offset)
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
            }

            // ASSIGNED GROUPS COLUMN
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) {
                // New group drop target area (Dashed Box)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(vertical = 8.dp)
                        .onGloballyPositioned { newGroupBounds = it.boundsInRoot() }
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .drawBehind {
                            drawRoundRect(
                                // Use local variable instead of MaterialTheme
                                color = if (draggedStudent != null) primaryColor else dashColorInactive,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(10f, 10f),
                                        0f
                                    )
                                ),
                                cornerRadius = CornerRadius(12.dp.toPx())
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Arraste para novo grupo", style = MaterialTheme.typography.bodySmall)
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { groupBounds[group.id] = it.boundsInRoot() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Grupo ${group.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth()) {
                                    group.students.forEach { student ->
                                        DraggableStudentWrapper(
                                            student = student,
                                            isDragging = draggedStudent?.student?.studentId == student.studentId,
                                            onStart = { offset, coords ->
                                                draggedStudent = DraggedStudent(
                                                    student,
                                                    Location.Group(group.id)
                                                )
                                                dragPositionInContainer =
                                                    containerCoords?.localPositionOf(coords, offset)
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

        // FLOATING DRAG GHOST
        if (draggedStudent != null && dragPositionInContainer != null) {
            val currentPos = dragPositionInContainer!!
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            currentPos.x.toInt() - 40.dp.toPx().toInt(),
                            currentPos.y.toInt() - 40.dp.toPx().toInt()
                        )
                    }
                    .size(80.dp)
            ) {
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    StudentItem(
                        student = draggedStudent!!.student,
                        emoji = mapStudentIdToEmoji(draggedStudent!!.student.studentId),
//                        circleSize = 48.dp
                    )
                }
            }
        }
    }
}

/**
 * Wrapper component to handle drag gestures for a single student.
 */
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
            .width(80.dp)
            .onGloballyPositioned { itemCoords = it }
            .alpha(if (isDragging) 0f else 1f)
            .pointerInput(student) {
                detectDragGestures(
                    onDragStart = { offset -> itemCoords?.let { onStart(offset, it) } },
                    onDrag = { change, dragAmount -> change.consume(); onMove(dragAmount) },
                    onDragEnd = onEnd,
                    onDragCancel = onEnd
                )
            }
    ) {
        StudentItem(
            student = student,
            emoji = mapStudentIdToEmoji(student.studentId),
//            circleSize = 48.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ConfigurationView(
    viewModel: CourseViewModel,
    onCancel: () -> Unit,
    onGroupsCreated: () -> Unit
) {
    val groupingCriteria = listOf("Aleatório", "Manual")
    val formationOptions = listOf("Número de grupos", "Alunos por grupo")

    // Identificação da orientação para ajuste de espaço
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(modifier = Modifier.padding(16.dp)) {
        // Título opcional ou reduzido em landscape
        if (!isLandscape) {
            Text("Configuração de Grupos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            // Em landscape, colocamos as opções lado a lado para ganhar altura
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                groupingCriteria.forEach { c ->
                    Row(
                        modifier = Modifier
                            // Em landscape, cada opção ocupa metade da largura
                            .then(if (isLandscape) Modifier.weight(1f) else Modifier.fillMaxWidth())
                            .selectable(
                                selected = viewModel.groupingCriterion == c,
                                onClick = { viewModel.groupingCriterion = c }
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Start
                    ) {
                        // Mantendo apenas o seu texto original, sem RadioButtons ou ícones
                        Text(
                            text = c,
                            color = if (viewModel.groupingCriterion == c)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (viewModel.groupingCriterion == c)
                                FontWeight.Bold
                            else
                                FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(if (isLandscape) 8.dp else 16.dp))

        if (viewModel.groupingCriterion == "Aleatório") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                formationOptions.forEachIndexed { i, o ->
                    SegmentedButton(
                        selected = viewModel.groupFormationType == o,
                        onClick = { viewModel.groupFormationType = o },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i, count = formationOptions.size
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text(o, textAlign = TextAlign.Center) }
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

            Spacer(Modifier.height(if (isLandscape) 8.dp else 16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }

                Button(
                    onClick = {
                        viewModel.createBalancedGroups()
                        onGroupsCreated()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Criar Grupos") }
            }
        } else {
            LaunchedEffect(Unit) { viewModel.enterManualMode() }
            ManualGroupEditorView(
                groups = viewModel.manualGroups,
                unassignedStudents = viewModel.unassignedStudents,
                onMoveStudent = { s, from, to -> viewModel.moveStudent(s, from, to) }
            )
        }
    }
}

/**
 * Dialog to apply skill assessments to multiple students at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignGroupSkillsForAllDialog(
    students: List<Student>,
    viewModel: CourseViewModel,
    activityId: Long?,
    onDismiss: () -> Unit
) {
    val courseSkills by viewModel.courseSkills.collectAsState()
    val context = LocalContext.current
    var highlightedSkillNames by remember(activityId) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(activityId) {
        if (activityId == null || activityId == 0L) return@LaunchedEffect
        val db = DatabaseProvider.getDatabase(context.applicationContext)
        highlightedSkillNames = withContext(Dispatchers.IO) {
            db.activityHighlightedSkillDao().getHighlightedSkillNamesForActivity(activityId).toSet()
        }
    }

    var skillLevels by remember(courseSkills) {
        mutableStateOf(courseSkills.associate { it.skillName to SkillLevel.NOT_APPLICABLE })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habilidades para o Grupo") },
        text = {
            Column {
                Text(
                    "Aplicar a todos os ${students.size} alunos:",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(courseSkills.sortedBy { it.skillName }) { skill ->
                        SkillAssignmentRow(
                            skillName = skill.skillName,
                            currentLevel = skillLevels[skill.skillName]
                                ?: SkillLevel.NOT_APPLICABLE,
                            onLevelSelected = { new ->
                                skillLevels = skillLevels + (skill.skillName to new)
                            },
                            isHighlighted = highlightedSkillNames.contains(skill.skillName)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val assessments = skillLevels.filter { it.value != SkillLevel.NOT_APPLICABLE }
                students.forEach { student ->
                    assessments.forEach { (skillName, level) ->
                        viewModel.addSkillAssessment(
                            student.studentId,
                            skillName,
                            level,
                            AssessmentSource.PROFESSOR_OBSERVATION
                        )
                    }
                }
                onDismiss()
            }) { Text("Salvar Grupo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Dialog to assess skills for a specific student within the grouping screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignGroupSkillsDialog(
    student: Student,
    viewModel: CourseViewModel,
    activityId: Long?,
    onDismiss: () -> Unit
) {
    val courseSkills by viewModel.courseSkills.collectAsState()
    val context = LocalContext.current
    var highlightedSkillNames by remember(activityId) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(activityId) {
        if (activityId == null || activityId == 0L) return@LaunchedEffect
        val db = DatabaseProvider.getDatabase(context.applicationContext)
        highlightedSkillNames = withContext(Dispatchers.IO) {
            db.activityHighlightedSkillDao().getHighlightedSkillNamesForActivity(activityId).toSet()
        }
    }

    var skillLevels by remember(courseSkills) {
        mutableStateOf(courseSkills.associate { it.skillName to SkillLevel.NOT_APPLICABLE })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Avaliar ${student.displayName.split(" ").first()}") },
        text = {
            Column {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(courseSkills.sortedBy { it.skillName }) { skill ->
                        SkillAssignmentRow(
                            skillName = skill.skillName,
                            currentLevel = skillLevels[skill.skillName]
                                ?: SkillLevel.NOT_APPLICABLE,
                            onLevelSelected = { new ->
                                skillLevels = skillLevels + (skill.skillName to new)
                            },
                            isHighlighted = highlightedSkillNames.contains(skill.skillName)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                skillLevels.filter { it.value != SkillLevel.NOT_APPLICABLE }
                    .forEach { (skillName, level) ->
                        viewModel.addSkillAssessment(
                            student.studentId,
                            skillName,
                            level,
                            AssessmentSource.PROFESSOR_OBSERVATION
                        )
                    }
                onDismiss()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * A row component for a single skill assessment, displaying options (Low, Medium, High).
 */
@Composable
private fun SkillAssignmentRow(
    skillName: String,
    currentLevel: SkillLevel,
    onLevelSelected: (SkillLevel) -> Unit,
    isHighlighted: Boolean
) {
    val background =
        if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (isHighlighted) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(skillName, style = MaterialTheme.typography.bodyMedium)
        }

        @OptIn(ExperimentalMaterial3Api::class)
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row {
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.NOT_APPLICABLE) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Clear, null, modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.NOT_APPLICABLE) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.LOW) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbDown, null, modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.LOW) MaterialTheme.colorScheme.error else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.MEDIUM) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Circle, null, modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.MEDIUM) Color(0xFFFFA500) else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.HIGH) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbUp, null, modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.HIGH) Color(0xFF008000) else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
            }
        }
    }
}