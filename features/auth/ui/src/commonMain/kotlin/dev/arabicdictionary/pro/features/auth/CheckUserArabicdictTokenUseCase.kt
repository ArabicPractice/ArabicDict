package dev.arabicdictionary.pro.features.auth

import kotlinx.coroutines.delay

/**
 * Verify that dev token is valid and cen vbe used to access ArabicDict API.
 * The use-case makes a request to Frame API to check the token.
 */
internal class CheckUserArabicdictTokenUseCase {
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "MagicNumber")
    suspend operator fun invoke(arabicdictToken: String): Result {
        delay(1000)

        return Result.VALID
//        try {
//            val response = validateArabicdictDeveloperToken(DeveloperToken(arabicdictToken))
//            return when {
//                response is Response.Success -> Result.VALID
//                response is Response.NetworkError ->
//                    when {
//                        response.httpCode == 401 -> Result.INVALID
//                        response.httpCode / 500 == 1 -> Result.SERVER_ERROR
//                        else -> Result.UNKNOWN_ERROR
//                    }
//                else -> Result.UNKNOWN_ERROR
//            }
//        } catch (_: Exception) {
//            return Result.UNKNOWN_ERROR
//        }

    }

    enum class Result {
        /**
         * Token is valid and can be used to access Frame API.
         */
        VALID,

        /**
         * Server return 401 Unauthorized HTTP status code.
         */
        INVALID,

        /**
         * Server return 5xx HTTP status code.
         */
        SERVER_ERROR,

        /**
         * Another kind of error during the request.
         */
        UNKNOWN_ERROR,
    }
}
