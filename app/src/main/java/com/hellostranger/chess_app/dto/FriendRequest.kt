package com.hellostranger.chess_app.dto

import com.hellostranger.chess_app.models.entites.User

data class FriendRequest (private val id : Long, private val sender : User, private val recipient : User)