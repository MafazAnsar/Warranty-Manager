package com.example.warrantymanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String = "",
    val model: String = "",
    val category: String = "Other",
    val serialNumber: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val currency: String = "LKR",
    val supplier: String = "",
    val supplierContact: String = "",
    val notes: String = "",
    val photoUris: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "warranties",
    foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("itemId")]
)
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val type: String = "Manufacturer warranty",
    val provider: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val terms: String = "",
    val reminderDays: String = "90,30,7",
    val documentUris: String = "",
    val active: Boolean = true
)

@Entity(
    tableName = "claims",
    foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("itemId")]
)
data class ClaimEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val date: String,
    val description: String,
    val outcome: String = "",
    val cost: String = ""
)
