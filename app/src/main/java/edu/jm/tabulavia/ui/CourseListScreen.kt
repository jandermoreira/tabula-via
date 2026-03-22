/**
 * CourseListScreen.kt
 *
 * Displays the list of courses grouped by academic year.
 * Provides authentication actions and backup/restore operations
 * through a dialog embedded in this screen.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Course
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.AuthViewModel
import edu.jm.tabulavia.viewmodel.ClassViewModel
import kotlinx.coroutines.launch
import edu.jm.tabulavia.BuildConfig


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
        /**
         * Displays the main course list screen.
         *
         * @param viewModel ViewModel responsible for course data and backup/restore operations.
         * @param authViewModel ViewModel responsible for authentication state.
         * @param onAddCourseClicked Callback triggered when add course is requested.
         * @param onCourseClicked Callback triggered when a course is selected.
         * @param onLoginClicked Callback triggered when login is requested.
         * @param onLogoutClicked Callback triggered when logout is requested.
         */
fun CourseListScreen(
    viewModel: ClassViewModel,
    authViewModel: AuthViewModel,
    onAddCourseClicked: () -> Unit,
    onCourseClicked: (Course) -> Unit,
    onLoginClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    MessageHandler(viewModel)

    val courseList by viewModel.classes.collectAsState()
    val groupedCourses = courseList
        .groupBy { it.academicYear }
        .toSortedMap(compareByDescending { it })

    val snackbarHostState = remember { SnackbarHostState() }
//    val userMessage by viewModel.userMessage.collectAsState()
    val authenticatedUser by authViewModel.user.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var isBackupLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

//    LaunchedEffect(Unit) {
//        viewModel.loadAllCourses()
//    }

//    /**
//     * Shows user messages in a snackbar.
//     */
//    LaunchedEffect(userMessage) {
//        userMessage?.let {
//            snackbarHostState.showSnackbar(it)
//            viewModel.onUserMessageShown()
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (BuildConfig.FLAVOR == "dev")
                        Text("Tabula Via (DEV)")
                    else
                        Text("Tabual Via")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {

                    /**
                     * Displays authentication and backup actions.
                     */
                    if (authenticatedUser != null) {
                        IconButton(onClick = { showBackupDialog = true }) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Cópia de segurança"
                            )
                        }
                        IconButton(onClick = onLogoutClicked) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sair"
                            )
                        }
                    } else {
                        IconButton(onClick = onLoginClicked) {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Entrar"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCourseClicked) {
                Icon(
                    Icons.Default.GroupAdd,
                    contentDescription = "Adicionar turma"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        /**
         * Displays the list of courses grouped by academic year.
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            groupedCourses.forEach { (year, coursesInYear) ->

                stickyHeader {
                    Text(
                        text = year,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(coursesInYear) { course ->
                    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                        CourseItem(
                            course = course,
                            onClick = { onCourseClicked(course) }
                        )
                    }
                }
            }
        }
    }

    /**
     * Backup and restore dialog.
     */
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBackupLoading) showBackupDialog = false
            },
            title = { Text("Cópia de segurança") },
            text = {
                if (isBackupLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        /**
                         * Executes backup operation.
                         */
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBackupLoading = true
                                    viewModel.backup()
                                    isBackupLoading = false
                                    showBackupDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.CloudUpload,
                                contentDescription = "Fazer cópia de segurança"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("FAZER CÓPIA")
                        }

                        /**
                         * Executes restore operation.
                         */
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBackupLoading = true
                                    viewModel.restore()
                                    isBackupLoading = false
                                    showBackupDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.CloudDownload,
                                contentDescription = "Restaurar cópia de segurança"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("RESTAURAR CÓPIA")
                        }
                        /**
                         * Clears the entire local database.
                         */
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBackupLoading = true
                                    viewModel.clearDatabase()
                                    isBackupLoading = false
                                    showBackupDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Limpar base de dados"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("LIMPAR BASE")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showBackupDialog = false },
                    enabled = !isBackupLoading
                ) {
                    Text("Fechar")
                }
            }
        )
    }
}

/**
 * Displays a single course item inside a card.
 *
 * @param course The course to be displayed.
 * @param onClick Callback triggered when the item is selected.
 */
@Composable
fun CourseItem(
    course: Course,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Turma",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = course.className,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${course.academicYear}/${course.period}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}