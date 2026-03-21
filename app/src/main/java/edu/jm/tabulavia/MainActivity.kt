/**
 * MainActivity.kt
 *
 * Main entry point of the application.
 * Responsible for navigation setup and authentication integration using persistent String IDs.
 */
package edu.jm.tabulavia

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import edu.jm.tabulavia.ui.*
import edu.jm.tabulavia.ui.theme.TabulaViaTheme
import edu.jm.tabulavia.viewmodel.AuthViewModel
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val courseViewModel: CourseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

    /**
     * Initiates Google Sign-In flow using Credential Manager.
     */
    private fun signIn() {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

                authViewModel.signInWithGoogle(credential.idToken)

            } catch (e: GetCredentialException) {
                Log.e("MainActivity", "Erro de credenciais", e)
                Toast.makeText(this@MainActivity, "Login cancelado ou falhou.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro inesperado no login", e)
                Toast.makeText(this@MainActivity, "Sem conexão ou erro de serviço Google.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Signs out the current user.
     */
    private fun logout() {
        Firebase.auth.signOut()
        authViewModel.clearUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager.create(this)

        setContent {
            TabulaViaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController, startDestination = "splash"
                    ) {

                        composable("splash") {
                            LaunchedEffect(Unit) {
                                delay(600)
                                navController.navigate("courseList") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }

                        composable("courseList") {
                            CourseListScreen(
                                viewModel = courseViewModel,
                                authViewModel = authViewModel,
                                onAddCourseClicked = { navController.navigate("addCourse") },
                                onCourseClicked = { course ->
                                    navController.navigate("courseDashboard/${course.classId}")
                                },
                                onLoginClicked = { signIn() },
                                onLogoutClicked = { logout() })
                        }

                        composable("addCourse") {
                            AddCourseScreen(
                                viewModel = courseViewModel,
                                onCourseAdded = { navController.popBackStack() },
                                onNavigateBack = { navController.popBackStack() })
                        }

                        composable(
                            route = "courseDashboard/{classId}",
                            arguments = listOf(navArgument("classId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val classId = backStackEntry.arguments?.getString("classId") ?: ""
                            CourseDashboardScreen(
                                classId = classId,
                                viewModel = courseViewModel,
                                navController = navController,
                                onNavigateBack = {
                                    courseViewModel.resetCourseState()
                                    navController.popBackStack()
                                })
                        }

                        composable(
                            route = "studentList/{classId}",
                            arguments = listOf(navArgument("classId") { type = NavType.StringType })
                        ) {
                            StudentListScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() })
                        }

                        composable(
                            route = "courseSkills/{classId}",
                            arguments = listOf(navArgument("classId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val classId = backStackEntry.arguments?.getString("classId") ?: ""
                            CourseSkillsScreen(
                                courseId = classId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() })
                        }

                        composable(
                            route = "frequencyDashboard/{classId}",
                            arguments = listOf(navArgument("classId") { type = NavType.StringType })
                        ) {
                            AttendanceDashboardScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onStartNewAttendance = {
                                    courseViewModel.prepareNewSession()
                                    navController.navigate("attendanceScreen")
                                },
                                onEditAttendance = { session ->
                                    courseViewModel.prepareToEditFrequencySession(session)
                                    navController.navigate("attendanceScreen")
                                })
                        }

                        composable("attendanceScreen") {
                            AttendanceScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() })
                        }

                        composable(
                            route = "activityList/{classId}",
                            arguments = listOf(navArgument("classId") { type = NavType.StringType })
                        ) {
                            ActivityListScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onActivityClicked = { activity ->
                                    val route = if (activity.description == "Individual") "activityStudentList" else "activityGroupScreen"
                                    navController.navigate("$route/${activity.activityId}")
                                })
                        }

                        composable(
                            route = "activityStudentList/{activityId}",
                            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
                            ActivityStudentListScreen(
                                activityId = activityId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() })
                        }

                        composable(
                            route = "activityGroupScreen/{activityId}",
                            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val activityId = backStackEntry.arguments?.getString("activityId") ?: ""
                            ActivityGroupScreen(
                                activityId = activityId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}