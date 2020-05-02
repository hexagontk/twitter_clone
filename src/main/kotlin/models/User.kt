package models

import java.math.BigInteger
import java.security.MessageDigest
import kotlin.experimental.and
import kotlin.experimental.or

fun toHex(byteArray: ByteArray): String {
    val sb = StringBuffer()
    for (element in byteArray) {
        sb.append(
            Integer.toHexString(((element and 0xFF.toByte()) or 0x100.toByte()).toInt()).substring(1, 3)
        )
    }
    return sb.toString()
}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

data class User(
    val email: String,
    val username: String,
    val password: String,
    val following: MutableSet<String> = mutableSetOf(),
    val gravatarUrl: String = "https://www.gravatar.com/avatar/${email.md5()}?d=monsterid"
)