package edu.jm.tabulavia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.jm.tabulavia.ui.*
import edu.jm.tabulavia.ui.theme.TabulaViaTheme
import edu.jm.tabulavia.viewmodel.AuthViewModel
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val courseViewModel: CourseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TabulaViaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen()
                        LaunchedEffect(Unit) {
                            delay(1500) // Atraso para exibir a splash
                            showSplash = false
                        }
                    } else {
                        val user by authViewModel.user.collectAsState()
                        if (user == null) {
                            val errorMessage by authViewModel.errorMessage.collectAsState()
                            LoginScreen(
                                onLoginClick = { email, password -> authViewModel.login(email, password) },
                                onSignUpClick = { email, password -> authViewModel.signUp(email, password) },
                                errorMessage = errorMessage
                            )
                        } else {
                            val navController = rememberNavController()
                            val scope = rememberCoroutineScope()

                            NavHost(navController = navController, startDestination = "courseList") {
                                composable("courseList") {
                                    CourseListScreen(
                                        viewModel = courseViewModel,
                                        onAddCourseClicked = { navController.navigate("addCourse") },
                                        onCourseClicked = { course ->
                                            navController.navigate("courseDashboard/${course.classId}")
                                        },
                                        onBackupClicked = { navController.navigate("backup") }
                                    )
                                }

                                composable("addCourse") {
                                    AddCourseScreen(
                                        viewModel = courseViewModel,
                                        onCourseAdded = { navController.popBackStack() },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = "courseDashboard/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val classId = backStackEntry.arguments?.getLong("classId") ?: 0L
                                    CourseDashboardScreen(
                                        classId = classId,
                                        viewModel = courseViewModel,
                                        navController = navController,
                                        onNavigateBack = {
                                            courseViewModel.clearCourseDetails()
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                composable(
                                    route = "studentList/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) {
                                    StudentListScreen(
                                        viewModel = courseViewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onNavigateToSkills = { studentId ->
                                            navController.navigate("studentSkills/$studentId")
                                        }
                                    )
                                }

                                composable(
                                    route = "studentSkills/{studentId}", // Nova rota
                                    arguments = listOf(navArgument("studentId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                                    StudentSkillsScreen(
                                        studentId = studentId,
                                        viewModel = courseViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = "frequencyDashboard/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) {
                                    FrequencyDashboardScreen(
                                        viewModel = courseViewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onStartNewAttendance = {
                                            courseViewModel.prepareNewFrequencySession()
                                            navController.navigate("attendanceScreen")
                                        },
                                        onEditAttendance = { session ->
                                            scope.launch {
                                                courseViewModel.prepareToEditFrequencySession(session)
                                                navController.navigate("attendanceScreen")
                                            }
                                        }
                                    )
                                }

                                composable("attendanceScreen") {
                                    AttendanceScreen(
                                        viewModel = courseViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("backup") {
                                    BackupScreen(
                                        viewModel = courseViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = "activityList/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) {
                                    ActivityListScreen(
                                        viewModel = courseViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
