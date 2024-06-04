package com.hellostranger.chess_app.activities

import android.app.AlertDialog
import android.app.Dialog
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.hellostranger.chess_app.R
import retrofit2.Response

open class BaseActivity : AppCompatActivity() {
    private lateinit var mProgressDialog: Dialog // Holds the dialog



    /**
     * Shows a simple progress dialog.
     * @param text: The message to be displayed in the dialog
     * @param isCancelable: is the dialog cancelable.
     */
    fun showProgressDialog(text: String, isCancelable : Boolean = true) {
        mProgressDialog = Dialog(this)

        mProgressDialog.setContentView(R.layout.dialog_progress)

        mProgressDialog.findViewById<TextView>(R.id.tv_progress_text).text = text

        mProgressDialog.setCanceledOnTouchOutside(isCancelable)

        mProgressDialog.show()
    }


    /**
     * Hides the progress dialog
     */
    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }

    /**
     * A wrapper function to make a request, check if it failed, and log out if it did.
     * @param request: The function to make the request (retrofit)
     * @param errorMessage: An error message to be logged out in case on an error
     * @return The response of the request. the type is generic, matching the request. If the request failed, will be null, otherwise it is not null.
     */
    suspend fun <T> handleResponse(
        request : suspend () -> Response<T>,
        errorMessage : String
    ) : T? {
        val response = request()
        if (!response.isSuccessful || response.body() == null) {
            Log.e("TAG", "$errorMessage. error is: ${response.errorBody()?.toString()}")
            return null
        }
        return response.body()!!
    }


    /**
     * Shows an error snackbar
     * @param message: The message
     */
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