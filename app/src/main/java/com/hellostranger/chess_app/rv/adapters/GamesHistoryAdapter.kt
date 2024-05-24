package com.hellostranger.chess_app.rv.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.R
import com.hellostranger.chess_app.core.Arbiter
import com.hellostranger.chess_app.core.GameResult
import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.databinding.GameHistoryItemBinding
import com.hellostranger.chess_app.models.rvEntities.GameHistory

class GamesHistoryAdapter(
    private val gameHistoryOnClickListener: GameHistoryOnClickListener,
    private val MAX_ITEMS : Int = -1
    )
    : RecyclerView.Adapter<GamesHistoryAdapter.GamesHistoryViewHolder>(){

    private var gameHistoryList = mutableListOf<GameHistory>()
    fun updateGameHistoryList(updateGameHistoryList : List<GameHistory>){
        this.gameHistoryList = updateGameHistoryList.toMutableList()
        Log.e("GameHistoryAdapter","We have ${gameHistoryList.size} elements in the rv of game histories")
        notifyDataSetChanged()
    }

    @ExperimentalUnsignedTypes
    inner class GamesHistoryViewHolder(private val binding : GameHistoryItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        val saveImageView : ImageView = binding.ivSaveGame
        fun bind(gameHistory: GameHistory?){
            val opponentImage = if(gameHistory?.opponentColorIndex == Board.WHITE_INDEX) gameHistory.whiteImage else gameHistory?.blackImage
            val opponentName = if(gameHistory?.opponentColorIndex == Board.WHITE_INDEX) gameHistory.whiteName else gameHistory?.blackName
            val opponentElo = if(gameHistory?.opponentColorIndex == Board.WHITE_INDEX) gameHistory.whiteElo else gameHistory?.blackElo
            Glide
                .with(binding.ivOpponentImage)
                .load(opponentImage)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(binding.ivOpponentImage)
            binding.tvOpponentUsernameAndElo.text = "$opponentName ($opponentElo)"

            if(gameHistory?.isSaved == true){
                binding.ivSaveGame.setImageResource(R.drawable.ic_save_filled)
            } else{
                binding.ivSaveGame.setImageResource(R.drawable.ic_save_border)
            }

            if(gameHistory?.result?.let { Arbiter.isWhiteWinResult(it) } == true){
                if(gameHistory?.opponentColorIndex == Board.WHITE_INDEX){
                    binding.ivGameResult.setImageResource(R.drawable.ic_red_minus)
                } else{
                    binding.ivGameResult.setImageResource(R.drawable.ic_green_checkmark)
                }
            } else if(gameHistory?.result?.let { Arbiter.isBlackWinResult(it) } == true){
                if(gameHistory.opponentColorIndex == Board.WHITE_INDEX){
                    binding.ivGameResult.setImageResource(R.drawable.ic_green_checkmark)
                } else{
                    binding.ivGameResult.setImageResource(R.drawable.ic_red_minus)
                }
            } else{
                binding.ivGameResult.setImageResource(R.drawable.ic_grey_equals)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GamesHistoryViewHolder {
        return GamesHistoryViewHolder(
            GameHistoryItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return if(gameHistoryList.size  > MAX_ITEMS && MAX_ITEMS != -1){
            MAX_ITEMS
        } else{
            gameHistoryList.size
        }
    }

    override fun onBindViewHolder(holder: GamesHistoryViewHolder, position: Int) {
        val gameHistory = gameHistoryList[position]
        holder.saveImageView.setOnClickListener{
            gameHistoryOnClickListener.onSaveClick(gameHistory)
            notifyItemChanged(holder.layoutPosition)
        }
        holder.itemView.setOnClickListener{
            gameHistoryOnClickListener.onItemClick(gameHistory)
        }
        holder.bind(gameHistory)
    }

    class GameHistoryOnClickListener(
        val itemClickListener: (gameHistory : GameHistory) -> Unit,
        val saveClickListener: (gameHistory: GameHistory) -> Unit)

    {
        fun onSaveClick(gameHistory: GameHistory) = saveClickListener(gameHistory)
        fun onItemClick(gameHistory: GameHistory) = itemClickListener(gameHistory)
    }

}

