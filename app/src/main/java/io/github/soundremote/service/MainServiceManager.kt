package io.github.soundremote.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import io.github.soundremote.data.Hotkey
import io.github.soundremote.util.ConnectionStatus
import io.github.soundremote.util.Key
import io.github.soundremote.util.SystemMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MainServiceManager(
    dispatcher: CoroutineDispatcher,
) : ServiceManager {

    @Inject
    constructor() : this(Dispatchers.Default)

    private val scope = CoroutineScope(dispatcher)
    private var service: MainService? = null
    private var bound: Boolean = false
    private var stateCollect: Job? = null
    private var messageCollect: Job? = null
    private var _serviceState = MutableStateFlow(ServiceState(ConnectionStatus.DISCONNECTED, false))
    override val serviceState: StateFlow<ServiceState>
        get() = _serviceState

    private val _systemMessages: Channel<SystemMessage> = Channel(5, BufferOverflow.DROP_OLDEST)
    override val systemMessages: ReceiveChannel<SystemMessage>
        get() = _systemMessages

    override fun bind(context: Context) {
        Intent(context, MainService::class.java).also { intent ->
            context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    override fun unbind(context: Context) {
        stopCollect()
        bound = false
        context.unbindService(serviceConnection)
    }

    override fun connect(address: String) {
        if (!bound) return
        service?.connect(address)
    }

    override fun disconnect() {
        if (!bound) return
        service?.disconnect()
    }

    override fun sendHotkey(hotkey: Hotkey) {
        if (!bound) return
        service?.sendHotkey(hotkey)
    }

    override fun sendKey(key: Key) {
        if (!bound) return
        service?.sendKey(key)
    }

    override fun setMuted(value: Boolean) {
        if (!bound) return
        service?.setMuted(value)
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as MainService.LocalBinder
            service = localBinder.getService()
            startCollect()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
            stopCollect()
        }
    }

    private fun startCollect() {
        service?.let { service ->
            stateCollect = scope.launch {
                combine(service.connectionStatus, service.isMuted) { connectionStatus, isMuted ->
                    ServiceState(connectionStatus, isMuted)
                }.collect { _serviceState.value = it }
            }
            messageCollect = scope.launch {
                while (isActive) {
                    val message = service.systemMessages.receive()
                    _systemMessages.send(message)
                }
            }
        }
    }

    private fun stopCollect() {
        stateCollect?.cancel()
        stateCollect = null
        messageCollect?.cancel()
        messageCollect = null
    }
}