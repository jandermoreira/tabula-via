package edu.jm.classsupervision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
                    val user by authViewModel.user.collectAsState()
                    val errorMessage by authViewModel.errorMessage.collectAsState()

                    if (user == null) {
                        LoginScreen(
                            onLoginClick = { email, password -> authViewModel.login(email, password) },
                            onSignUpClick = { email, password -> authViewModel.signUp(email, password) },
                            errorMessage = errorMessage
                        )
                    } else {
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
                                    onBackupClicked = { navController.navigate("backup") } // Rota para a nova tela
                                )
                            }

                            composable("addClass") {
                                AddClassScreen(
                                    viewModel = classViewModel,
                                    onClassAdded = { navController.popBackStack() }
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
                            
                            composable("backup") { // Definição da nova rota de backup
                                BackupScreen(
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
