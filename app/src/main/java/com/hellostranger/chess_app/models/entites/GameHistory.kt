package com.hellostranger.chess_app.models.entites

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState

@Entity
data class GameHistory(
    @PrimaryKey(autoGenerate = false)
    val id : Int = 0,
    val opponentImage : String,
    val opponentName : String,
    val opponentElo : Int,
    val opponentColor : Color,
    val gameDate : String,
    val result : GameState,
    val boardsHistoryRepresentation : List<String> = ArrayList(),
    var isSaved : Boolean = false
)
