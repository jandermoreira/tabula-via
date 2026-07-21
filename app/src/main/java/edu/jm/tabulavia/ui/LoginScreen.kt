/**
 * LoginScreen.kt
 *
 * Provides the user interface for mandatory Google Authentication.
 * This screen prevents access to the application until a valid session is established.
 */
package edu.jm.tabulavia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Renders the login screen with a Google Sign-In button.
 *
 * @param onGoogleSignInClick Callback to initiate the authentication flow.
 * @param errorMessage Optional error message to display in case of failure.
 */
@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tabula Via", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Bem-vindo", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        // Display error message if authentication fails
        if (!errorMessage.isNullOrEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar com o Google")
        }
    }
}
