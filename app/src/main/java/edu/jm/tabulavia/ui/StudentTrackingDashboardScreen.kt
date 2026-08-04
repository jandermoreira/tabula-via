/**
 * StudentTrackingDashboardScreen.kt
 *
 * Screen that displays the pedagogical tracking dashboard for a class.
 * It provides a summary of students' tracking states and a detailed list.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import edu.jm.tabulavia.model.EvidenceHistoryItem
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.model.StudentTrackingState
import edu.jm.tabulavia.ui.theme.Amber
import edu.jm.tabulavia.ui.theme.SkyBlue
import edu.jm.tabulavia.viewmodel.ClassViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying the pedagogical tracking dashboard for a class.
 *
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onStudentClick Callback when a student row is clicked (can be used for navigation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTrackingDashboardScreen(
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit,
    onStudentClick: (String) -> Unit
) {
    val dashboardItems by viewModel.dashboardItems.collectAsState()
    var selectedItemForHistory by remember { mutableStateOf<StudentDashboardItem?>(null) }

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
            TrackingSummaryCards(
                items = dashboardItems,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dashboardItems, key = { it.student.studentId }) { item ->
                    StudentTrackingRow(
                        item = item,
                        onClick = { selectedItemForHistory = item }
                    )
                }
            }
        }

        selectedItemForHistory?.let { item ->
            StudentEvidencesDialog(
                item = item,
                onDismiss = { selectedItemForHistory = null }
            )
        }
    }
}

/**
 * Dialog displaying the chronological evidence history of a student.
 */
@Composable
fun StudentEvidencesDialog(
    item: StudentDashboardItem,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Evidências: ${item.student.effectiveName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (item.evidenceHistory.isEmpty()) {
                    Text(
                        text = "Nenhuma evidência registrada.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    item.evidenceHistory.forEach { historyItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = historyItem.evidenceName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(historyItem.deadline)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", historyItem.score),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (historyItem.score < 5.0) Color.Red else Color.Unspecified
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
