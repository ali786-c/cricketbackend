package com.devwithguru.cricket.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.devwithguru.cricket.ui.navigation.Screen

class NavigationViewModel : ViewModel() {
    val navigationStack = mutableStateListOf<Screen>(Screen.Home)

    val currentScreen: Screen
        get() = navigationStack.lastOrNull() ?: Screen.Home

    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
        }
    }

    fun clearAndNavigateTo(screen: Screen) {
        navigationStack.clear()
        navigationStack.add(screen)
    }

    fun updateCurrentScreen(oldScreen: Screen, newScreen: Screen) {
        val index = navigationStack.indexOf(oldScreen)
        if (index != -1) {
            navigationStack[index] = newScreen
        }
    }

    fun removeTossAndLineup() {
        navigationStack.removeAll { screen ->
            screen is Screen.Toss || screen is Screen.TossLineup
        }
    }
}
