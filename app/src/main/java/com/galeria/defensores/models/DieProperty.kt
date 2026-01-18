package com.galeria.defensores.models

import com.google.firebase.firestore.PropertyName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DieProperty(
    @get:PropertyName("canCrit") var canCrit: Boolean = false,
    @get:PropertyName("isNegative") var isNegative: Boolean = false,
    @get:PropertyName("critRangeStart") var critRangeStart: Int = 6
) : Parcelable
