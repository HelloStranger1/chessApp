package com.hellostranger.chess_app.core

import com.hellostranger.chess_app.core.helpers.FenUtility

data class Game(
    var id : String,
    var gameResult: GameResult,
    var boardsFen : List<String> = mutableListOf(FenUtility.START_POSITION_FEN),
    var isDrawOfferedByWhite : Boolean = false,
    var isDrawOfferedByBlack : Boolean = false
) {
    companion object {
        @Volatile
        private var instance : Game? = null
        fun getInstance() : Game?{
            return instance
        }
        fun setInstance(game : Game) {
            instance = game
        }
    }

}
