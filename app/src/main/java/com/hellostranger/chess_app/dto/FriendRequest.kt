package com.hellostranger.chess_app.dto

import com.hellostranger.chess_app.models.entites.User

data class FriendRequest ( val id : Int,  val sender : User,  val recipient : User)