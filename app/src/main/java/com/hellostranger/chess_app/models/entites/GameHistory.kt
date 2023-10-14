package com.hellostranger.chess_app.models.entites

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState

@Entity
data class GameHistory(
    @PrimaryKey(autoGenerate = false)
    val id : Int = 0,
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
    var isSaved : Boolean = false
)
  