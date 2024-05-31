package com.hellostranger.chess_app.core.search

import com.hellostranger.chess_app.core.board.Board
import com.hellostranger.chess_app.core.board.Move
import com.hellostranger.chess_app.core.board.Piece
import com.hellostranger.chess_app.core.evaluation.Evaluation
import com.hellostranger.chess_app.core.helpers.BoardHelper
import com.hellostranger.chess_app.core.helpers.FenUtility
import com.hellostranger.chess_app.core.helpers.MoveUtility
import com.hellostranger.chess_app.core.moveGeneration.MoveGenerator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@ExperimentalUnsignedTypes
class Searcher(val board: Board) {
    companion object {
        const val MAX_EXTENSION = 16
        const val IMMEDIATE_MATE_SCORE = 100_000
        const val POSITIVE_INFINITY = 999_999
        const val NEGATIVE_INFINITY = -999_999

        fun isMateScore(score : Int) : Boolean {
            if (score == Int.MIN_VALUE) {
                return false
            }
            val maxMateDepth = 1_000
            return abs(score) > IMMEDIATE_MATE_SCORE - maxMateDepth
        }
        fun numPlyToMateScore(score: Int) : Int {
            return IMMEDIATE_MATE_SCORE - abs(score)
        }
    }

    var onSearchComplete : ((move : Move) -> Unit)? = null

    // State
    private var currentDepth : Int = 0
    private var isPlayingWhite = true
    private lateinit var bestMoveThisIteration : Move
    private var bestEvalThisIteration = 0
    private lateinit var bestMove : Move
    private var bestEval : Int = 0
    private var hasSearchedAtLeastOnMove : Boolean = false
    private var searchCanceled : Boolean = false

    // Diagnostics
    private var searchDiagnostics = SearchDiagnostics()
    private var currentIterationDepth : Int = 0
    private var debugInfo : String = ""


    // References
    private val transpositionTable : TranspositionTable = TranspositionTable(board)
    private val repetitionTable : RepetitionTable = RepetitionTable()
    private val moveGenerator : MoveGenerator = MoveGenerator()
    private val moveOrderer = MoveOrdering()
    private val evaluation = Evaluation()

    init {
        moveGenerator.promotionsToGenerate = MoveGenerator.PromotionMode.QueenAndKnight
    }

    fun startSearch() {
        // Initialize search
        bestEvalThisIteration = 0
        bestEval = 0
        bestMoveThisIteration = Move.NullMove
        bestMove = Move.NullMove

        isPlayingWhite = board.isWhiteToMove

        moveOrderer.clearHistory()
        repetitionTable.initialize(board)
        searchCanceled = false

        // Initialize Debug Info
        currentDepth = 0
        debugInfo = "Starting search with FEN ${FenUtility.currentFen(board)}"
        searchDiagnostics = SearchDiagnostics()

        runIterativeDeepeningSearch()


        if (bestMove.isNull) {
            bestMove = moveGenerator.generateMoves(board)[0]
        }
        onSearchComplete?.invoke(bestMove)
        println("Invoked on searched complete with move $bestMove")
        searchCanceled = false
    }

    private fun runIterativeDeepeningSearch() {
        for (searchDepth in 1..256) {
            hasSearchedAtLeastOnMove = false
            debugInfo += "\nStarting Iteration $searchDepth"
            currentIterationDepth = searchDepth

            search(searchDepth, 0, NEGATIVE_INFINITY, POSITIVE_INFINITY)

            if (searchCanceled) {
                if (hasSearchedAtLeastOnMove) {
                    bestMove = bestMoveThisIteration
                    bestEval = bestEvalThisIteration
                    searchDiagnostics.move = MoveUtility.getMoveNameUCI(bestMove)
                    searchDiagnostics.eval = bestEval
                    searchDiagnostics.moveIsFromPartialSearch = true
                    debugInfo += "\nUsing partial search result: ${searchDiagnostics.move} Eval: $bestEval"
                }
                debugInfo += "\nSearch Aborted"
                break
            }

            currentDepth = searchDepth
            bestMove = bestMoveThisIteration
            bestEval = bestEvalThisIteration

            debugInfo += "\nIteration result: ${MoveUtility.getMoveNameUCI(bestMove)} Eval: $bestEval"
            if (isMateScore(bestEval)) {
                debugInfo += " Mate in ply: ${numPlyToMateScore(bestEval)}"
            }

            bestEvalThisIteration = Int.MIN_VALUE
            bestMoveThisIteration = Move.NullMove

            // Update Diagnostics
            searchDiagnostics.numCompletedIterations = searchDepth
            searchDiagnostics.move = MoveUtility.getMoveNameUCI(bestMove)
            searchDiagnostics.eval = bestEval

            // Exit search if found a mate within search depth.
            // A mate found outside of search depth (due to extensions) may not be the fastest mate.
            if (isMateScore(bestEval) && numPlyToMateScore(bestEval) <= searchDepth) {
                debugInfo += "\nExiting search due to mate found within search depth"
                break
            }
        }
    }

