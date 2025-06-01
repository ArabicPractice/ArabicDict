@file:OptIn(ExperimentalComposeUiApi::class)

package dev.arabicdictionary.pro.navstate


import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import dev.arabicdictionary.pro.navstate.stack.NavStack
import dev.arabicdictionary.pro.navstate.stack.popTop
import kotlinx.coroutines.Dispatchers

@Suppress("CompositionLocalAllowlist")
public val LocalNavigator: ProvidableCompositionLocal<Navigator> =
    compositionLocalOf { error("LocalNavigator must be called inside NavHost()") }

@Composable
public fun rememberNavState(): NavState {
    val navigator = LocalNavigator.current
    val state = navigator.stateFlow.collectAsState(Dispatchers.Main.immediate)
    val remState by remember { state }
    return remState
}

@Composable
public fun rememberNavTopEntry(): NavEntry {
    val navState = rememberNavState()
    return navState.current.entries.last()
}

@Composable
public fun rememberNavTopEntry(count: Int): List<NavEntry> {
    require(count > 0) { "count must be >= 1" }
    val navState = rememberNavState()
    val entries = navState.current.entries
    val startIndex = (entries.size - count).coerceAtLeast(0)
    return entries.subList(startIndex, entries.size)
}

@Composable
@NonRestartableComposable
public fun NavHost(
    initialDestination: NavDest,
    onRootBack: () -> Unit,
    initialStackId: NavStructure.Id = NavState.DefaultStructId,
    content: @Composable () -> Unit,
) {
    NavHost(
        initialState =
            buildNavState {
                val navStack = NavStack(id = initialStackId, entry = NavEntry(initialDestination))
                add(navStack)
                setCurrent(navStack.id)
            },
        onRootBack,
        content,
    )
}

@Composable
public fun NavHost(
    initialState: NavState,
    onRootBack: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    NavHost(
        navigator = Navigator(initialState),
        onRootBack = onRootBack,
        content = content,
    )
}

@Composable
public fun NavHost(
    navigator: Navigator,
    onRootBack: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    BackHandler(
        enabled = true,
        onBack = {
            if (navigator.currentState.current.entries.size > 1) {
                navigator.enqueue(NavCommand.popTop(count = 1))
            } else {
                onRootBack()
            }
        },
    )

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        content,
    )
}

@Composable
public fun AnimatedNavHost(
    navigator: Navigator,
    onRootBack: () -> Unit = {},
    content: @Composable (NavDest) -> Unit,
) {
    BackHandler(
        enabled = true,
        onBack = {
            if (navigator.currentState.current.entries.size > 1) {
                navigator.enqueue(NavCommand.popTop(count = 1))
            } else {
                onRootBack()
            }
        },
    )

    CompositionLocalProvider(
        LocalNavigator provides navigator,
    ) {
        val navState = rememberNavState()
        AnimatedContent(navState.current.current.destination) { dest ->
            content(dest)
        }
    }
}
