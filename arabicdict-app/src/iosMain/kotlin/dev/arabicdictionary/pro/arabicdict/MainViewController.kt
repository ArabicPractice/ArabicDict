package dev.arabicdictionary.pro.arabicdict

import androidx.compose.ui.window.ComposeUIViewController
import dev.arabicdictionary.pro.arabicdict.common.app.initKoinApp
import dev.arabicdictionary.pro.compose.ArabicDictComposeApp
import dev.arabicdictionary.pro.core.utils.PlatformContext
import org.koin.core.context.startKoin
import platform.Foundation.NSProcessInfo
import dev.arabicdictionary.pro.core.uikit.localization.PlatformContext as locale


private fun isDebugBuild(): Boolean = NSProcessInfo.processInfo.environment["DEBUG"] == "1"

@Suppress("unused", "ktlint:standard:function-naming", "FunctionName")
fun MainViewController() =
    ComposeUIViewController(
        configure = {
            startKoin {
                initKoinApp(
                    platformContext = PlatformContext.INSTANCE,
                    isDebug = isDebugBuild(),
                )
            }
        },
        content = {
            ArabicDictComposeApp(
                platformContext = locale.INSTANCE,
                onRootBack = { }
            )
        },
    )
