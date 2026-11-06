package sa.factory.lyaqapulsefitness1.domain.repository

import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseOrder
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LyaqaPulseRepository {
    val catalogFlow: Flow<List<LyaqaPulseItem>>
    val activeCart: StateFlow<List<LyaqaPulseItem>>
    val telemetryFlow: Flow<LyaqaPulseTelemetry>
    
    fun addToCart(item: LyaqaPulseItem)
    fun removeFromCart(item: LyaqaPulseItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): LyaqaPulseOrder
    suspend fun refreshCatalog()
}
