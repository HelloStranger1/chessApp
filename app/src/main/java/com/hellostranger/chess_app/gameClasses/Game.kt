package com.hellostranger.chess_app.gameClasses

import com.hellostranger.chess_app.gameClasses.enums.GameState

data class Game(
    val board : Board,
    var id: String,
    var isP1Turn : Boolean = true,
    var gameState: GameState,
    ){

    companion object {
        @Volatile
        private var instance: Game? = null

        fun getInstance(): Game? {
            return instance
        }

        fun setInstance(game: Game) {
            instance = game

        }
    }


}