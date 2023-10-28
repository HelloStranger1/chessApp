package com.hellostranger.chess_app.models.gameModels

import com.hellostranger.chess_app.models.gameModels.enums.GameState

data class Game(
    /*
        var currentMove : Int = 0, //Represents the current half move that we are showing to the user. if it is -1, we are showing the temporary board
    */
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