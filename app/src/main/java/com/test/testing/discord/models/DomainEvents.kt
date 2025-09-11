package com.test.testing.discord.models

// Domain events for decoupling layers
sealed class DomainEvent {
    data class UserDataUpdated(
        val user: User,
    ) : DomainEvent()

    data class UserLocationUpdated(
        val userId: String,
        val location: Location,
    ) : DomainEvent()

    data class AuthenticationFailed(
        val reason: String,
    ) : DomainEvent()

    data class NetworkError(
        val operation: String,
        val error: Exception,
    ) : DomainEvent()

    data class DataRefreshCompleted(
        val success: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
    ) : DomainEvent()

    object UserLoggedOut : DomainEvent()

    object DataCleared : DomainEvent()
}

// Event bus for publishing and subscribing to domain events
interface DomainEventBus {
    fun publish(event: DomainEvent)

    fun subscribe(subscriber: DomainEventSubscriber)

    fun unsubscribe(subscriber: DomainEventSubscriber)
}

interface DomainEventSubscriber {
    fun onEvent(event: DomainEvent)
}

// Simple in-memory event bus implementation
class SimpleEventBus : DomainEventBus {
    private val subscribers = mutableSetOf<DomainEventSubscriber>()

    override fun publish(event: DomainEvent) {
        subscribers.forEach { subscriber ->
            try {
                subscriber.onEvent(event)
            } catch (e: Exception) {
                // Log error but don't crash the publisher
                println("Error delivering event to subscriber: ${e.message}")
            }
        }
    }

    override fun subscribe(subscriber: DomainEventSubscriber) {
        subscribers.add(subscriber)
    }

    override fun unsubscribe(subscriber: DomainEventSubscriber) {
        subscribers.remove(subscriber)
    }
}
