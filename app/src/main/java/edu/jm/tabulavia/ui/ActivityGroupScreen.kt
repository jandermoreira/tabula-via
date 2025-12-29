package edu.jm.tabulavia.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.R
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    onGroupClicked: (Int) -> Unit // Mantido para compatibilidade, mas não usado na nova lógica visual
) {
    val activity by viewModel.selectedActivity.collectAsState()
    val groups by viewModel.generatedGroups.collectAsState()
    val groupsLoaded by viewModel.groupsLoaded.collectAsState()
    val loadedActivityId by viewModel.loadedActivityId.collectAsState()

    // --- Estados para os Diálogos de Habilidade ---
    var showAssignSkillsDialog by remember { mutableStateOf(false) }
    var selectedStudentForSkillAssignment by remember { mutableStateOf<Student?>(null) }

    var showAssignGroupSkillsDialog by remember { mutableStateOf(false) }
    var selectedGroupForSkillAssignment by remember { mutableStateOf<List<Student>?>(null) }
    // ----------------------------------------------

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

    // --- DIÁLOGOS ---

    if (showAssignSkillsDialog) {
        selectedStudentForSkillAssignment?.let { student ->
            AssignGroupSkillsDialog(
                student = student,
                viewModel = viewModel,
                activityId = activityId,
                onDismiss = {
                    showAssignSkillsDialog = false
                    selectedStudentForSkillAssignment = null
                }
            )
        }
    }

    if (showAssignGroupSkillsDialog) {
        selectedGroupForSkillAssignment?.let { groupStudents ->
            AssignGroupSkillsForAllDialog(
                students = groupStudents,
                viewModel = viewModel,
                activityId = activityId,
                onDismiss = {
                    showAssignGroupSkillsDialog = false
                    selectedGroupForSkillAssignment = null
                }
            )
        }
    }
}

