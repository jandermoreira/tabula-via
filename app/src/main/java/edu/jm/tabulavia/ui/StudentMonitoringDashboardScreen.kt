/**
 * StudentMonitoringDashboardScreen.kt
 *
 * Screen that displays the pedagogical monitoring dashboard for a class.
 * It provides a summary of students' monitoring states and a detailed list.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.viewmodel.ClassViewModel

/**
 * Screen displaying the pedagogical monitoring dashboard for a class.
 *
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onStudentClick Callback when a student row is clicked (can be used for navigation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMonitoringDashboardScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit,
    onStudentClick: (String) -> Unit
) {
    val dashboardItems by viewModel.dashboardItems.collectAsState()
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    Scaffold(
        topBar = {
            TabulaTopBar(
                title = "Acompanhamento",
                viewModel = viewModel,
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            MonitoringSummaryCards(
                items = dashboardItems,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dashboardItems, key = { it.student.studentId }) { item ->
                    StudentMonitoringRow(
                        item = item,
                        onClick = { selectedStudent = item.student }
                    )
                }
            }
        }

        selectedStudent?.let { student ->
            StudentDetailsDialog(
                student = student,
                attendancePercentage = dashboardItems.find { it.student.studentId == student.studentId }?.summary?.attendance?.toFloat(),
                viewModel = viewModel,
                onDismiss = { selectedStudent = null }
            )
        }
    }
}
