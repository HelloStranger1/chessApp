package com.hellostranger.chess_app.dto.requests

import com.hellostranger.chess_app.models.entities.User

data class FriendRequest (val id : Int, val sender : User, val recipient : User)