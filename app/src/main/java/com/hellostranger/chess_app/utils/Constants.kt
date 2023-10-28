package com.hellostranger.chess_app.utils

import com.hellostranger.chess_app.R

object Constants {
    const val scaleFactor = 0.9f

    //To fetch GameMode from intent
    const val MODE = "MODE"

    //Game modes
    const val ONLINE_MODE = "Online"
    const val ANALYSIS_MODE = "Analysis"

    //For Analysis:
    const val START_DATA = "Start"
    const val MOVES_LIST = "MOVES"

    const val FAVORITE_GAMES_DB = "favorite-games-database"
    val imgResIDs = setOf(
        R.drawable.ic_white_king,
        R.drawable.ic_white_queen,
        R.drawable.ic_white_rook,
        R.drawable.ic_white_knight,
        R.drawable.ic_white_bishop,
        R.drawable.ic_white_pawn,

        R.drawable.ic_black_king,
        R.drawable.ic_black_queen,
        R.drawable.ic_black_rook,
        R.drawable.ic_black_knight,
        R.drawable.ic_black_bishop,
        R.drawable.ic_black_pawn,
    )
}