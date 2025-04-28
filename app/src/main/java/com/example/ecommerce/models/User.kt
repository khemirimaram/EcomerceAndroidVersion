package com.example.ecommerce.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    var id: String = "",
    var username: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var phoneNumber: String? = null,
    var profileImageUrl: String? = null,
    var location: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var bio: String? = null,
    var isVerified: Boolean = false,
    var avatarId: Int = 0 // ID de l'avatar par défaut (0-5)
) : Parcelable 