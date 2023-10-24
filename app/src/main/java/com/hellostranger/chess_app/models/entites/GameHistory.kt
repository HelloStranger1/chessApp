package com.hellostranger.chess_app.models.entites

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState

@Entity
data class GameHistory @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    val localId : Int,
    val id : Int,
    val whiteImage : String,
    val blackImage : String,
    val whiteName : String,
    val blackName : String,
    val blackElo : Int,
    val whiteElo : Int,
    val opponentColor : Color,
    val gameDate : String,
    val result : GameState,
    val startBoardJson : String,
    val gameMoves : String,
    @Ignore
    var isSaved : Boolean = false
)
  