/**
 * Visualização expandida onde os grupos e seus membros aparecem diretamente.
 * Substitui o antigo GroupsView com cards pequenos.
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
                    // Cabeçalho do Grupo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Grupo ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${groupStudents.size} alunos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Botão para avaliar o grupo inteiro
                        IconButton(
                            onClick = { onGroupActionClick(groupStudents) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "Avaliar Grupo"
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Lista de alunos em FlowRow (quebra de linha automática)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.Top,
                        maxItemsInEachRow = Int.MAX_VALUE
                    ) {
                        groupStudents.sortedBy { it.displayName }.forEach { student ->
                            Box(
                                modifier = Modifier
                                    .width(90.dp) // Largura fixa para alinhar na grade visual
                                    .clickable { onStudentClick(student) }
                                    .padding(4.dp)
                            ) {
                                StudentVisualContent(student = student)
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- COMPONENTES COPIADOS/ADAPTADOS DE GROUP DETAILS SCREEN ---

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
        if (activityId == null || activityId == 0L) {
            highlightedSkillNames = emptySet()
            return@LaunchedEffect
        }
        val appContext = context.applicationContext
        val db = DatabaseProvider.getDatabase(appContext)
        val dao = db.activityHighlightedSkillDao()
        highlightedSkillNames = withContext(Dispatchers.IO) {
            dao.getHighlightedSkillNamesForActivity(activityId).toSet()
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
                    text = "Aplicar a todos os ${students.size} alunos:",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (courseSkills.isEmpty()) {
                    Text("Nenhuma habilidade definida para esta turma.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(
                            courseSkills.sortedBy { it.skillName },
                            key = { it.skillName }
                        ) { courseSkill ->
                            val isHighlighted = highlightedSkillNames.contains(courseSkill.skillName)

                            SkillAssignmentRow(
                                skillName = courseSkill.skillName,
                                currentLevel = skillLevels[courseSkill.skillName]
                                    ?: SkillLevel.NOT_APPLICABLE,
                                onLevelSelected = { newLevel ->
                                    skillLevels = skillLevels.toMutableMap().apply {
                                        this[courseSkill.skillName] = newLevel
                                    }
                                },
                                isHighlighted = isHighlighted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = students.isNotEmpty(),
                onClick = {
                    val assessmentsToSave = skillLevels.entries
                        .filter { it.value != SkillLevel.NOT_APPLICABLE }
                        .map { it.key to it.value }

                    if (assessmentsToSave.isNotEmpty()) {
                        students.forEach { student ->
                            assessmentsToSave.forEach { (skill, level) ->
                                viewModel.addSkillAssessment(
                                    studentId = student.studentId,
                                    skillName = skill,
                                    level = level,
                                    source = AssessmentSource.PROFESSOR_OBSERVATION
                                )
                            }
                        }
                    }
                    onDismiss()
                }
            ) { Text("Salvar Grupo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

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
        if (activityId == null || activityId == 0L) {
            highlightedSkillNames = emptySet()
            return@LaunchedEffect
        }
        val appContext = context.applicationContext
        val db = DatabaseProvider.getDatabase(appContext)
        val dao = db.activityHighlightedSkillDao()
        highlightedSkillNames = withContext(Dispatchers.IO) {
            dao.getHighlightedSkillNamesForActivity(activityId).toSet()
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
                if(highlightedSkillNames.isNotEmpty()) {
                    Text(
                        text = "Destaques da atividade: ${highlightedSkillNames.size}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (courseSkills.isEmpty()) {
                    Text("Nenhuma habilidade definida para esta turma.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(
                            courseSkills.sortedBy { it.skillName },
                            key = { it.skillName }) { courseSkill ->
                            val isHighlighted =
                                highlightedSkillNames.contains(courseSkill.skillName)

                            SkillAssignmentRow(
                                skillName = courseSkill.skillName,
                                currentLevel = skillLevels[courseSkill.skillName]
                                    ?: SkillLevel.NOT_APPLICABLE,
                                onLevelSelected = { newLevel ->
                                    skillLevels = skillLevels.toMutableMap().apply {
                                        this[courseSkill.skillName] = newLevel
                                    }
                                },
                                isHighlighted = isHighlighted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val assessmentsToSave = skillLevels.entries
                        .filter { it.value != SkillLevel.NOT_APPLICABLE }
                        .map { it.key to it.value }

                    assessmentsToSave.forEach { (skill, level) ->
                        viewModel.addSkillAssessment(
                            studentId = student.studentId,
                            skillName = skill,
                            level = level,
                            source = AssessmentSource.PROFESSOR_OBSERVATION
                        )
                    }
                    onDismiss()
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SkillAssignmentRow(
    skillName: String,
    currentLevel: SkillLevel,
    onLevelSelected: (SkillLevel) -> Unit,
    isHighlighted: Boolean
) {
    val rowBackground =
        if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    val textColor =
        if (isHighlighted) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp), // Padding levemente reduzido
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isHighlighted) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Habilidade em destaque",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = skillName,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val buttonSize = 28.dp // Reduzido ligeiramente
                val iconSize = 16.dp

                IconButton(
                    onClick = { onLevelSelected(SkillLevel.NOT_APPLICABLE) },
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = SkillLevel.NOT_APPLICABLE.displayName,
                        tint = if (currentLevel == SkillLevel.NOT_APPLICABLE) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(if (currentLevel == SkillLevel.NOT_APPLICABLE) 1f else 0.7f)
                    )
                }

                IconButton(
                    onClick = { onLevelSelected(SkillLevel.LOW) },
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = SkillLevel.LOW.displayName,
                        tint = if (currentLevel == SkillLevel.LOW) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(if (currentLevel == SkillLevel.LOW) 1f else 0.7f)
                    )
                }

                IconButton(
                    onClick = { onLevelSelected(SkillLevel.MEDIUM) },
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = SkillLevel.MEDIUM.displayName,
                        tint = if (currentLevel == SkillLevel.MEDIUM) {
                            Color(0xFFFFA500)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(if (currentLevel == SkillLevel.MEDIUM) 1f else 0.7f)
                    )
                }

                IconButton(
                    onClick = { onLevelSelected(SkillLevel.HIGH) },
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = SkillLevel.HIGH.displayName,
                        tint = if (currentLevel == SkillLevel.HIGH) {
                            Color(0xFF008000)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(if (currentLevel == SkillLevel.HIGH) 1f else 0.7f)
                    )
                }
            }
        }
    }
}


// --- CONFIGURAÇÃO E EDITOR MANUAL (MANTIDOS ORIGINAIS) ---

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

    // Bounds para detecção de colisão
    var unassignedBounds by remember { mutableStateOf<Rect?>(null) }
    var newGroupBounds by remember { mutableStateOf<Rect?>(null) }
    val groupBounds = remember { mutableStateMapOf<Int, Rect>() }

    fun detectDropTarget(): DropTarget? {
        val container = containerCoords ?: return null
        val dragPos = dragPositionInContainer ?: return null

        // Converte a posição local do drag para a coordenada global (Root)
        // para comparar com os bounds capturados via onGloballyPositioned
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
            .onGloballyPositioned { containerCoords = it }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // COLUNA: NÃO ALOCADOS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { unassignedBounds = it.boundsInRoot() }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "Não alocados",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(unassignedStudents, key = { it.studentId }) { student ->
                        DraggableStudentWrapper(
                            student = student,
                            isDragging = draggedStudent?.student?.studentId == student.studentId,
                            onStart = { localOffset, itemCoords ->
                                draggedStudent = DraggedStudent(student, Location.Unassigned)
                                dragPositionInContainer = containerCoords?.localPositionOf(itemCoords, localOffset)
                            },
                            onMove = { delta ->
                                dragPositionInContainer = dragPositionInContainer?.plus(delta)
                            },
                            onEnd = {
                                val target = detectDropTarget()
                                if (target != null && draggedStudent != null) {
                                    onMoveStudent(draggedStudent!!.student, draggedStudent!!.from, target)
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
                    .padding(horizontal = 8.dp)
            ) {
                // Área para criar novo grupo
                OutlinedButton(
                    onClick = { /* Apenas visual para drop */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .onGloballyPositioned { newGroupBounds = it.boundsInRoot() },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Novo Grupo")
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { groupBounds[group.id] = it.boundsInRoot() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Grupo ${group.id}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    maxItemsInEachRow = 3
                                ) {
                                    group.students.forEach { student ->
                                        DraggableStudentWrapper(
                                            student = student,
                                            isDragging = draggedStudent?.student?.studentId == student.studentId,
                                            onStart = { localOffset, itemCoords ->
                                                draggedStudent = DraggedStudent(student, Location.Group(group.id))
                                                dragPositionInContainer = containerCoords?.localPositionOf(itemCoords, localOffset)
                                            },
                                            onMove = { delta ->
                                                dragPositionInContainer = dragPositionInContainer?.plus(delta)
                                            },
                                            onEnd = {
                                                val target = detectDropTarget()
                                                if (target != null && draggedStudent != null) {
                                                    onMoveStudent(draggedStudent!!.student, draggedStudent!!.from, target)
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

        // OVERLAY DO ITEM ARRASTADO (GHOST)
        if (draggedStudent != null && dragPositionInContainer != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (dragPositionInContainer!!.x - 120).toInt(), // Ajuste de centralização
                            y = (dragPositionInContainer!!.y - 120).toInt()
                        )
                    }
                    .size(90.dp)
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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