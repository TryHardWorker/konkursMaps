package com.mandrykevich.myhelper.data.repository

data class Comment(
    val buildingId: String = "",
    val userId: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val hasDisabledParking: Boolean = false,
    val hasElevator: Boolean = false,
    val hasHelper: Boolean = false,
    @com.google.firebase.database.Exclude
    var id: String? = null
)