package com.galeria.defensores.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DieProperty(
    var canCrit: Boolean = false,
    var isNegative: Boolean = false,
    var critRangeStart: Int = 6
) : Parcelable
