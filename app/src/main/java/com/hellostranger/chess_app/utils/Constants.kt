package com.hellostranger.chess_app.utils

import com.hellostranger.chess_app.R

object Constants {
    const val SCALE_FACTOR = 0.9f

    //To fetch GameMode from intent
    const val MODE = "MODE"

    //Game modes
    const val ONLINE_MODE = "Online"
    const val ANALYSIS_MODE = "Analysis"
    const val AI_MODE = "AI"

    // To pass around user info
    const val USER = "USER"


    //For Analysis:
    const val START_DATA = "Start"
    const val MOVES_LIST = "MOVES"

    const val DEFAULT_PUZZLE_AMOUNT = 4

    const val FAVORITE_GAMES_DB = "favorite-games-database"
}