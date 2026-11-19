package dev.kok.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.kok.app.data.local.entity.MealEntryEntity

@Dao
interface MealDao {
    @Insert
    suspend fun insert(entity: MealEntryEntity): Long

    @Insert
    suspend fun insertAll(entities: List<MealEntryEntity>): List<Long>

    @Update
    suspend fun update(entity: MealEntryEntity)

    @Query("SELECT * FROM meal_entry WHERE id = :id")
    suspend fun getById(id: Long): MealEntryEntity?

    @Query("DELETE FROM meal_entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM meal_entry WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<Long>)

    /** Son kullanılan yemek adları: manuel girişte hızlı öneri için. */
    @Query(
        """
        SELECT * FROM meal_entry WHERE id IN (
            SELECT MAX(id) FROM meal_entry GROUP BY name COLLATE NOCASE
        ) ORDER BY created_at DESC LIMIT :limit
        """,
    )
    suspend fun recentDistinct(limit: Int): List<MealEntryEntity>
}
