package com.hellostranger.chess_app

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.core.helpers.Arbiter
import com.hellostranger.chess_app.core.board.GameResult

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_WHITE_NAME = "whiteName"
private const val ARG_BLACK_NAME = "blackName"
private const val ARG_WHITE_IMAGE = "whiteImg"
private const val ARG_BLACK_IMAGE = "blackImg"
private const val ARG_OUR_ELO = "ourElo"
private const val ARG_RESULT = "gameResult"
@ExperimentalUnsignedTypes
class GameResultFragment : DialogFragment() {

    private var whiteName: String? = null
    private var blackName: String? = null
    private var whiteImage : String? = null
    private var blackImage : String? = null
    private var ourElo : Int = 0
    private var gameResult : GameResult? = null

    private var whiteImageView : ImageView? = null
    private var blackImageView : ImageView? = null
    private var whiteTextView : TextView? = null
    private var blackTextView : TextView? = null
    private var resultTextTextView : TextView? = null
    private var resultTextView : TextView? = null
    private var resultDescTextView : TextView? = null
    private var eloTextView : TextView? = null
    private var btnBackToMenu : AppCompatButton? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            whiteName = it.getString(ARG_WHITE_NAME)
            blackName = it.getString(ARG_BLACK_NAME)
            whiteImage = it.getString(ARG_WHITE_IMAGE)
            blackImage = it.getString(ARG_BLACK_IMAGE)
            ourElo = it.getInt(ARG_OUR_ELO)
            gameResult = it.getSerializable(ARG_RESULT, GameResult::class.java)
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
        if (Arbiter.isWhiteWinResult(gameResult!!)) {
            whiteImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_win)
            blackImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_lose)
            resultTextView!!.text = "1 - 0"
            resultTextTextView!!.text = "White Won"
        } else if (Arbiter.isBlackWinResult(gameResult!!)) {
            whiteImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_lose)
            blackImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_win)
            resultTextView!!.text = "0 - 1"
            resultTextTextView!!.text = "Black Won"
        } else {
            whiteImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_draw)
            blackImageView!!.background = ContextCompat.getDrawable(requireContext(), R.drawable.image_border_draw)
            resultTextView!!.text = "0.5 - 0.5"
            resultTextTextView!!.text = "Draw"
        }
        resultDescTextView!!.text = Arbiter.getResultDescription(gameResult!!)
        eloTextView!!.text = ourElo.toString()

        btnBackToMenu?.setOnClickListener {
            requireActivity().finish()
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(whiteName: String, blackName: String, whiteImage : String, blackImage : String, ourElo : Int, result : GameResult) : GameResultFragment =
            GameResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WHITE_NAME, whiteName)
                    putString(ARG_BLACK_NAME, blackName)
                    putString(ARG_WHITE_IMAGE, whiteImage)
                    putString(ARG_BLACK_IMAGE, blackImage)
                    putInt(ARG_OUR_ELO, ourElo)
                    putSerializable(ARG_RESULT, result)
                }
            }
    }
}