    data class SearchResult(val move : Move, val eval : Int)
    fun getSearchResult() : SearchResult {
        return SearchResult(bestMove, bestEval)
    }

    fun endSearch() { searchCanceled = true }

    private fun search(plyRemaining : Int, plyFromRoot : Int, prevAlpha : Int, prevBeta : Int, numExtensions : Int = 0, prevMove : Move = Move.NullMove, prevWasCapture : Boolean = false) : Int {
        var alpha : Int = prevAlpha
        var beta  : Int = prevBeta
        if (searchCanceled) {
            return 0
        }
        if (plyFromRoot > 0) {
            // Detect draw by three-fold repetition.
            // (Note: returns a draw score even if this position has only appeared once for sake of simplicity)
            if (board.fiftyMoveCount >= 100 ||repetitionTable.contains(board.zobristKey)) {
                return 0
            }
            // Skip this position if a mating sequence has already been found earlier in the search, which would be shorter
            // than any mate we could find from here. This is done by observing that alpha can't possibly be worse
            // (and likewise beta can't  possibly be better) than being mated in the current position.
            alpha = max(alpha, -IMMEDIATE_MATE_SCORE + plyFromRoot)
            beta = min(beta, IMMEDIATE_MATE_SCORE - plyFromRoot)
            if (alpha > beta) {
                return alpha
            }
        }

        // Try looking up the current position in the transposition table.
        // If the same position has already been searched to at least an equal depth
        // to the search we're doing now,we can just use the recorded evaluation.
        val ttVal = transpositionTable.lookupEvaluation(plyRemaining, plyFromRoot, alpha, beta)
        if (ttVal != TranspositionTable.LOOKUP_FAILED) {
            if (plyFromRoot == 0) {
                bestMoveThisIteration = transpositionTable.tryToGetStoredMove() ?: Move.NullMove
                bestEvalThisIteration = transpositionTable.entries[transpositionTable.index].value
            }
            return ttVal
        }

        if (plyRemaining == 0) {
            return quiescenceSearch(alpha, beta)
        }

        val moves : Array<Move> = moveGenerator.generateMoves(board, arrayOfNulls(256), capturesOnly = false)
        val prevBestMove : Move = if (plyFromRoot == 0) bestMove else transpositionTable.tryToGetStoredMove() ?: Move.NullMove
        moveOrderer.orderMoves(prevBestMove, board, moves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, false, plyFromRoot)
        if (moves.isEmpty()) {
            return if (moveGenerator.inCheck()) {
                -(IMMEDIATE_MATE_SCORE - plyFromRoot)
            } else {
                0
            }
        }

        if (plyFromRoot > 0) {
            val wasPawnMove : Boolean = Piece.pieceType(board.square[prevMove.targetSquare]) == Piece.PAWN
            repetitionTable.push(board.zobristKey, prevWasCapture || wasPawnMove)
        }

        var evaluationBound : Int = TranspositionTable.UPPER_BOUND
        var bestMoveInThisPosition : Move = Move.NullMove

        for (i in moves.indices) {
            val move = moves[i]
            val capturedPieceType = Piece.pieceType(board.square[move.targetSquare])
            val isCapture : Boolean = capturedPieceType != Piece.NONE
            board.makeMove(moves[i], inSearch = true)

            var extension = 0
            if (numExtensions < MAX_EXTENSION) {
                val movedPieceType = Piece.pieceType(board.square[move.targetSquare])
                val targetRank = BoardHelper.rankIndex(move.targetSquare)
                if (board.isInCheck()) {
                    extension = 1
                } else if (movedPieceType == Piece.PAWN && (targetRank == 1 || targetRank == 6)) {
                    extension = 1
                }
            }

            var needsFullSearch = true
            var eval = 0
            // Reduce the depth of the search for moves later in the move list as these are less likely to be good
            // (assuming our move ordering isn't terrible)
            if (extension == 0 && plyRemaining >= 3 && i >= 3 && !isCapture) {
                eval = -search(plyRemaining - 2, plyRemaining + 1, -alpha - 1, -alpha, numExtensions, move, false)
                // If the evaluation is better than expected, we'd better to a full-depth search to get a more accurate evaluation
                needsFullSearch = eval > alpha
            }
            // Perform a full-depth search
            if (needsFullSearch) {
                eval = -search(plyRemaining - 1 + extension, plyFromRoot + 1, -beta, -alpha, numExtensions + extension, move, isCapture)
            }
            board.unmakeMove(moves[i], inSearch = true)

            if (searchCanceled) {
                return 0
            }
            // Move was *too* good, opponent will choose a different move earlier on to avoid this position.
            // (Beta-cutoff / Fail high)
            if (eval >= beta) {
                transpositionTable.storeEvaluation(plyRemaining, plyFromRoot, beta,
                    TranspositionTable.LOWER_BOUND, move)

                // Update killer moves and history heuristic
                if (!isCapture) {
                    if (plyFromRoot < MoveOrdering.MAX_KILLER_MOVE_PLY) {
                        moveOrderer.killerMoves[plyFromRoot].add(move)
                    }
                    val historyScore = plyRemaining * plyRemaining
                    moveOrderer.history[board.moveColourIndex][move.startSquare][move.targetSquare] += historyScore
                }
                if (plyFromRoot > 0) {
                    repetitionTable.tryPop()
                }

                searchDiagnostics.numCutOffs++
                return beta
            }

            // Found a new best move in this position
            if (eval > alpha) {
                evaluationBound = TranspositionTable.EXACT
                bestMoveInThisPosition = move

                alpha = eval
                if (plyFromRoot == 0) {
                    bestMoveThisIteration = move
                    bestEvalThisIteration = eval
                    hasSearchedAtLeastOnMove = true
                }
            }

        }

        if (plyFromRoot > 0) {
            repetitionTable.tryPop()
        }
        transpositionTable.storeEvaluation(plyRemaining, plyFromRoot, alpha, evaluationBound, bestMoveInThisPosition)

        return alpha
    }

