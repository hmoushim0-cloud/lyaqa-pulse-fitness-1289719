package sa.factory.lyaqapulsefitness1.domain.usecase

import sa.factory.lyaqapulsefitness1.domain.repository.LyaqaPulseRepository
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseOrder
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: LyaqaPulseRepository) {
    operator fun invoke(): Flow<List<LyaqaPulseItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: LyaqaPulseRepository) {
    val cart = repository.activeCart
    fun add(item: LyaqaPulseItem) = repository.addToCart(item)
    fun remove(item: LyaqaPulseItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: LyaqaPulseRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): LyaqaPulseOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: LyaqaPulseRepository) {
    operator fun invoke(): Flow<LyaqaPulseTelemetry> = repository.telemetryFlow
}
