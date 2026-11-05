package com.example.warrantymanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItem(id: Long): ItemEntity?

    @Insert suspend fun insertItem(item: ItemEntity): Long
    @Update suspend fun updateItem(item: ItemEntity)
    @Delete suspend fun deleteItem(item: ItemEntity)

    @Query("SELECT * FROM warranties WHERE itemId = :itemId ORDER BY endDate")
    fun observeWarranties(itemId: Long): Flow<List<WarrantyEntity>>

    @Query("SELECT * FROM warranties ORDER BY endDate")
    suspend fun allWarranties(): List<WarrantyEntity>

    @Insert suspend fun insertWarranty(warranty: WarrantyEntity): Long
    @Update suspend fun updateWarranty(warranty: WarrantyEntity)
    @Delete suspend fun deleteWarranty(warranty: WarrantyEntity)

    @Query("SELECT * FROM claims WHERE itemId = :itemId ORDER BY date DESC")
    fun observeClaims(itemId: Long): Flow<List<ClaimEntity>>

    @Insert suspend fun insertClaim(claim: ClaimEntity)
    @Query("SELECT COUNT(*) FROM items")
    fun observeItemCount(): Flow<Int>
}

@Database(entities = [ItemEntity::class, WarrantyEntity::class, ClaimEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): WarrantyDao
}
