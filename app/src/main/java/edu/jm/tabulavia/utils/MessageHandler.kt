/**
 * Composable function to collect and display messages from a BaseAndroidViewModel.
 * Place this at the root of any screen that needs to show user messages.
 */
package edu.jm.tabulavia.utils

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import edu.jm.tabulavia.viewmodel.BaseAndroidViewModel

/**
 * Collects messages from the ViewModel's messageFlow and displays them as Toasts.
 * @param viewModel The ViewModel that extends BaseAndroidViewModel
 */
@Composable
fun MessageHandler(viewModel: BaseAndroidViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.messageFlow.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}