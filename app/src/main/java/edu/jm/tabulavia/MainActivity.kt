package edu.jm.tabulavia

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import edu.jm.tabulavia.ui.*
import edu.jm.tabulavia.ui.theme.TabulaViaTheme
import edu.jm.tabulavia.viewmodel.AuthViewModel
import edu.jm.tabulavia.viewmodel.CourseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main entry point for the Tabula Via application.
 *
 * Tabula Via is a tool designed to assist educators in managing their classes.
 * The application offers features for registering courses and students,
 * tracking attendance, creating activities, and monitoring skills.
 * Navigation is built with Jetpack Compose, and data persistence
 * is handled locally, with an option for cloud backup via Firebase.
 */
class MainActivity : ComponentActivity() {

    private val courseViewModel: CourseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var oneTapClient: SignInClient

    private val oneTapSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        authViewModel.signInWithGoogle(idToken)
                    } else {
                        Log.e("MainActivity", "Google ID token was null.")
                        Toast.makeText(
                            this,
                            "Erro no login: token não encontrado.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: ApiException) {
                    Log.e("MainActivity", "Google Sign-In failed", e)
                    Toast.makeText(this, "Falha no login com Google.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(
                    "MainActivity",
                    "One-tap sign-in activity was cancelled or failed. Result code: ${result.resultCode}"
                )
            }
        }

    private fun signIn() {
        val signInRequest = GetSignInIntentRequest.builder()
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        oneTapClient.getSignInIntent(signInRequest)
            .addOnSuccessListener(this) { pendingIntent ->
                val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent).build()
                oneTapSignInLauncher.launch(intentSenderRequest)
            }
            .addOnFailureListener(this) { e ->
                Log.e("MainActivity", "Google Sign-In get intent failed", e)
                Toast.makeText(this, "Falha ao iniciar login com Google.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        Firebase.auth.signOut()
        authViewModel.clearUser()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oneTapClient = Identity.getSignInClient(this)

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
                            delay(1500) // Delay to show splash screen
                            showSplash = false
                        }
                    } else {
                        val navController = rememberNavController()
                        val scope = rememberCoroutineScope()

                        NavHost(navController = navController, startDestination = "courseList") {
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
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToSkills = { studentId ->
                                        navController.navigate("studentSkills/$studentId")
                                    }
                                )
                            }

                            composable(
                                route = "studentSkills/{studentId}", // New route
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
