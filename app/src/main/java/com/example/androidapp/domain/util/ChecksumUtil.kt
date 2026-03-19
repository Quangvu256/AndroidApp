package com.example.androidapp.domain.util

import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import java.security.MessageDigest

/**
 * SHA-256 checksum computation for quiz data integrity verification.
 *
 * Produces a deterministic hex-encoded hash from a quiz's content and its
 * ordered questions/choices, enabling change detection without comparing
 * every field individually.
 */
object ChecksumUtil {

    /**
     * Computes a SHA-256 checksum over the essential content of a quiz and its questions.
     *
     * Questions are sorted by [Question.position] and choices within each question
     * are sorted by their position before hashing, so the result is independent of
     * the order in which they are supplied.
     *
     * @param quiz The [Quiz] whose title and description are included in the hash.
     * @param questions The list of [Question]s (with nested choices) belonging to the quiz.
     * @return A lowercase hex-encoded SHA-256 digest string.
     */
    fun computeQuizChecksum(quiz: Quiz, questions: List<Question>): String {
        val sep = "|"
        val data = buildString {
            append(quiz.title); append(sep)
            append(quiz.description ?: ""); append(sep)
            questions.sortedBy { it.position }.forEach { q ->
                append(q.content); append(sep)
                append(q.mediaUrl ?: ""); append(sep)
                append(q.explanation ?: ""); append(sep)
                append(q.points); append(sep)
                append(q.isMultiSelect); append(sep)
                q.choices.sortedBy { it.position }.forEach { c ->
                    append(c.content); append(sep)
                    append(c.isCorrect); append(sep)
                }
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
