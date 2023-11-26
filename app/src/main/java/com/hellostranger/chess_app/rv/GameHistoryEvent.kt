package com.hellostranger.chess_app.rv

import com.hellostranger.chess_app.models.rvEntities.GameHistory

sealed interface GameHistoryEvent{
    data class SaveGame(val gameHistory: GameHistory) : GameHistoryEvent
    data class OpenGame(val gameHistory: GameHistory) : GameHistoryEvent
    data class DeleteGame(val gameHistory: GameHistory) : GameHistoryEvent
}