package com.redhawk.code.ui.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Basit rota yığını. "home" her zaman dipte kalır.
 */
class NavStack(initial: String = "home") {
    private val stack: SnapshotStateList<String> = mutableStateListOf(initial)

    val current: String get() = stack.lastOrNull() ?: "home"
    val canGoBack: Boolean get() = stack.size > 1
    val size: Int get() = stack.size

    fun push(route: String) {
        if (stack.lastOrNull() == route) return
        stack.add(route)
    }

    /** true döner: geri yapıldı. false: daha geri yok (home'dayız). */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.size - 1)
        return true
    }

    /** Drawer'dan geçiş: home'u koruyarak hedefe git */
    fun navigate(route: String) {
        if (route == "home") {
            while (stack.size > 1) stack.removeAt(stack.size - 1)
            return
        }
        // Stack'i [home, route] yap
        while (stack.size > 1) stack.removeAt(stack.size - 1)
        if (stack.isEmpty()) stack.add("home")
        if (stack.last() != route) stack.add(route)
    }

    /** Her şeyi sıfırla, sadece route kalsın */
    fun reset(route: String = "home") {
        stack.clear()
        stack.add(route)
    }
}
