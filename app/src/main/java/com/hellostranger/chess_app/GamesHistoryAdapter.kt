package com.hellostranger.chess_app

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hellostranger.chess_app.models.gameModels.enums.Color
import com.hellostranger.chess_app.models.gameModels.enums.GameState
import com.hellostranger.chess_app.databinding.GameHistoryItemBinding
import com.hellostranger.chess_app.models.entites.GameHistory

class GamesHistoryAdapter(
    private val onClickListener: OnClickListener,
    private val MAX_ITEMS : Int = -1
    )
    : RecyclerView.Adapter<GamesHistoryAdapter.GamesHistoryViewHolder>(){

    private var gameHistoryList = mutableListOf<GameHistory>()
    fun updateGameHistoryList(updateGameHistoryList : List<GameHistory>){
        this.gameHistoryList = updateGameHistoryList.toMutableList()
        notifyDataSetChanged()
    }

    inner class GamesHistoryViewHolder(private val binding : GameHistoryItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        val saveImageView : ImageView = binding.ivSaveGame
        fun bind(gameHistory: GameHistory?){
            val opponentImage = if(gameHistory?.opponentColor == Color.WHITE) gameHistory.whiteImage else gameHistory?.blackImage
            val opponentName = if(gameHistory?.opponentColor == Color.WHITE) gameHistory.whiteName else gameHistory?.blackName
            val opponentElo = if(gameHistory?.opponentColor == Color.WHITE) gameHistory.whiteElo else gameHistory?.blackElo
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

            if(gameHistory?.result == GameState.WHITE_WIN){
                if(gameHistory.opponentColor == Color.WHITE){
                    binding.ivGameResult.setImageResource(R.drawable.ic_red_minus)
                } else{
                    binding.ivGameResult.setImageResource(R.drawable.ic_green_checkmark)
                }
            } else if(gameHistory?.result == GameState.BLACK_WIN){
                if(gameHistory.opponentColor == Color.WHITE){
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
            onClickListener.onSaveClick(gameHistory)
            notifyItemChanged(holder.layoutPosition)
        }
        holder.itemView.setOnClickListener{
            onClickListener.onItemClick(gameHistory)
        }
        holder.bind(gameHistory)
    }

    class OnClickListener(
        val itemClickListener: (gameHistory : GameHistory) -> Unit,
        val saveClickListener: (gameHistory: GameHistory) -> Unit)
    {
        fun onSaveClick(gameHistory: GameHistory) = saveClickListener(gameHistory)
        fun onItemClick(gameHistory: GameHistory) = itemClickListener(gameHistory)
    }

}

