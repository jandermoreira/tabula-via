/**
 * ActivityGroupScreen.kt
 *
 * This file contains the UI components for managing student groups in an activity.
 * It supports both random and manual grouping, including a drag-and-drop editor.
 */

package edu.jm.tabulavia.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.wear.compose.foundation.weight
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.grouping.DropTarget
import edu.jm.tabulavia.model.grouping.Group
import edu.jm.tabulavia.model.grouping.Location
import edu.jm.tabulavia.viewmodel.CourseViewModel
import edu.jm.tabulavia.ui.StudentEmojiColorHelper.mapStudentIdToEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    activityId: Long, viewModel: CourseViewModel, onNavigateBack: () -> Unit
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

    val groupsListState = rememberLazyListState()

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

    LaunchedEffect(groups.size) {
        if (groups.isNotEmpty()) {
            groupsListState.animateScrollToItem(index = groups.size - 1)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(activity?.title ?: "Montar Grupos") }, navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
        }, actions = {
            if (uiState == GroupUiState.SHOW_GROUPS) {
                IconButton(
                    onClick = {
                        viewModel.enterManualMode(forceRefresh = true)
                        uiState = GroupUiState.CONFIGURE
                    }) {
                    Icon(Icons.Default.Edit, null)
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        )
        )
    }, floatingActionButton = {
        val isConfiguring = uiState == GroupUiState.CONFIGURE || uiState == GroupUiState.NO_GROUPS

        if (isConfiguring) {
            when (viewModel.groupingCriterion) {
                "Manual" -> {
                    FloatingActionButton(
                        onClick = {
                            viewModel.exitManualMode()
                            uiState = GroupUiState.SHOW_GROUPS
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check, contentDescription = "Finish"
                        )
                    }
                }

                "Aleatório" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (uiState == GroupUiState.CONFIGURE) {
                                    uiState = GroupUiState.SHOW_GROUPS
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, contentDescription = "Cancel"
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                viewModel.createBalancedGroups()
                                viewModel.enterManualMode(forceRefresh = true)
                                uiState = GroupUiState.SHOW_GROUPS
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check, contentDescription = "Create"
                            )
                        }
                    }
                }
            }
        }
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
                    ConfigurationView(viewModel = viewModel, onCancel = {
                        if (uiState == GroupUiState.CONFIGURE) uiState = GroupUiState.SHOW_GROUPS
                    }, onGroupsCreated = { uiState = GroupUiState.SHOW_GROUPS })
                }

                GroupUiState.SHOW_GROUPS -> {
                    GroupsExpandedView(
                        groups = groups,
                        lazyListState = groupsListState,
                        viewModel = viewModel,
                        onStudentClick = { student ->
                            selectedStudentForSkillAssignment = student
                            viewModel.loadStudentDetails(student.studentId)
                            showAssignSkillsDialog = true
                        },
                        onGroupActionClick = { groupStudents ->
                            selectedGroupForSkillAssignment = groupStudents
                            showAssignGroupSkillsDialog = true
                        })
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
 *
 * @param groups The list of student groups to display.
 * @param lazyListState The LazyListState for controlling the scroll state of the list.
 * @param onStudentClick Callback when a student item is clicked.
 * @param onGroupActionClick Callback when a group action icon is clicked.
 * @param viewModel The CourseViewModel instance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupsExpandedView(
    groups: List<List<Student>>,
    lazyListState: LazyListState,
    onStudentClick: (Student) -> Unit,
    onGroupActionClick: (List<Student>) -> Unit,
    viewModel: CourseViewModel
) {
    val todaysAttendance by viewModel.todaysAttendance.collectAsState()

    LazyColumn(
        state = lazyListState,
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
                            val isStudentAbsent =
                                todaysAttendance[student.studentId] == AttendanceStatus.ABSENT
                            StudentItem(
                                student = student,
                                emoji = mapStudentIdToEmoji(student.studentId),
                                isAbsent = isStudentAbsent,
                                modifier = Modifier
                                    .width(90.dp)
                                    .clickable { onStudentClick(student) }
//                                    .alpha(if (isStudentAbsent) 0.5f else 1f)
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
 * Allows students to be moved between an unassigned pool and various groups.
 * Features a fade-out animation for the dragged student ghost on drop.
 *
 * @param groups The current list of manual groups.
 * @param unassignedStudents The list of students not currently in any group.
 * @param onMoveStudent Callback to handle moving a student between locations.
 * @param viewModel The CourseViewModel instance.
 * @param isLandscape Boolean indicating if the device is in landscape orientation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ManualGroupEditorView(
    groups: List<Group>,
    unassignedStudents: List<Student>,
    onMoveStudent: (Student, Location, DropTarget) -> Unit,
    viewModel: CourseViewModel,
    isLandscape: Boolean
) {
    var draggedStudent by remember { mutableStateOf<DraggedStudent?>(null) }
    var dragPositionInContainer by remember { mutableStateOf<Offset?>(null) }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var unassignedBounds by remember { mutableStateOf<Rect?>(null) }
    var newGroupBounds by remember { mutableStateOf<Rect?>(null) }
    val groupBounds = remember { mutableStateMapOf<Int, Rect>() }

    // State for the drag ghost alpha animation
    val ghostTargetAlpha = remember { mutableFloatStateOf(0f) }
    val animatedGhostAlpha by animateFloatAsState(
        targetValue = ghostTargetAlpha.floatValue,
        animationSpec = tween(durationMillis = 300),
        label = "dragGhostAlphaAnimation"
    )

    val scope = rememberCoroutineScope()

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dropTargetBackground = primaryColor.copy(alpha = 0.05f)
    val dashColorInactive = Color.Gray.copy(alpha = 0.4f)

    /**
     * Detects the drop target based on the current drag position.
     * @return The DropTarget if a valid target is found, otherwise null.
     */
    fun detectDropTarget(): DropTarget? {
        val container = containerCoords ?: return null
        val dragPos = dragPositionInContainer ?: return null
        val rootPos = container.localToRoot(dragPos)

        if (unassignedBounds?.contains(rootPos) == true) return DropTarget.Unassigned
        if (newGroupBounds?.contains(rootPos) == true) return DropTarget.NewGroup

        groupBounds.forEach { (groupId, rect) ->
            if (rect.contains(rootPos)) {
                if (groups.any { it.id == groupId }) {
                    return DropTarget.ExistingGroup(groupId)
                } else {
                    return null
                }
            }
        }
        return null
    }
    if (!isLandscape) {
        CriterionSelector(viewModel = viewModel, isFullWidth = true)
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
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
                                    ghostTargetAlpha.floatValue = 1f
                                },
                                onMove = { delta ->
                                    dragPositionInContainer = dragPositionInContainer?.plus(delta)
                                },
                                onEnd = {
                                    val target = detectDropTarget()
                                    val currentDraggedStudent =
                                        draggedStudent // Capture for delayed clearing

                                    if (target != null && currentDraggedStudent != null) {
                                        onMoveStudent(
                                            currentDraggedStudent.student,
                                            currentDraggedStudent.from,
                                            target
                                        )
                                    }

                                    ghostTargetAlpha.floatValue = 0f // Trigger fade out animation

                                    // Clear dragged student state after animation completes
                                    scope.launch {
                                        delay(300) // Match animation duration
                                        if (draggedStudent == currentDraggedStudent) { // Only clear if no new drag started
                                            draggedStudent = null
                                            dragPositionInContainer = null
                                        }
                                    }
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // ASSIGNED GROUPS COLUMN
            Column(
                modifier = Modifier
                    .weight(if (isLandscape) 3f else 2f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) {
                // Block: Toolbar Row (Drop Area + Criterion Menu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Group drop target area (Dashed Box)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isLandscape) 60.dp else 80.dp)
                            .onGloballyPositioned { newGroupBounds = it.boundsInRoot() }
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .drawBehind {
                                drawRoundRect(
                                    color = if (draggedStudent != null) primaryColor else dashColorInactive,
                                    style = Stroke(
                                        width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(10f, 10f), 0f
                                        )
                                    ),
                                    cornerRadius = CornerRadius(12.dp.toPx())
                                )
                            }, contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GroupAdd,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Arraste para novo grupo",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (isLandscape) {
                        CriterionSelector(viewModel = viewModel, isFullWidth = false)
                    }
                }

                val manualGroupsListState = rememberLazyListState()

                LazyColumn(
                    state = manualGroupsListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    itemsIndexed(
                        items = groups.filter { it.students.isNotEmpty() },
                        key = { _, group -> group.id }) { index, group ->

                        // Cleans up the bounding box entry when the group is removed from composition
                        DisposableEffect(group.id) {
                            onDispose {
                                groupBounds.remove(group.id)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    groupBounds[group.id] = coordinates.boundsInRoot()
                                }, colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Grupo ${index + 1}",
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
                                                    student, Location.Group(group.id)
                                                )
                                                dragPositionInContainer =
                                                    containerCoords?.localPositionOf(coords, offset)
                                                ghostTargetAlpha.floatValue = 1f // Show ghost
                                            },
                                            onMove = { delta ->
                                                dragPositionInContainer =
                                                    dragPositionInContainer?.plus(delta)
                                            },
                                            onEnd = {
                                                val target = detectDropTarget()
                                                val currentDraggedStudent = draggedStudent

                                                if (target != null && currentDraggedStudent != null) {
                                                    onMoveStudent(
                                                        currentDraggedStudent.student,
                                                        currentDraggedStudent.from,
                                                        target
                                                    )
                                                }

                                                ghostTargetAlpha.floatValue = 0f

                                                // Clear dragged student state after animation completes
                                                scope.launch {
                                                    delay(300)
                                                    if (draggedStudent == currentDraggedStudent) {
                                                        draggedStudent = null
                                                        dragPositionInContainer = null
                                                    }
                                                }
                                            },
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                var previousGroupsSize by remember { mutableIntStateOf(groups.size) }
                LaunchedEffect(groups.size) {
                    if (groups.size > previousGroupsSize) {
                        manualGroupsListState.animateScrollToItem(index = groups.size - 1)
                    }
                    previousGroupsSize = groups.size
                }
            }
        }

        // FLOATING DRAG GHOST: Renders the draggable student during a drag operation
        if (draggedStudent != null && dragPositionInContainer != null) {
            val currentPos = dragPositionInContainer!!
            Box(modifier = Modifier
                .offset {
                    IntOffset(
                        currentPos.x.toInt() - 40.dp.toPx().toInt(),
                        currentPos.y.toInt() - 40.dp.toPx().toInt()
                    )
                }
                .size(80.dp)
                .alpha(animatedGhostAlpha) // Apply the animated alpha for fade-out
            ) {
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    val isStudentAbsent =
                        viewModel.todaysAttendance.collectAsState().value[draggedStudent!!.student.studentId] == AttendanceStatus.ABSENT
                    StudentItem(
                        student = draggedStudent!!.student,
                        emoji = mapStudentIdToEmoji(draggedStudent!!.student.studentId),
                        isAbsent = isStudentAbsent,
//                        modifier = Modifier.alpha(if (isStudentAbsent) 0.5f else 1f)
                    )
                }
            }
        }
    }
}


/**
 * Wrapper component to handle drag gestures for a single student.
 * It detects drag start, move, and end events, and provides callbacks.
 *
 * @param student The student object being dragged.
 * @param isDragging True if this student is currently being dragged.
 * @param onStart Callback triggered when a drag gesture starts. Provides initial offset and coordinates.
 * @param onMove Callback triggered when the drag position changes. Provides the drag amount.
 * @param onEnd Callback triggered when a drag gesture ends (either dropped or cancelled).
 * @param viewModel The CourseViewModel instance.
 */
@Composable
private fun DraggableStudentWrapper(
    student: Student,
    isDragging: Boolean,
    onStart: (Offset, LayoutCoordinates) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
    viewModel: CourseViewModel
) {
    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val todaysAttendance by viewModel.todaysAttendance.collectAsState()
    val isStudentAbsent = todaysAttendance[student.studentId] == AttendanceStatus.ABSENT

    Box(modifier = Modifier
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
        }) {
        StudentItem(
            student = student,
            emoji = mapStudentIdToEmoji(student.studentId),
            isAbsent = isStudentAbsent,
//            modifier = Modifier.alpha(if (isStudentAbsent) 0.5f else 1f)
        )
    }
}

