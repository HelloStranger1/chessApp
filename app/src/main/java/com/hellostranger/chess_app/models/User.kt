package com.hellostranger.chess_app.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDate


@Parcelize
data class User(
    val id: Int = 0,
    val name : String = "",
    val email : String = "",
    val elo : Int = 0,
    val image : String = "",
    val accountCreation : String,
    val totalGames : Int = 0,
    val gamesWon : Int = 0,
    val gamesLost : Int = 0,
    val gamesDrawn : Int = 0
) : Parcelable

