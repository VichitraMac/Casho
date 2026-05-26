package com.vichitra.casho

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@SuppressLint("ParcelCreator")
@Parcelize
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double
) : Parcelable
