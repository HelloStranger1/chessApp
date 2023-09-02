package com.hellostranger.chess_app

import com.hellostranger.chess_app.models.entites.GameHistory

sealed interface GameHistoryEvent{
    data class SaveGame(val gameHistory: GameHistory) : GameHistoryEvent
    data class OpenGame(val gameHistory: GameHistory) : GameHistoryEvent
    data class DeleteGame(val gameHistory: GameHistory) : GameHistoryEvent
}