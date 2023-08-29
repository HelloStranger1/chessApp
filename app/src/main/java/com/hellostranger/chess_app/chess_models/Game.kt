package com.hellostranger.chess_app.chess_models

data class Game(
    var board: Board,
    val id: String,
    var isP1turn: Boolean,
    var gameState: GameState,
    var prev_board : Board? = null,
    var boards_history : MutableList<Board>? =null,

){

    companion object {
        @Volatile
        private var instance: Game? = null

        fun getInstance(): Game? {
            return instance
        }

        fun setInstance(game: Game) {
            if(game.boards_history == null){
                game.boards_history = ArrayList()
            }
            instance = game

        }
    }

}