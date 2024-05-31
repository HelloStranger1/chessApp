package com.hellostranger.chess_app.core.board

enum class GameResult {
    NotStarted,
    Waiting,
    Aborted,
    InProgress,
    WhiteIsMated,
    BlackIsMated,
    WhiteResigned,
    BlackResigned,
    Stalemate,
    Repetition,
    FiftyMoveRule,
    InsufficientMaterial,
    DrawByArbiter,
    DrawByAgreement,
}