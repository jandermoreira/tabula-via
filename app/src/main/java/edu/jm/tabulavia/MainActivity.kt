package edu.jm.tabulavia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            authViewModel.signInWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Handle error
        }
    }

    private fun logout() {
        Firebase.auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener { 
            authViewModel.clearUser()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
                                    onLoginClicked = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
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
                                route = "studentSkills/{studentId}", // New route
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
                                    onNavigateBack = { navController.popBackStack() },
                                    onActivityClicked = { activity ->
                                        if (activity.description == "Individual") {
                                            navController.navigate("activityStudentList/${activity.activityId}")
                                        } else {
                                            // TODO: Handle group activity click
                                        }
                                    }
                                )
                            }

                            composable(
                                route = "activityStudentList/{activityId}",
                                arguments = listOf(navArgument("activityId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val activityId = backStackEntry.arguments?.getLong("activityId") ?: 0L
                                ActivityStudentListScreen(
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