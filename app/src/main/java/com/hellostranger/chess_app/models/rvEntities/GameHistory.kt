package com.hellostranger.chess_app.models.rvEntities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.hellostranger.chess_app.core.GameResult

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
    val opponentColorIndex : Int,
    val gameDate : String,
    val result : GameResult,
    val startBoardFen : String,
    val gameMoves : String,
    @Ignore
    var isSaved : Boolean = false
)
  