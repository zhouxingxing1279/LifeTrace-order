package com.lifetrace.order.platform

import com.lifetrace.order.domain.PlatformId

class PlatformRegistry(adapters: List<PlatformAdapter>) {
    private val byId = adapters.associateBy { it.spec.id }

    fun get(platform: PlatformId): PlatformAdapter =
        requireNotNull(byId[platform]) { "No adapter registered for ${platform.wireValue}" }

    fun all(): List<PlatformAdapter> = byId.values.toList()

    companion object {
        fun createDefault(sessionStore: WebViewSessionStore): PlatformRegistry = PlatformRegistry(
            PlatformSpecs.all.map { WebOnlyPocAdapter(it, sessionStore) },
        )
    }
}
