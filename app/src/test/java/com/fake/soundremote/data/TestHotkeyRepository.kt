package com.fake.soundremote.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class TestHotkeyRepository : HotkeyRepository {
    private val _hotkeysFlow: MutableSharedFlow<List<Hotkey>> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val currentHotkeys get() = _hotkeysFlow.replayCache.firstOrNull() ?: emptyList()
    private val id = AtomicLong(1)

    init {
        // Set the initial value
        _hotkeysFlow.tryEmit(emptyList())
    }

    override suspend fun getById(id: Int): Hotkey? {
        return currentHotkeys.find { it.id == id }?.copy()
    }

    override suspend fun insert(hotkey: Hotkey): Long {
        val newId = id.getAndIncrement()
        val newHotkey = hotkey.copy(id = newId.toInt())
        _hotkeysFlow.emit(currentHotkeys + newHotkey)
        return newId
    }

    override suspend fun update(hotkey: Hotkey): Int {
        val indexToUpdate = currentHotkeys.indexOfFirst { it.id == hotkey.id }
        if (indexToUpdate == -1) return 0
        val updatedList = currentHotkeys.toMutableList()
        updatedList[indexToUpdate] = hotkey
        _hotkeysFlow.tryEmit(updatedList)
        return 1
    }

    override suspend fun deleteById(id: Int) {
        val hotkeys = currentHotkeys.toMutableList()
        hotkeys.removeIf { it.id == id }
        _hotkeysFlow.tryEmit(hotkeys)
    }

    override suspend fun changeFavoured(id: Int, favoured: Boolean) {
        val hotkeys = currentHotkeys
        hotkeys.find { it.id == id }!!.isFavoured = favoured
        _hotkeysFlow.tryEmit(hotkeys)
    }

    override fun getFavouredOrdered(favoured: Boolean): Flow<List<HotkeyInfo>> {
        return _hotkeysFlow.map { hotkeys ->
            hotkeys
                .filter { it.isFavoured == favoured }
                .sortedByDescending { it.order }
                .map { HotkeyInfo(it.id, it.keyCode, it.mods, it.name) }
        }
    }

    override fun getAllOrdered(): Flow<List<Hotkey>> {
        return _hotkeysFlow.map { hotkeys ->
            hotkeys.sortedByDescending { it.order }
        }
    }

    override fun getAllInfoOrdered(): Flow<List<HotkeyInfo>> {
        return _hotkeysFlow.map { hotkeys ->
            hotkeys
                .sortedByDescending { it.order }
                .map { HotkeyInfo(it.id, it.keyCode, it.mods, it.name) }
        }
    }

    override suspend fun updateOrders(hotkeyOrders: List<HotkeyOrder>) {
        val hotkeys = currentHotkeys
        for ((id, order) in hotkeyOrders) {
            hotkeys.find { it.id == id }!!.order = order
        }
        _hotkeysFlow.tryEmit(hotkeys)
    }

    // Test methods
    fun setHotkeys(hotkeys: List<Hotkey>) {
        _hotkeysFlow.tryEmit(hotkeys)
    }
}
