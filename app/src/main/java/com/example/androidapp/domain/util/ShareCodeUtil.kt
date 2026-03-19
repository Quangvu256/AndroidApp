package com.example.androidapp.domain.util

import kotlin.random.Random

/**
 * Utility object for generating unique share codes for quizzes.
 */
object ShareCodeUtil {

    // Using uppercase letters and numbers to avoid confusion (e.g., lowercase 'l' and number '1')
    private val CHAR_POOL: List<Char> = ('A'..'Z') + ('0'..'9')
    private const val CODE_LENGTH = 6

    /**
     * Generates a random 6-character alphanumeric code.
     *
     * @return A 6-character uppercase alphanumeric string.
     */
    fun generateCode(): String {
        return (1..CODE_LENGTH)
            .map { Random.nextInt(0, CHAR_POOL.size) }
            .map { CHAR_POOL[it] }
            .joinToString("")
    }
}