package models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val text: String,
    val date: LocalDateTime = LocalDateTime.now(),
    val dateString: String = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy @ hh:mm"))
)