package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.R
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    activityId: Long,
    groupNumber: Int,
    viewModel: CourseViewModel,
    onNavigateBack: () -> Unit
) {
    val students by viewModel.selectedGroupDetails.collectAsState()

    var showAssignSkillsDialog by remember { mutableStateOf(false) }
    var selectedStudentForSkillAssignment by remember { mutableStateOf<Student?>(null) }

    var showAssignSkillsForAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupNumber) {
        viewModel.loadGroupDetails(groupNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupo $groupNumber") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearGroupDetails()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAssignSkillsForAllDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "Atribuir habilidades ao grupo"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        val context = LocalContext.current

        val studentsSorted = remember(students) {
            students.sortedBy { it.displayName.trim().lowercase() }
        }

        val studentIconMap = remember(studentsSorted, context) {
            studentsSorted.associate { student ->
                val iconIndex = (student.studentId.mod(80L) + 1).toInt()
                val iconName = "student_${iconIndex}"
                val drawableResId =
                    context.resources.getIdentifier(iconName, "drawable", context.packageName)
                student.studentId to (drawableResId.takeIf { it != 0 } ?: R.drawable.student_0)
            }
        }

        if (studentsSorted.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum aluno no grupo.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(studentsSorted, key = { it.studentId }) { student ->
                    val studentDrawableResId =
                        studentIconMap[student.studentId] ?: R.drawable.student_0
                    StudentItem(
                        student = student,
                        drawableResId = studentDrawableResId,
                        modifier = Modifier.clickable {
                            selectedStudentForSkillAssignment = student
                            viewModel.loadStudentDetails(student.studentId)
                            showAssignSkillsDialog = true
                        }
                    )
                }
            }
        }
    }

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

    if (showAssignSkillsForAllDialog) {
        AssignGroupSkillsForAllDialog(
            students = remember(students) { students.sortedBy { it.displayName.trim().lowercase() } },
            viewModel = viewModel,
            activityId = activityId,
            onDismiss = { showAssignSkillsForAllDialog = false }
        )
    }
}

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
        title = { Text("Habilidades para o grupo (${students.size} alunos)") },
        text = {
            Column {
                Text(
                    text = "As habilidades selecionadas serão registradas para todos os membros do grupo.",
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
            ) { Text("Salvar para o grupo") }
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
        title = { Text("Habilidades para ${student.displayName}") },
        text = {
            Column {
                Text(
                    text = "Destaques da atividade: ${highlightedSkillNames.size}",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

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
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = skillName,
                style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val buttonSize = 32.dp
                val iconSize = 18.dp

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