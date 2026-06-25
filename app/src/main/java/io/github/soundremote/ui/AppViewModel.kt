package io.github.soundremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.soundremote.service.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppViewModel @Inject constructor(private val serviceRepository: ServiceRepository) :
    ViewModel() {

    private val _systemMessage = MutableStateFlow<Int?>(null)
    val systemMessage: StateFlow<Int?>
        get() = _systemMessage

    init {
        viewModelScope.launch {
            while (isActive) {
                val message = serviceRepository.systemMessages.receive()
                _systemMessage.value = message.stringId
            }
        }
    }

    fun bindService() {
        serviceRepository.bind()
    }

    fun unbindService() {
        serviceRepository.unbind()
    }

    fun messageShown() {
        _systemMessage.value = null
    }
}