package com.hellostranger.chess_app.models.entites

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


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

