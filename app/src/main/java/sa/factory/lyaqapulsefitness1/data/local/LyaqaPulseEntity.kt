package sa.factory.lyaqapulsefitness1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.lyaqapulsefitness1.model.LyaqaPulseItem

@Entity(tableName = "fitness_items_cache")
data class LyaqaPulseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): LyaqaPulseItem = LyaqaPulseItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
