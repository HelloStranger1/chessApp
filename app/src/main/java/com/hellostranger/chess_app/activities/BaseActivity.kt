package com.hellostranger.chess_app.activities

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.chess_models.Color
import com.hellostranger.chess_app.chess_models.Game
import com.hellostranger.chess_app.chess_models.PieceType

open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false


    private lateinit var mProgressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /*fun getCurrentGame() : Game?{
        Log.e("TAG", "Current game is: $currentGame")
        return currentGame
    }

    fun setCurrentGame(updatedGame : Game){
        currentGame = updatedGame
        Log.e("TAG", "Updated game to: $currentGame")
    }*/

    fun updatePiecesResId(){
        for(squaresRow in Game.getInstance()!!.board.squaresArray){
            for(square in squaresRow){
                if(square.piece != null){
                    val isWhite = square.piece!!.color == Color.WHITE
                    square.piece!!.resID = when(square.piece!!.pieceType){
                        PieceType.KING -> {
                            if(isWhite) R.drawable.ic_white_king else R.drawable.ic_black_king
                        }
                        PieceType.QUEEN -> {
                            if(isWhite) R.drawable.ic_white_queen else R.drawable.ic_black_queen
                        }
                        PieceType.ROOK -> {
                            if(isWhite) R.drawable.ic_white_rook else R.drawable.ic_black_rook
                        }
                        PieceType.BISHOP -> {
                            if(isWhite) R.drawable.ic_white_bishop else R.drawable.ic_black_bishop
                        }
                        PieceType.KNIGHT -> {
                            if(isWhite) R.drawable.ic_white_knight else R.drawable.ic_black_knight
                        }
                        PieceType.PAWN -> {
                            if(isWhite) R.drawable.ic_white_pawn else R.drawable.ic_black_pawn
                        }
                    }
                }
            }
        }
    }
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)


        mProgressDialog.setContentView(R.layout.dialog_progress)

        mProgressDialog.findViewById<TextView>(R.id.tv_progress_text).text = text

        mProgressDialog.show()
    }


    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }

    fun getCurrentUserID(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }

    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
            this,
            resources.getString(R.string.please_click_back_again_to_exit),
            Toast.LENGTH_SHORT
        ).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    fun showErrorSnackBar(message: String) {
        val snackBar =
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this@BaseActivity,
                R.color.snackbar_error_color
            )
        )
        snackBar.show()
    }
}