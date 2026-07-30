/**
 * ClassListScreen.kt
 *
 * Displays the list of classes grouped by academic year.
 * Provides authentication actions and backup/restore operations
 * through a dialog embedded in this screen.
 */

package edu.jm.tabulavia.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import edu.jm.tabulavia.model.AcademicClass
import edu.jm.tabulavia.utils.MessageHandler
import edu.jm.tabulavia.viewmodel.AuthViewModel
import edu.jm.tabulavia.viewmodel.ClassViewModel
import kotlinx.coroutines.launch
import edu.jm.tabulavia.BuildConfig
//import edu.jm.tabulavia.ui.theme.TabulaColorScheme
import java.io.InputStreamReader
import java.io.BufferedReader


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
        /**
         * Displays the main class list screen.
         *
         * @param viewModel ViewModel responsible for class data and backup/restore operations.
         * @param authViewModel ViewModel responsible for authentication state.
         * @param onAddClassClicked Callback triggered when add class is requested.
         * @param onClassClicked Callback triggered when a class is selected.
         * @param onLoginClicked Callback triggered when login is requested.
         * @param onLogoutClicked Callback triggered when logout is requested.
         */
fun ClassListScreen(
    viewModel: ClassViewModel,
    authViewModel: AuthViewModel,
    onAddClassClicked: () -> Unit,
    onClassClicked: (AcademicClass) -> Unit,
    onLoginClicked: () -> Unit,
    onLogoutClicked: () -> Unit
) {
    MessageHandler(viewModel)

    val classList by viewModel.classes.collectAsState()
    val groupedClasses = classList
        .groupBy { it.academicYear }
        .toSortedMap(compareByDescending { it.toIntOrNull() ?: 0 })

    val snackbarHostState = remember { SnackbarHostState() }
    val authenticatedUser by authViewModel.user.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var isBackupLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonContent by remember { mutableStateOf("") }
    var suggestedClassName by remember { mutableStateOf("") }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Turma") },
            text = {
                Column {
                    Text("Deseja importar esta turma? Você pode alterar o nome abaixo:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = suggestedClassName,
                        onValueChange = { suggestedClassName = it },
                        label = { Text("Class Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importClassBackup(importJsonContent, suggestedClassName)
                    showImportDialog = false
                }) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Launcher for Importing a Class
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val content = BufferedReader(InputStreamReader(inputStream)).readText()

                        // Try to extract original name for suggestion
                        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        val backup = json.decodeFromString(
                            edu.jm.tabulavia.model.ClassBackup.serializer(),
                            content
                        )

                        importJsonContent = content
                        suggestedClassName = "${backup.clazz.className} (Recuperado)"
                        showImportDialog = true
                    }
                } catch (e: Exception) {
                    viewModel.showMessage("Arquivo de backup inválido")
                    Log.e("json", "Erro ao importar backup", e)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TabulaTopBar(
                title = if (BuildConfig.FLAVOR == "dev") "Tabula Via (beta)" else "Tabula Via",
                viewModel = viewModel,
                actions = {
                    /**
                     * Displays authentication and backup actions.
                     */
                    if (authenticatedUser != null) {
                        IconButton(onClick = { showBackupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Cópia de segurança"
                            )
                        }
                        IconButton(onClick = onLogoutClicked) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sair"
                            )
                        }
                    } else {
                        IconButton(onClick = onLoginClicked) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Entrar"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClassClicked) {
                Icon(
                    Icons.Default.GroupAdd,
                    contentDescription = "Adicionar turma"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        /**
         * Displays the list of classes grouped by academic year.
         */
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            groupedClasses.forEach { (year, classesInYear) ->

                stickyHeader {
                    Text(
                        text = year,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(classesInYear) { clazz ->
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    ) {
                        ClassItem(
                            clazz = clazz,
                            onClick = { onClassClicked(clazz) }
                        )
                    }
                }
            }
        }
    }

    /**
     * Backup and restore dialog.
     */
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBackupLoading) showBackupDialog = false
            },
            title = { Text("Cópia de segurança") },
            text = {
                if (isBackupLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        /**
                         * Executes cloud backup operation.
                         */
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBackupLoading = true
                                    viewModel.backup()
                                    isBackupLoading = false
                                    showBackupDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.CloudUpload,
                                contentDescription = "Fazer cópia de segurança"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("BACKUP TO CLOUD")
                        }

                        /**
                         * Executes cloud restore operation.
                         */
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isBackupLoading = true
                                    viewModel.restore()
                                    isBackupLoading = false
                                    showBackupDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.CloudDownload,
                                contentDescription = "Restaurar cópia de segurança"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("RESTAURAR CÓPIA")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        /**
                         * Executes local file import operation.
                         */
                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                                showBackupDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Filled.FileUpload,
                                contentDescription = "Importar turma de arquivo"
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("IMPORTAR TURMA (.json)")
                        }

                        if (BuildConfig.FLAVOR == "dev") {
                            /**
                             * Clears the entire local database.
                             */
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isBackupLoading = true
                                        viewModel.clearDatabase()
                                        isBackupLoading = false
                                        showBackupDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Limpar base de dados"
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("CLEAR DATABASE")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showBackupDialog = false },
                    enabled = !isBackupLoading
                ) {
                    Text("Fechar")
                }
            }
        )
    }
}

/**
 * Displays a single class item inside a card.
 *
 * @param clazz The class to be displayed.
 * @param onClick Callback triggered when the item is selected.
 */
@Composable
fun ClassItem(
    clazz: AcademicClass,
    onClick: () -> Unit
) {
    TabulaCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Turma",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clazz.className,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${clazz.academicYear}/${clazz.period}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
