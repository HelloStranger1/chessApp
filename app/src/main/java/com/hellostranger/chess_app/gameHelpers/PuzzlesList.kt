package com.hellostranger.chess_app.gameHelpers

import com.hellostranger.chess_app.models.entities.Puzzle

class PuzzlesList{

    private var puzzles : MutableList<Puzzle> = mutableListOf()
    private var mCurrentPuzzle : Int = -1
    companion object {
        val instance: PuzzlesList by lazy {
            PuzzlesList()
        }
    }

    fun getCurrentPuzzle() : Puzzle?{
        if(mCurrentPuzzle == -1 || mCurrentPuzzle >= puzzles.size){
            return null
        }
        return puzzles[mCurrentPuzzle]
    }

    fun goToNextPuzzle() : Puzzle?{
        if(mCurrentPuzzle == -1 || mCurrentPuzzle >= puzzles.size -1){
            return null
        }
        mCurrentPuzzle += 1
        return puzzles[mCurrentPuzzle]
    }
    fun addPuzzles(puzzlesList : List<Puzzle>){
        if(mCurrentPuzzle == -1){
            mCurrentPuzzle = 0
        }
        puzzles.addAll(puzzlesList)
    }

}