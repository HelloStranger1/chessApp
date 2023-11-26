package com.hellostranger.chess_app.dto

data class Puzzle(
    val id: String,
    val fen: String,
    val moves: List<String>,
    val numberOfMoves: String,
    val rating: String,
    val ratingDeviation: String,
    val minRating: String,
    val maxRating: String,
    val themes: String,
    val openingFamily: String,
    val openingVariation: String,
)
