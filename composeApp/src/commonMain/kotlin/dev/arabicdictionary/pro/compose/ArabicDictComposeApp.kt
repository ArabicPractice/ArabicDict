package dev.arabicdictionary.pro.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.arabicdictionary.pro.common.features.ArabicDictNavGraph
import dev.arabicdictionary.pro.core.utils.UrlLauncher
import dev.arabicdictionary.pro.features.auth.AuthPane
import dev.arabicdictionary.pro.navstate.NavCommand
import dev.arabicdictionary.pro.navstate.NavDest
import dev.arabicdictionary.pro.navstate.NavEntry
import dev.arabicdictionary.pro.navstate.NavHost
import dev.arabicdictionary.pro.navstate.NavInterceptor
import dev.arabicdictionary.pro.navstate.NavState
import dev.arabicdictionary.pro.navstate.NavStructure
import dev.arabicdictionary.pro.navstate.Navigator
import dev.arabicdictionary.pro.navstate.buildNavState
import dev.arabicdictionary.pro.navstate.deeplink.SimpleUriPattern
import dev.arabicdictionary.pro.navstate.rememberNavTopEntry
import dev.arabicdictionary.pro.navstate.stack.NavStack
import dev.arabicdictionary.pro.core.uikit.ArabicDictTheme
import dev.arabicdictionary.pro.core.uikit.localization.LocalizationChanger
import dev.arabicdictionary.pro.core.uikit.localization.PlatformContext
import org.koin.compose.KoinContext
import org.koin.compose.LocalKoinApplication
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.scope.KoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.core.module.Module as KoinModule

@OptIn(KoinExperimentalAPI::class)
@Composable
@Suppress("ModifierMissing")
fun ArabicDictComposeApp(
    platformContext: PlatformContext,
    onRootBack: () -> Unit
) {
    KoinContext {
        KoinScope(LOCALIZATION_SCOPE_ID, localizationKoinScopeQualifier) {
            rememberKoinModules {
                listOf(localizationKoinModule())
            }
            val viewModel: LocalizationViewModel = koinViewModel<LocalizationViewModel>()
            val state = viewModel.state.collectAsState().value

            ArabicDictTheme(
                localization = state,
                platformContext = platformContext
            ) {
                Scaffold(
                    containerColor = ArabicDictTheme.colors.surfaceDim,
                    modifier = Modifier.fillMaxSize(),
                ) { contentPaddings ->
                    NavHost(
                        navigator = createNavigator(),
                        onRootBack = onRootBack,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(contentPaddings)
                                    .padding(16.dp),
                        ) {
                            val navTopEntry = rememberNavTopEntry()
                            AnimatedContent(navTopEntry) { navEntry ->
                                println("Переход на экран: $navEntry")
                                when (navEntry.destination as ArabicDictNavGraph.Dest) {
                                    is ArabicDictNavGraph.Dest.AssetNavigator -> StubScreen("Asset Navigator")
                                    is ArabicDictNavGraph.Dest.Auth -> AuthPane(onChangeLanguage = {
                                        viewModel.update(it)
                                        LocalizationChanger.update(it)
                                    })
                                    is ArabicDictNavGraph.Dest.ProjectNavigator -> {
                                        StubScreen("Project Navigator")
                                    }
                                    is ArabicDictNavGraph.Alias -> error("Impossible to open alias")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StubScreen(title: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            title,
            style = ArabicDictTheme.typography.headlineLarge,
            color = ArabicDictTheme.colors.onSurface,
        )
    }
}

private fun koinModule(navigator: Navigator): KoinModule =
    module {
        factory { navigator }
    }

@OptIn(KoinExperimentalAPI::class)
@Composable
private fun createNavigator(): Navigator {
    val koinApp = LocalKoinApplication.current
    val urlLauncherProvider: () -> UrlLauncher = { koinApp.get() }
    val initialState =
        buildNavState {
            add(
                NavStack(
                    id = NavStructure.Id("default"),
                    entry = NavEntry(ArabicDictNavGraph.Alias.Root),
                ),
                setCurrent = true,
            )
        }
    return Navigator(
        initialState = initialState,
        interceptors =
            listOf(
                NavGraphAliasInterceptor(
                    alias = ArabicDictNavGraph.Alias.Main,
                    dest = { ArabicDictNavGraph.Dest.ProjectNavigator() },
                ),
                NavGraphAliasInterceptor(
                    alias = ArabicDictNavGraph.Alias.Root,
                    dest = { ArabicDictNavGraph.Dest.Auth() },
                ),
                NavGraphAliasInterceptor(
                    alias = ArabicDictNavGraph.Alias.Authorization,
                    dest = { ArabicDictNavGraph.Dest.Auth() },
                ),
            ),
    ).apply {
        registerDeepLink(
            matcher =
                SimpleUriPattern(
                    schemeRegex = "https?",
                    hostRegex = ".+\\.frame\\.io",
                    pathRegex = ".*",
                ),
        ) { _, uri, _ ->
            urlLauncherProvider().openUrl(uri)
        }
    }.also { navigator ->
        rememberKoinModules { listOf(koinModule(navigator)) }
    }
}

private class NavGraphAliasInterceptor(
    private val alias: ArabicDictNavGraph.Alias,
    private val dest: suspend () -> NavDest,
) : NavInterceptor {
    override suspend fun intercept(
        navCommand: NavCommand,
        oldState: NavState,
        newState: NavState,
    ): NavState {
        if (newState.current.current.destination == alias) {
            return newState.buildNavState {
                add(
                    newState.current.copy(
                        entries = listOf(NavEntry(destination = dest())),
                    ),
                )
            }
        }

        return newState
    }
}
