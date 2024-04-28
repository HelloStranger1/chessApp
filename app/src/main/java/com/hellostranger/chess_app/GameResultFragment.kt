package com.hellostranger.chess_app

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.gameClasses.enums.GameState

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_WHITE_NAME = "whiteName"
private const val ARG_BLACK_NAME = "blackName"
private const val ARG_WHITE_IMAGE = "whiteImg"
private const val ARG_BLACK_IMAGE = "blackImg"
private const val ARG_OUR_ELO = "ourElo"
private const val ARG_RESULT = "gameResult"
private const val ARG_RESULT_DESC = "resultDesc"
/**
 * A simple [Fragment] subclass.
 * Use the [GameResultFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GameResultFragment : DialogFragment() {

    private var whiteName: String? = null
    private var blackName: String? = null
    private var whiteImage : String? = null
    private var blackImage : String? = null
    private var ourElo : Int = 0
    private var gameResult : GameState? = null
    private var gameResultDesc : String? = null

    private var whiteImageView : ImageView? = null
    private var blackImageView : ImageView? = null
    private var whiteTextView : TextView? = null
    private var blackTextView : TextView? = null
    private var resultTextTextView : TextView? = null
    private var resultTextView : TextView? = null
    private var resultDescTextView : TextView? = null
    private var eloTextView : TextView? = null
    private var btnBackToMenu : AppCompatButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            whiteName = it.getString(ARG_WHITE_NAME)
            blackName = it.getString(ARG_BLACK_NAME)
            whiteImage = it.getString(ARG_WHITE_IMAGE)
            blackImage = it.getString(ARG_BLACK_IMAGE)
            ourElo = it.getInt(ARG_OUR_ELO)
            val resInt = it.getInt(ARG_RESULT)
            gameResult = if(resInt == 0) {GameState.WHITE_WIN} else if(resInt == 1) {GameState.DRAW} else {GameState.BLACK_WIN}
            gameResultDesc = it.getString(ARG_RESULT_DESC)
        }


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view : View? = inflater.inflate(R.layout.fragment_game_result, container, false)
        whiteImageView = view?.findViewById(R.id.iv_player1)
        blackImageView = view?.findViewById(R.id.iv_player2)
        whiteTextView = view?.findViewById(R.id.tv_player1_name)
        blackTextView = view?.findViewById(R.id.tv_player2_name)
        resultTextTextView = view?.findViewById(R.id.tv_result_text)
        resultTextView = view?.findViewById(R.id.tv_result)
        resultDescTextView = view?.findViewById(R.id.tv_result_description)

        eloTextView = view?.findViewById(R.id.tv_elo)
        btnBackToMenu = view?.findViewById(R.id.btn_result_back_to_menu)
        Glide
            .with(this@GameResultFragment)
            .load(whiteImage)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(whiteImageView!!)
        Glide
            .with(this@GameResultFragment)
            .load(blackImage)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(blackImageView!!)
        whiteTextView!!.text = whiteName
        blackTextView!!.text = blackName
        resultTextView!!.text = if(gameResult == GameState.WHITE_WIN) "1 - 0" else if(gameResult == GameState.DRAW) "0.5 - 0.5" else "0 - 1"
        when (gameResult) {
            GameState.WHITE_WIN -> {
                whiteImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_win)
                blackImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_lose)
                resultTextTextView!!.text = "White won"
            }
            GameState.BLACK_WIN -> {
                whiteImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_lose)
                blackImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_win)
                resultTextTextView!!.text = "Black won"
            }
            else -> {
                whiteImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_draw)
                blackImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_draw)
                resultTextTextView!!.text = "Draw"
            }
        }
        resultDescTextView!!.text = gameResultDesc
        eloTextView!!.text = ourElo.toString()

        btnBackToMenu?.setOnClickListener {
            requireActivity().finish()
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         *
         * @return A new instance of fragment GameResultFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(whiteName: String, blackName: String, whiteImage : String, blackImage : String, ourElo : Int, result : Int, resultDesc : String) : GameResultFragment =
            GameResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WHITE_NAME, whiteName)
                    putString(ARG_BLACK_NAME, blackName)
                    putString(ARG_WHITE_IMAGE, whiteImage)
                    putString(ARG_BLACK_IMAGE, blackImage)
                    putInt(ARG_OUR_ELO, ourElo)
                    putInt(ARG_RESULT, result)
                    putString(ARG_RESULT_DESC, resultDesc)
                    Log.i("TAG", "resultDesc $resultDesc")
                }
            }
    }
}