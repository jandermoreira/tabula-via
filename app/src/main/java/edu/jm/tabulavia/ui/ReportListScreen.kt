/**
 * File: ReportListScreen.kt
 * Description: Screen that lists available reports for a class.
 */

package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.viewmodel.ClassViewModel
import kotlinx.coroutines.launch

/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
/**
 * Screen that lists available reports for a class.
 *
 * @param classId The unique identifier for the class.
 * @param viewModel The shared ClassViewModel instance.
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onNavigateToTracking Callback to navigate to the pedagogical tracking dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    classId: String,
    viewModel: ClassViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTracking: () -> Unit
) {
    var reportContent by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TabulaTopBar(
                title = "Relatórios",
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardCard(
                title = "Listagem de frequência",
                subtitle = "Exportar lista completa de presenças",
                icon = Icons.Default.Description,
                onClick = {
                    scope.launch {
                        reportContent = viewModel.generateAttendanceReport()
                        showDialog = true
                    }
                }
            )

            DashboardCard(
                title = "Acompanhamento Pedagógico",
                subtitle = "Diagnóstico inteligente de evidências",
                icon = Icons.Default.Analytics,
                onClick = onNavigateToTracking
            )
        }
    }

    if (showDialog && reportContent != null) {
        AttendanceReportDialog(
            content = reportContent!!,
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
/**
 * Dialog displaying the generated attendance report with a copy-to-clipboard option.
 *
 * @param content The text content of the report.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
fun AttendanceReportDialog(
    content: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listagem de Frequência") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(content))
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
