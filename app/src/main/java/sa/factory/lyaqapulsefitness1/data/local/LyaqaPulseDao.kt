package sa.factory.lyaqapulsefitness1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LyaqaPulseDao {
    @Query("SELECT * FROM fitness_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<LyaqaPulseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LyaqaPulseEntity>)

    @Query("DELETE FROM fitness_items_cache")
    suspend fun clearAll()
}
