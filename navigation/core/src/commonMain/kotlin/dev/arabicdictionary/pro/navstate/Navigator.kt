package dev.arabicdictionary.pro.navstate

import dev.arabicdictionary.pro.navstate.deeplink.UriMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlin.coroutines.CoroutineContext

public class Navigator(
    initialState: NavState,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate,
    interceptors: List<NavInterceptor> = emptyList(),
) : AutoCloseable {
    init {
        validate(initialState)
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val deepLinks = mutableMapOf<UriMatcher, DeepLinkHandler>()
    private val _stateFlow = MutableStateFlow(NavState.Empty)
    private var interceptors: List<NavInterceptor> = interceptors.toList()
    private val commandFlow = MutableSharedFlow<NavCommand>(extraBufferCapacity = Int.MAX_VALUE)

    init {
        commandFlow
            .map(::execute)
            .onEach(_stateFlow::emit)
            .launchIn(coroutineScope)

        replaceState { _ -> initialState }
    }

    public val stateFlow: StateFlow<NavState>
        get() = _stateFlow.asStateFlow()

    public val currentState: NavState
        get() = _stateFlow.value

    public fun registerDeepLink(
        matcher: UriMatcher,
        handler: DeepLinkHandler,
    ) {
        deepLinks[matcher] = handler
    }

    private suspend fun execute(command: NavCommand): NavState {
        val curState = currentState
        val transformedState = command.transform(curState).also(::validate)
        val (_, newState) =
            interceptors.fold(curState to transformedState) { (prevState, nextState), interceptor ->
                nextState to interceptor.intercept(command, prevState, nextState)
            }
        return newState.also(::validate)
    }

    public fun open(uri: String) {
        deepLinks.entries
            .asFlow()
            .mapNotNull { (pattern, handler) -> pattern.match(uri)?.let { result -> handler to result } }
            .takeWhile { (handler, result) -> handler.handle(this@Navigator, uri, result) }
            .launchIn(coroutineScope)
    }

    public fun enqueue(command: NavCommand) {
        commandFlow.tryEmit(command)
    }

    public fun addInterceptor(interceptor: NavInterceptor) {
        this.interceptors += interceptor
    }

    override fun close() {
        coroutineScope.cancel()
    }

    public fun interface DeepLinkHandler {
        public suspend fun handle(
            navigator: Navigator,
            uri: String,
            result: UriMatcher.MatchResult,
        ): Boolean
    }

    public interface StateTransformDeepLinkHandler : DeepLinkHandler {
        public override suspend fun handle(
            navigator: Navigator,
            uri: String,
            result: UriMatcher.MatchResult,
        ): Boolean {
            navigator.replaceState { curState -> transformState(curState) }
            return true
        }

        public suspend fun transformState(curState: NavState): NavState
    }
}

/**
 * Check [NavState] that it's valid and can be represented navigation state
 *
 * Conditions
 * - Has at least 1 stack
 * - Each stack contains at least 1 stack
 * - Active stack must be in NavState stacks
 */
private fun validate(navState: NavState) {
    check(navState.structures.isNotEmpty()) { "NavState must have at least 1 struct" }
    check(navState.current in navState.structures) { "Active struct out of structs" }
    navState.structures.forEach(NavStructure::validate)
}

public inline fun Navigator.replaceState(crossinline transform: suspend (curState: NavState) -> NavState) {
    enqueue { curState -> transform(curState) }
}
