package com.example.androidapp.domain.util

import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import java.security.MessageDigest

object ChecksumUtil {

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
