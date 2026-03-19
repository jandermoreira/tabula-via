/**
 * BaseViewModel provides common functionality for view models.
 *
 * It includes message handling capabilities using a shared flow.
 * Derived view models can leverage the provided functionality to display textual messages.
 *
 * Inherits from the Android Architecture Components ViewModel.
 */

package edu.jm.tabulavia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow: SharedFlow<String> = _messageFlow

    protected fun showMessage(text: String) {
        viewModelScope.launch {
            _messageFlow.emit(text)
        }
    }
}