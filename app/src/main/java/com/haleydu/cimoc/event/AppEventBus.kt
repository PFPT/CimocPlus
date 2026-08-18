package com.haleydu.cimoc.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    @JvmStatic
    fun post(event: AppEvent) {
        _events.tryEmit(event)
    }

    @JvmStatic
    fun observe(type: Int): Flow<AppEvent> = events.filter { it.type == type }
}