/**
 * ConfigurationView
 * Manages the grouping setup screen. In Landscape, it integrates the
 * criterion selector into the manual editor tools to maximize vertical space.
 *
 * @param viewModel The CourseViewModel instance.
 * @param onCancel Callback for when the configuration is cancelled.
 * @param onGroupsCreated Callback for when groups have been successfully created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationView(
    viewModel: CourseViewModel, onCancel: () -> Unit, onGroupsCreated: () -> Unit
) {
    val groupingCriteria = listOf("Aleatório", "Manual")
    val formationOptions = listOf("Número de grupos", "Alunos por grupo")

    val currentConfig = LocalConfiguration.current
    val isLandscape = currentConfig.orientation == Configuration.ORIENTATION_LANDSCAPE

    var menuExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(if (isLandscape) 8.dp else 16.dp)
            .fillMaxHeight()
    ) {
        if (viewModel.groupingCriterion == "Manual") {
            // Block: Manual Mode
            LaunchedEffect(Unit) {
                viewModel.enterManualMode()
            }

            ManualGroupEditorView(
                groups = viewModel.manualGroups,
                unassignedStudents = viewModel.unassignedStudents,
                onMoveStudent = { student, from, to ->
                    viewModel.moveStudent(
                        student, from, to
                    )
                },
                isLandscape = isLandscape,
                viewModel = viewModel
            )
        } else {
            CriterionSelector(viewModel = viewModel, isFullWidth = true)
            Spacer(Modifier.height(16.dp))

            // Random grouping parameters
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
                        Text(text = option, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = viewModel.groupFormationValue,
                onValueChange = { input ->
                    viewModel.groupFormationValue = input.filter { it.isDigit() }
                },
                label = { Text(viewModel.groupFormationType) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CriterionSelector(
    viewModel: CourseViewModel, isFullWidth: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val criteria = listOf("Aleatório", "Manual")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = if (isFullWidth) Modifier.fillMaxWidth() else Modifier.width(200.dp)
    ) {
        OutlinedTextField(
            value = viewModel.groupingCriterion,
            onValueChange = {},
            readOnly = true,
            label = { if (isFullWidth) Text("Escolha o critério") else Text("Critério") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            criteria.forEach { criterion ->
                DropdownMenuItem(text = { Text(criterion) }, onClick = {
                    viewModel.groupingCriterion = criterion
                    expanded = false
                })
            }
        }
    }
}

/**
 * Dialog to apply skill assessments to multiple students at once.
 *
 * @param students The list of students in the group to assess.
 * @param viewModel The CourseViewModel instance.
 * @param activityId The ID of the current activity.
 * @param onDismiss Callback to dismiss the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignGroupSkillsForAllDialog(
    students: List<Student>, viewModel: CourseViewModel, activityId: Long?, onDismiss: () -> Unit
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

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Habilidades para o Grupo") }, text = {
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
                        currentLevel = skillLevels[skill.skillName] ?: SkillLevel.NOT_APPLICABLE,
                        onLevelSelected = { new ->
                            skillLevels = skillLevels + (skill.skillName to new)
                        },
                        isHighlighted = highlightedSkillNames.contains(skill.skillName)
                    )
                }
            }
        }
    }, confirmButton = {
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
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

/**
 * Dialog to assess skills for a specific student within the grouping screen.
 *
 * @param student The student for whom skills are being assigned.
 * @param viewModel The CourseViewModel instance.
 * @param activityId The ID of the current activity.
 * @param onDismiss Callback to dismiss the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignGroupSkillsDialog(
    student: Student, viewModel: CourseViewModel, activityId: Long?, onDismiss: () -> Unit
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

/**
 * A row component for a single skill assessment, displaying options (Low, Medium, High).
 *
 * @param skillName The name of the skill.
 * @param currentLevel The currently selected skill level.
 * @param onLevelSelected Callback when a new skill level is selected.
 * @param isHighlighted True if the skill is highlighted for the current activity.
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

        @OptIn(ExperimentalMaterial3Api::class) CompositionLocalProvider(
            LocalMinimumInteractiveComponentSize provides 0.dp
        ) {
            Row {
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.NOT_APPLICABLE) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.NOT_APPLICABLE) MaterialTheme.colorScheme.primary else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.LOW) }, modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbDown,
                        null,
                        modifier = Modifier.size(16.dp),
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
                        Icons.Default.Circle,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.MEDIUM) Color(0xFFFFA500) else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
                IconButton(
                    onClick = { onLevelSelected(SkillLevel.HIGH) }, modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.ThumbUp,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentLevel == SkillLevel.HIGH) Color(0xFF008000) else Color.Gray.copy(
                            alpha = 0.5f
                        )
                    )
                }
            }
        }
    }
}