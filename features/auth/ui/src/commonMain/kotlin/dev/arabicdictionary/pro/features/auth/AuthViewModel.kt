package dev.arabicdictionary.pro.features.auth

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arabicdictionary.pro.common.features.ArabicDictNavGraph
import dev.arabicdictionary.pro.core.uikit.localization.ArabicDictLocalization
import dev.arabicdictionary.pro.navstate.NavCommand
import dev.arabicdictionary.pro.navstate.Navigator
import dev.arabicdictionary.pro.navstate.forward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal class StringProvider {
    @Composable
    fun get(id: StringResource): String {
        return stringResource(id)
    }
}

internal sealed interface AuthEvent {
    class OnNextClicked : AuthEvent

    data class OnUserAuthTextChanged(
        val text: String,
    ) : AuthEvent

    class OnRequestTokenClick : AuthEvent

    class OnSignUpClick : AuthEvent

    data class OnLanguageChange(
        val iso: String
    ): AuthEvent
}

internal enum class AuthError {
    INVALID_AUTH_TEXT,
    INVALID_ARABICDICT_DEV_TOKEN,
    EMAIL_AUTH_NOT_SUPPORTED,
    SERVER_ERROR,
    UNKNOWN,
}

internal sealed class AuthState {
    data class Default(
        val userAuthText: String,
        val error: AuthError?,
    ) : AuthState()

    data class Loading(
        internal val prevState: Default,
    ) : AuthState()

    companion object {
        fun default(): AuthState =
            Default(
                userAuthText = "",
                error = null,
            )
    }
}

internal class AuthViewModel(
    private val localization: ArabicDictLocalization,
    private val navigator: Navigator,
    private val validateUserAuthTextUseCaseProvider: () -> ValidateUserAuthTextUseCase,
    private val checkUserArabicdictTokenUseCaseProvider: () -> CheckUserArabicdictTokenUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthState.default())
    val state = _state.asStateFlow()
    private val _localizationState = MutableStateFlow(StringProvider())
    val localizationState = _localizationState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnNextClicked -> onNextClicked()
            is AuthEvent.OnRequestTokenClick -> {} //navigator.open(ARABICDICT_CREATE_TOKEN_URL)
            is AuthEvent.OnSignUpClick -> {} //navigator.open(ARABICDICT_REGISTER_URL)
            is AuthEvent.OnUserAuthTextChanged -> onUserAuthTextChanged(event.text)
            is AuthEvent.OnLanguageChange -> {
                localization.applyLanguage(event.iso)
                _localizationState.value = StringProvider()
            }
        }
    }

    private fun onUserAuthTextChanged(text: String) {
        val state = _state.value
        check(state is AuthState.Default)
        _state.value = state.copy(userAuthText = text)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun onNextClicked() {
        val curState = state.value
        check(curState is AuthState.Default)
        // Prefer not to use using KoinApp
        // KoinPlatform.getKoin().get<ValidateUserAuthTextUseCase>().invoke(curState.userAuthText)
        _state.value = AuthState.Loading(prevState = curState)
        viewModelScope.launch {
            try {
                val authText = curState.userAuthText
                when (validateUserAuthTextUseCaseProvider().invoke(authText)) {
                    ValidateUserAuthTextUseCase.Result.MAYBE_TOKEN -> checkToken(authText)
                    ValidateUserAuthTextUseCase.Result.EMPTY -> returnToDefaultState(AuthError.INVALID_AUTH_TEXT)
                    ValidateUserAuthTextUseCase.Result.EMAIL -> returnToDefaultState(AuthError.EMAIL_AUTH_NOT_SUPPORTED)
                    ValidateUserAuthTextUseCase.Result.UNKNOWN -> returnToDefaultState(AuthError.UNKNOWN)
                }
            } catch (e: Exception) {
                returnToDefaultState(AuthError.UNKNOWN)
                throw e
            }
        }
    }

    private fun returnToDefaultState(error: AuthError?) {
        val curState = _state.value
        // TODO Always must be Loading state
        check(curState is AuthState.Loading)
        _state.value = curState.prevState.copy(error = error)
    }

    private suspend fun checkToken(frameDevToken: String) {
        when (checkUserArabicdictTokenUseCaseProvider().invoke(frameDevToken)) {
            CheckUserArabicdictTokenUseCase.Result.VALID -> {
                // TODO Start auth user session
                // TODO Save dev token
                navigator.enqueue(NavCommand.forward(ArabicDictNavGraph.Alias.Main))
                return
            }

            CheckUserArabicdictTokenUseCase.Result.INVALID -> returnToDefaultState(AuthError.INVALID_ARABICDICT_DEV_TOKEN)
            CheckUserArabicdictTokenUseCase.Result.SERVER_ERROR -> returnToDefaultState(AuthError.SERVER_ERROR)
            CheckUserArabicdictTokenUseCase.Result.UNKNOWN_ERROR -> returnToDefaultState(AuthError.UNKNOWN)
        }
    }

    private companion object {
        const val ARABICDICT_REGISTER_URL = "https://accounts.frame.io/welcome"
        const val ARABICDICT_CREATE_TOKEN_URL = "https://developer.frame.io/app/tokens"
    }
}
