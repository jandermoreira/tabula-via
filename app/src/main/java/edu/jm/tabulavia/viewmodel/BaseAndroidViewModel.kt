/**
 * BaseAndroidViewModel serves as an abstraction for Android-specific view models
 * that require message handling capabilities through a shared flow mechanism.
 *
 * This class extends AndroidViewModel to provide application context access
 * and includes utility functions for message emission, enabling derived view models
 * to display textual messages that can be observed by the UI layer.
 */

package edu.jm.tabulavia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

abstract class BaseAndroidViewModel(application: Application) : AndroidViewModel(application) {
    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow: SharedFlow<String> = _messageFlow

    fun showMessage(text: String) {
        viewModelScope.launch {
            _messageFlow.emit(text)
        }
    }
}