package com.test.testing.discord.ui.settings

import android.os.Parcelable
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.models.Guild
import com.test.testing.discord.models.User
import com.test.testing.discord.ui.UiAction
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Immutable UI state for the user/settings screen following MVI pattern
 */
sealed class UserScreenState : Parcelable {
    abstract val isLoading: Boolean

    @Parcelize
    object Loading : UserScreenState() {
        @IgnoredOnParcel
        override val isLoading: Boolean = true
    }

    @Parcelize
    data class Content(
        val currentUser: User? = null,
        val guilds: List<Guild> = emptyList(),
        val lastUpdated: Long = System.currentTimeMillis(),
    ) : UserScreenState() {
        @IgnoredOnParcel
        override val isLoading: Boolean = false
    }

    @Parcelize
    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.UNKNOWN,
        val canRetry: Boolean = false,
        val actions: List<UiAction> = emptyList(),
    ) : UserScreenState() {
        @IgnoredOnParcel
        override val isLoading: Boolean = false

        @IgnoredOnParcel
        val shouldShowRetryButton: Boolean = canRetry && actions.contains(UiAction.Retry)
    }
}

/**
 * Intents representing user actions for the user/settings screen
 */
sealed class UserIntent {
    object LoadData : UserIntent()

    data class UpdateUser(
        val user: User,
    ) : UserIntent()

    object DeleteUserData : UserIntent()
}

/**
 * Effects representing side effects that need UI handling
 */
sealed class UserEffect {
    data class ShowSnackbar(
        val message: String,
    ) : UserEffect()

    object NavigateToLogin : UserEffect()

    data class ShowConfirmationDialog(
        val title: String,
        val message: String,
    ) : UserEffect()
}
