package models

import md5

data class User(
    val email: String,
    val username: String,
    val password: String,
    val following: Set<String> = setOf(),
    val gravatarUrl: String = "https://www.gravatar.com/avatar/${email.md5()}?d=monsterid"
)
