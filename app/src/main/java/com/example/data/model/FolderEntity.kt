package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String = "folder",
    val colorHex: String = "#64748B",
    val orderIndex: Int = 0,
    val isProtected: Boolean = false
)
