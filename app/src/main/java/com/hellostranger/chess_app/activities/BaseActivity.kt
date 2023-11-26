package com.hellostranger.chess_app.activities

import android.app.AlertDialog
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.gameClasses.enums.Color
import com.hellostranger.chess_app.gameClasses.Game
import com.hellostranger.chess_app.gameClasses.enums.PieceType

open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    private lateinit var mProgressDialog: Dialog

    fun showRationaleDialog(
        title: String,
        message: String,
    ) {

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

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