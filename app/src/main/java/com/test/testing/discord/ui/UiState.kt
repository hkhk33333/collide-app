package com.test.testing.discord.ui

import android.os.Parcelable
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Result
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Common UI states for the application
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T,
    ) : UiState<T>()

    data class Error(
        val message: String,
        val canRetry: Boolean = false,
    ) : UiState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    companion object {
        fun <T> fromResult(result: Result<T>): UiState<T> =
            when (result) {
                is Result.Success -> {
                    Success(result.data)
                }

                is Result.Error -> {
                    Error(
                        message = result.exception.message ?: "Unknown error",
                        canRetry = result.canRetry,
                    )
                }
            }
    }
}

/**
 * Common actions for UI components
 */
sealed class UiAction : Parcelable {
    @Parcelize
    object Retry : UiAction()

    @Parcelize
    object Refresh : UiAction()

    @Parcelize
    data class Navigate(
        val destination: String,
    ) : UiAction()

    @Parcelize
    object Dismiss : UiAction()
}

/**
 * Enhanced error state with recovery actions
 */
@Parcelize
data class ErrorState(
    val message: String,
    val errorType: ErrorType = ErrorType.UNKNOWN,
    val canRetry: Boolean = false,
    val recoveryActions: List<UiAction> = emptyList(),
) : Parcelable {
    @IgnoredOnParcel
    val shouldShowRetry: Boolean = canRetry && recoveryActions.contains(UiAction.Retry)

    companion object {
        fun fromResultError(error: Result.Error): ErrorState {
            val recoveryActions = mutableListOf<UiAction>()
            if (error.canRetry) {
                recoveryActions.add(UiAction.Retry)
            }

            return ErrorState(
                message = error.exception.message ?: "An error occurred",
                errorType = error.errorType,
                canRetry = error.canRetry,
                recoveryActions = recoveryActions,
            )
        }
    }
}

/**
 * UI Events for ViewModel communication
 */
sealed class UiEvent {
    // Map-related events
    object RefreshUsers : UiEvent()

    object LoadUsers : UiEvent()

    // User-related events
    data class UpdateUser(
        val user: com.test.testing.discord.models.User,
    ) : UiEvent()

    object DeleteUserData : UiEvent()

    // Auth-related events
    object Login : UiEvent()

    object Logout : UiEvent()
}
