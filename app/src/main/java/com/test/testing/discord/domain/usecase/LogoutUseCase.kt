package com.test.testing.discord.domain.usecase

import com.test.testing.discord.auth.AuthManager
import javax.inject.Inject

/**
 * Use case for logging out the current user.
 * This abstracts the interaction with the authentication system away from the ViewModel.
 */
class LogoutUseCase
    @Inject
    constructor(
        private val authManager: AuthManager,
    ) {
        /**
         * Executes the logout logic.
         */
        fun execute(onComplete: () -> Unit) {
            // The business logic is simply to call the auth manager.
            // The ViewModel doesn't need to know this detail.
            authManager.logout(onComplete)
        }
    }
