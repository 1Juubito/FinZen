package com.finzen.app.notifications

import android.content.Context
import android.content.Intent
import com.finzen.app.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppNavBus {

    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route.asStateFlow()

    fun submit(route: String) {
        _route.value = route
    }

    fun consume(): String? {
        val current = _route.value
        _route.value = null
        return current
    }
}

object NavIntent {

    const val ACTION = "com.finzen.app.NAVIGATE"
    private const val EXTRA_ROUTE = "extra_nav_route"

    fun build(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION
            putExtra(EXTRA_ROUTE, route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    fun route(intent: Intent?): String? =
        if (intent?.action == ACTION) intent.getStringExtra(EXTRA_ROUTE) else null
}
