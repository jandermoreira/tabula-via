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
import androidx.compose.runtime.*
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
import edu.jm.tabulavia.ui.theme.TabulaViaTheme
import edu.jm.tabulavia.viewmodel.AuthViewModel
import edu.jm.tabulavia.viewmodel.CourseViewModel
import edu.jm.tabulavia.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val courseViewModel: CourseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

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
                Log.e("MainActivity", "GetCredentialException", e)
                Toast.makeText(this@MainActivity, "Falha no login com Google.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            //SplashScreen()
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
                                onBackupClicked = { navController.navigate("backup") },
                                onLoginClicked = { signIn() },
                                onLogoutClicked = { logout() }
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
                            arguments = listOf(navArgument("classId") {
                                type = NavType.LongType
                            })
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
                            arguments = listOf(navArgument("classId") {
                                type = NavType.LongType
                            })
                        ) {
                            StudentListScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() }
                                // Removed onNavigateToSkills lambda here as the feature is removed
                            )
                        }

                        // The 'studentSkills' composable route is commented out and removed from navigation.
                        /*
                        composable(
                            route = "studentSkills/{studentId}",
                            arguments = listOf(navArgument("studentId") {
                                type = NavType.LongType
                            })
                        ) { backStackEntry ->
                            val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                            StudentSkillsScreen(
                                studentId = studentId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        */

                        composable(
                            route = "courseSkills/{classId}",
                            arguments = listOf(navArgument("classId") {
                                type = NavType.LongType
                            })
                        ) { backStackEntry ->
                            val classId = backStackEntry.arguments?.getLong("classId") ?: 0L
                            CourseSkillsScreen(
                                courseId = classId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "frequencyDashboard/{classId}",
                            arguments = listOf(navArgument("classId") {
                                type = NavType.LongType
                            })
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
                            arguments = listOf(navArgument("classId") {
                                type = NavType.LongType
                            })
                        ) {
                            ActivityListScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onActivityClicked = { activity ->
                                    if (activity.description == "Individual") {
                                        navController.navigate("activityStudentList/${activity.activityId}")
                                    } else {
                                        navController.navigate("activityGroupScreen/${activity.activityId}")
                                    }
                                }
                            )
                        }

                        composable(
                            route = "activityStudentList/{activityId}",
                            arguments = listOf(navArgument("activityId") {
                                type = NavType.LongType
                            })
                        ) { backStackEntry ->
                            val activityId =
                                backStackEntry.arguments?.getLong("activityId") ?: 0L
                            ActivityStudentListScreen(
                                activityId = activityId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "activityGroupScreen/{activityId}",
                            arguments = listOf(navArgument("activityId") {
                                type = NavType.LongType
                            })
                        ) { backStackEntry ->
                            val activityId =
                                backStackEntry.arguments?.getLong("activityId") ?: 0L
                            ActivityGroupScreen(
                                activityId = activityId,
                                viewModel = courseViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onGroupClicked = { groupNumber ->
                                    navController.navigate("groupDetails/$activityId/$groupNumber")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
