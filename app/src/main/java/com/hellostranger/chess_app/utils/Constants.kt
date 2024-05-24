package com.hellostranger.chess_app.utils

import com.hellostranger.chess_app.R

object Constants {
    const val scaleFactor = 0.9f

    //To fetch GameMode from intent
    const val MODE = "MODE"

    //Game modes
    const val ONLINE_MODE = "Online"
    const val ANALYSIS_MODE = "Analysis"
    const val AI_MODE = "AI"
    const val PUZZLES_MODE = "Puzzles"

    // To pass around user info
    const val USER_EMAIL = "UserEmail"
    const val USER_NAME = "UserName"
    const val USER_ELO = "UserElo"
    const val USER_IMAGE = "UserImage"


    //For Analysis:
    const val START_DATA = "Start"
    const val MOVES_LIST = "MOVES"

    //To see profile of another user
    const val GUEST_EMAIL = "GuestEmail"

    const val DEFAULT_PUZZLE_AMOUNT = 4

    const val FAVORITE_GAMES_DB = "favorite-games-database"
    val plantResIDs = setOf(
        R.drawable.ic_white_king_plant,
        R.drawable.ic_white_queen,
        R.drawable.ic_white_rook_plant,
        R.drawable.ic_white_knight_plant,
        R.drawable.ic_white_bishop_plant,
        R.drawable.ic_white_pawn_plant,

        R.drawable.ic_black_king_plant,
        R.drawable.ic_black_queen,
        R.drawable.ic_black_rook_plant,
        R.drawable.ic_black_knight_plant,
        R.drawable.ic_black_bishop_plant,
        R.drawable.ic_black_pawn_plant,
    )
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