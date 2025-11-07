package edu.jm.classsupervision

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
import edu.jm.classsupervision.ui.*
import edu.jm.classsupervision.ui.theme.ClassSupervisionTheme
import edu.jm.classsupervision.viewmodel.AuthViewModel
import edu.jm.classsupervision.viewmodel.ClassViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val classViewModel: ClassViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassSupervisionTheme {
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
                            // NavHost completo para o usuário logado
                            val navController = rememberNavController()
                            val scope = rememberCoroutineScope()

                            NavHost(navController = navController, startDestination = "classList") {
                                composable("classList") {
                                    ClassListScreen(
                                        viewModel = classViewModel,
                                        onAddClassClicked = { navController.navigate("addClass") },
                                        onClassClicked = { aClass ->
                                            navController.navigate("classDashboard/${aClass.classId}")
                                        },
                                        onBackupClicked = { navController.navigate("backup") }
                                    )
                                }

                                composable("addClass") {
                                    AddClassScreen(
                                        viewModel = classViewModel,
                                        onClassAdded = { navController.popBackStack() },
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = "classDashboard/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) { backStackEntry ->
                                    val classId = backStackEntry.arguments?.getLong("classId") ?: 0L
                                    ClassDashboardScreen(
                                        classId = classId,
                                        viewModel = classViewModel,
                                        navController = navController,
                                        onNavigateBack = {
                                            classViewModel.clearClassDetails()
                                            navController.popBackStack()
                                        }
                                    )
                                }

                                composable(
                                    route = "studentList/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) {
                                    StudentListScreen(
                                        viewModel = classViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = "frequencyDashboard/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) {
                                    FrequencyDashboardScreen(
                                        viewModel = classViewModel,
                                        onNavigateBack = { navController.popBackStack() },
                                        onStartNewAttendance = {
                                            classViewModel.prepareNewFrequencySession()
                                            navController.navigate("attendanceScreen")
                                        },
                                        onEditAttendance = { session ->
                                            scope.launch {
                                                classViewModel.prepareToEditFrequencySession(session)
                                                navController.navigate("attendanceScreen")
                                            }
                                        }
                                    )
                                }

                                composable("attendanceScreen") {
                                    AttendanceScreen(
                                        viewModel = classViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable("backup") {
                                    BackupScreen(
                                        viewModel = classViewModel,
                                        onNavigateBack = { navController.popBackStack() }
                                    )
                                }

                                composable(
                                    route = "activityList/{classId}",
                                    arguments = listOf(navArgument("classId") { type = NavType.LongType })
                                ) {
                                    ActivityListScreen(
                                        viewModel = classViewModel,
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
