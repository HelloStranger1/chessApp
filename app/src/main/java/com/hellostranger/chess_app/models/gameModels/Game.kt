package com.hellostranger.chess_app.models.gameModels

import com.google.gson.annotations.SerializedName
import com.hellostranger.chess_app.models.gameModels.enums.GameState

data class Game(
    var currentMove : Int = 0, //Represents the current half move that we are showing to the user. if it is -1, we are showing the temporary board
    val board : Board,
    val id: String,

    var gameState: GameState,
    /*var temporaryBoard : Board? = null,
    var boards_history : MutableList<Board>? =null,*/

    ){

    companion object {
        @Volatile
        private var instance: Game? = null

        fun getInstance(): Game? {
            return instance
        }

        fun setInstance(game: Game) {
            /*if(game.boards_history == null){
                game.boards_history = ArrayList()
            }*/
            instance = game

        }
    }

    /*fun getCurrentBoard() : Board? {
        return if(currentMove == -1){
            temporaryBoard
        } else if(currentMove == 0) {
            startingBoard
        } else{
            boards_history?.get(currentMove)
        }

    }*/

    /*
    Returns if the current move being shown is the latest move
     */
    /*fun isCurrentMoveLast() : Boolean{
        return currentMove == (boards_history!!.size - 1)
    }

    fun goToLatestMove() {
        currentMove = (boards_history!!.size - 1)
    }*/

}