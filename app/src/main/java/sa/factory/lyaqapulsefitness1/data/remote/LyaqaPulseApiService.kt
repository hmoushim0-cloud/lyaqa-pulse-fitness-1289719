package sa.factory.lyaqapulsefitness1.data.remote

import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseOrder

interface LyaqaPulseApiService {
    suspend fun fetchCatalog(): List<LyaqaPulseItem>
    suspend fun submitOrder(order: LyaqaPulseOrder): Boolean
}
