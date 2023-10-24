package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.models.gameModels.Game

class BaseChessGame(
    protected val currentGame : Game,
    /*var isWhite : Boolean,
    protected val currentPlayerEmail: String*/

    ) /*: ChessGameInterface*/{
    private val TAG = "BaseChessGameClass"

    /*override fun pieceAt(col: Int, row: Int, isFlipped : Boolean): Piece? {
         if(isFlipped){
             return currentGame.getCurrentBoard()!!.squaresArray[7 - row][7 - col].piece
         }
         return currentGame.getCurrentBoard()!!.squaresArray[row][col].piece
    }*/

    /*
    Receives a move and performs a *very* simple validation of it. Server will check and validate the move.
     */
    /*abstract fun validateMove(moveMessage: MoveMessage) : Boolean*/
    /*override fun showPreviousBoard() : Boolean{
        Log.e(TAG, "Previous. Current: ${currentGame.currentMove}")
        return if(currentGame.currentMove > 0){
            currentGame.currentMove = currentGame.currentMove - 1
            true
        } else{
            false
        }
    }*/
    /*override fun showNextBoard() : Boolean{
        Log.e(TAG, "Previous. Current: ${currentGame.currentMove} is it last? ${currentGame.isCurrentMoveLast()}")
        if(!currentGame.isCurrentMoveLast()){
            currentGame.currentMove = currentGame.currentMove+1
            return true
        } else{
            Log.e(TAG, "Cant go forwards. current move is: ${currentGame.currentMove} and game history size is: ${currentGame.boards_history}")
            return false
        }
    }*/
    //Plays the move received from the server
    /*fun playMove(moveMessage: MoveMessage){
//        Log.e(TAG, "currentMoveDisplayed: ${currentGame.currentMove} last move: ${currentGame.boards_history!!.size - 1}")
        *//*if(!currentGame.isCurrentMoveLast()){
            //we arent on the latest move
            Log.e(TAG, "(PlayMove from server) We arent on the last move.")
            currentGame.goToLatestMove()
        }*//*
        *//*val updatedGame = currentGame.getCurrentBoard()!!.clone().movePiece(moveMessage)*//*
        if(updatedGame != null){
            if(!moveMessage.isSecondCastleMove){
                currentGame.boards_history!!.add(updatedGame)
                currentGame.currentMove = currentGame.currentMove + 1
            } else{
                currentGame.boards_history!![currentGame.currentMove] = updatedGame
            }

        }

        Log.e(TAG, "received move from socket. move is: $moveMessage. game History size is now: ${currentGame.boards_history!!.size - 1}")

        if(isWhite){
            currentGame.isP1turn = moveMessage.playerEmail != currentPlayerEmail
        }else{
            currentGame.isP1turn = moveMessage.playerEmail == currentPlayerEmail
        }

    }
    override fun isOnLastMove() : Boolean{
        return Game.getInstance()!!.isCurrentMoveLast()
    }

    override fun goToLastMove() {
        Game.getInstance()!!.goToLatestMove()
    }*/

    //TODO: This is only relevant to online and bot. extract from base
    //Undoing a move that the server has invalidated. Returns the move we should now show.
    /*fun undoMove() {
        val currentGame = Game.getInstance()!!
        currentGame.goToLatestMove()
        currentGame.temporaryBoard = null
    }*/

    //TODO: This is only relevant to online and bot. extract from base
    /*
    Used to temporarily make a move before knowing if it is a legal move.
    If the move is not legal, it will be undone. The time between sending a move and the server validating is short, but can cause lagging.
     */
    /*fun temporaryMakeMove(moveMessage: MoveMessage){
        val currentGame = Game.getInstance()!!
        if(!currentGame.isCurrentMoveLast()){
            //we arent on the latest move
            Log.e(TAG, "(temporaryMakeMove) We arent on the last move. current move show: ${currentGame.currentMove} and size is: ${currentGame.boards_history!!.size - 1}")
            currentGame.goToLatestMove()

        }
        currentGame.temporaryBoard = currentGame.getCurrentBoard()!!.clone()
        currentGame.currentMove = - 1
        val currentBoard = currentGame.getCurrentBoard()!!
        if(isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 0 &&
            currentGame.getCurrentBoard()!!.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.pieceType == PieceType.KING
        ){
            //White might be trying to castle. Checking for that.
            if(moveMessage.endCol == 7 && moveMessage.endRow == 0){ //Checking for short castle (O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 0, moveMessage.startCol+2, 0))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol+1, 0))
            }else if(moveMessage.endCol == 0 && moveMessage.endRow == 0){ //Checking for long castle (O-O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 0, moveMessage.startCol-2, 0))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol-1, 0))
            } else{
                currentBoard.movePiece(moveMessage)
            }
        }else if(!isWhite &&
            moveMessage.startCol == 4 && moveMessage.startRow == 7 &&
            currentBoard.squaresArray[moveMessage.startRow][moveMessage.startCol].piece!!.pieceType == PieceType.KING
        ){
            //Black might be trying to castle. Checking for that.
            if(moveMessage.endCol == 7 && moveMessage.endRow == 7){ //Checking for short castle (O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 7, moveMessage.startCol+2, 7))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 0, moveMessage.startCol+1, 7))
            }else if(moveMessage.endCol == 0 && moveMessage.endRow == 7){ //Checking for long castle (O-O-O)
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.startCol, 7, moveMessage.startCol-2, 7))
                currentBoard.movePiece(MoveMessage(moveMessage.playerEmail, moveMessage.endCol, 7, moveMessage.startCol-1, 7))
            } else{
                currentBoard.movePiece(moveMessage)
            }
        }else{
            currentBoard.movePiece(moveMessage)
        }
        Log.e(TAG, "Copied board to prev_board and played the move")

    }*/


//    fun convertFENStringToBoard(fenString : String) : Board{
//        //an fen start with each row, from top to bottom
//        var fen = fenString
//        var row = 0
//        val emptyRow0 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 1
//        val emptyRow1 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 2
//        val emptyRow2 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 3
//        val emptyRow3 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 4
//        val emptyRow4 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 5
//        val emptyRow5 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 6
//        val emptyRow6 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//        row = 7
//        val emptyRow7 = arrayListOf<Square>(
//            Square(row, null, 0), Square(row, null, 1), Square(row, null, 2), Square(row, null, 3),
//            Square(row, null, 4), Square(row, null, 5), Square(row, null, 6), Square(row, null, 7),
//        )
//
//        val squaresArray : List<List<Square>> = arrayListOf(emptyRow0, emptyRow1, emptyRow2, emptyRow3, emptyRow4, emptyRow5, emptyRow6, emptyRow7)
//
//        var col = 0
//        while(row >= 0){
//            while (col < 8){
//                val pieceAt = matchLetterToPiece(fen[0])
//                if(pieceAt == null && fen[0].isDigit()){
//                    //there is a number
//                    col += fen[0].digitToInt()
//                } else if(pieceAt == null){
//                    Log.e(TAG, " 1 convertFENStringToBoard i dont know. fen left: $fen")
//                } else{
//                    squaresArray[row][col].piece = pieceAt
//                    col++
//                }
//                fen = fen.substring(1)
//            }
//            if(fen[0] == '/'){
//                fen = fen.substring(1)
//                row++
//            }else{
//                Log.e(TAG, "2 convertFENStringToBoard i dont know. fen left: $fen")
//            }
//        }
//        return Board(squaresArray)
//    }
//
//    fun matchLetterToPiece(letter : Char) : Piece?{
//        return when(letter){
//            'k' ->{  Piece(Color.BLACK, false, PieceType.KING) }
//            'K' ->{  Piece(Color.WHITE, false, PieceType.KING) }
//
//            'q' ->{  Piece(Color.BLACK, false, PieceType.QUEEN) }
//            'Q' ->{  Piece(Color.WHITE, false, PieceType.QUEEN) }
//
//            'r' ->{  Piece(Color.BLACK, false, PieceType.ROOK) }
//            'R' ->{  Piece(Color.WHITE, false, PieceType.ROOK) }
//
//            'b' ->{  Piece(Color.BLACK, false, PieceType.BISHOP) }
//            'B' ->{  Piece(Color.WHITE, false, PieceType.BISHOP) }
//
//            'n' ->{  Piece(Color.BLACK, false, PieceType.KNIGHT) }
//            'N' ->{  Piece(Color.WHITE, false, PieceType.KNIGHT) }
//
//            'p' ->{  Piece(Color.BLACK, false, PieceType.PAWN) }
//            'P' ->{  Piece(Color.WHITE, false, PieceType.PAWN) }
//
//            else -> {null}
//        }
//    }

}