    // Search capture moves until a 'quiet' position is reached.
    private fun quiescenceSearch(prevAlpha : Int, beta: Int) : Int {
        if (searchCanceled) {
            return 0
        }
        var alpha = prevAlpha
        // A player isn't forced to make a capture (typically), so see what the evaluation is without capturing anything.
        // This prevents situations where a player ony has bad captures available from being evaluated as bad,
        // when the player might have good non-capture moves available.

        var eval : Int = evaluation.evaluate(board)
        searchDiagnostics.numPositionsEvaluated++
        if (eval >= beta) {
            searchDiagnostics.numCutOffs++
            return beta
        }
        if (eval > alpha) {
            alpha = eval
        }

        val moves = moveGenerator.generateMoves(board, arrayOfNulls(128), capturesOnly = true)
        moveOrderer.orderMoves(Move.NullMove, board, moves, moveGenerator.opponentAttackMap, moveGenerator.opponentPawnAttackMap, true , 0)
        for (i in moves.indices) {
            board.makeMove(moves[i], true)
            eval = -quiescenceSearch(-beta, -alpha)
            board.unmakeMove(moves[i], true)

            if (eval >= beta) {
                searchDiagnostics.numCutOffs++
                return beta
            }
            if (eval > alpha) {
                alpha = eval
            }

        }

        return alpha
    }

    class SearchDiagnostics {
        var numCompletedIterations : Int = 0
        var numPositionsEvaluated : Int = 0
        var numCutOffs : ULong = 0UL

        var move : String = ""
        var eval : Int = 0
        var moveIsFromPartialSearch = false
    }


}