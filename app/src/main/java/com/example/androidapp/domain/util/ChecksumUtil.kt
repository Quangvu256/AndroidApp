package com.example.androidapp.domain.util

import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import java.security.MessageDigest

object ChecksumUtil {

    fun computeQuizChecksum(quiz: Quiz, questions: List<Question>): String {
        val data = buildString {
            append(quiz.title)
            append(quiz.description ?: "")
            questions.sortedBy { it.position }.forEach { q ->
                append(q.content)
                append(q.mediaUrl ?: "")
                append(q.explanation ?: "")
                append(q.points)
                append(q.isMultiSelect)
                q.choices.sortedBy { it.position }.forEach { c ->
                    append(c.content)
                    append(c.isCorrect)
                }
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray())
            .fold("") { str, byte -> str + "%02x".format(byte) }
    }
}
