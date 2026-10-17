package com.arnavgames.cloakrun.core.game.lobby

import kotlin.random.Random

/**
 * Six-character lobby codes. The alphabet leaves out characters that are easy to confuse when read
 * aloud or off a screen in sunlight (0 and O, 1, I and L), so 31 symbols give about 887 million
 * codes. Codes only map to a match while it is open, so collisions are retried, not prevented.
 */
object JoinCode {
    const val LENGTH = 6
    const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

    fun generate(random: Random = Random.Default): String = buildString {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Cleans a typed or pasted code (case, spaces and dashes are ignored). Null if it cannot be a code. */
    fun normalize(input: String): String? = input.uppercase()
        .filterNot { it.isWhitespace() || it == '-' }
        .takeIf(::isValid)

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }

    /** Keeps only characters that can appear in a code, for live input filtering. */
    fun filterTyping(input: String): String = input.uppercase().filter { it in ALPHABET }.take(LENGTH)
}
