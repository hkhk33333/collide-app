package com.test.testing.discord.ui.map

import android.os.Parcelable
import com.test.testing.discord.models.ErrorType
import com.test.testing.discord.ui.UiAction
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Immutable UI state for the map screen following MVI pattern
 */
sealed class MapScreenState : Parcelable {
    @Parcelize
    object Loading : MapScreenState()

    @Parcelize
    data class Content(
        val users: @RawValue List<com.test.testing.discord.domain.models.DomainUser> = emptyList(),
        val allUsers: @RawValue List<com.test.testing.discord.domain.models.DomainUser> = emptyList(),
        val selectedUserId: String? = null,
        val isLocationEnabled: Boolean = false,
        val isRefreshing: Boolean = false,
        val lastUpdated: Long = System.currentTimeMillis(),
    ) : MapScreenState()

    @Parcelize
    data class Error(
        val message: String,
        val errorType: ErrorType = ErrorType.UNKNOWN,
        val canRetry: Boolean = false,
        val actions: List<UiAction> = emptyList(),
        val isRefreshing: Boolean = false,
    ) : MapScreenState() {
        @IgnoredOnParcel
        val shouldShowRetryButton: Boolean = canRetry && actions.contains(UiAction.Retry)
    }
}

/**
 * Intents representing user actions for the map screen
 */
sealed class MapIntent {
    object LoadUsers : MapIntent()

    object RefreshUsers : MapIntent()

    data class UserSelected(
        val userId: String,
    ) : MapIntent()

    data class LocationPermissionChanged(
        val granted: Boolean,
    ) : MapIntent()
}

/**
 * Effects representing side effects that need UI handling
 */
sealed class MapEffect {
    data class ShowSnackbar(
        val message: String,
    ) : MapEffect()

    data class NavigateToUser(
        val userId: String,
    ) : MapEffect()

    object RequestLocationPermission : MapEffect()
}
