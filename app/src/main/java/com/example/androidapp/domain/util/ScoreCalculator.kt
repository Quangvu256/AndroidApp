package com.example.androidapp.domain.util

/**
 * Utility object for calculating quiz scores.
 * Supports both single-choice and multiple-choice questions.
 */
object ScoreCalculator {

    /**
     * Calculates the total number of correctly answered questions.
     * A question is considered correct only if the user's selected choices
     * exactly match the correct choices (strict grading).
     *
     * @param correctAnswers A map where the key is the question ID and the value is a set of correct choice IDs.
     * @param userAnswers A map where the key is the question ID and the value is a set of the user's selected choice IDs.
     * @return The total number of correctly answered questions.
     */
    fun calculateCorrectCount(
        correctAnswers: Map<String, Set<String>>,
        userAnswers: Map<String, Set<String>>
    ): Int {
        var score = 0
        for ((questionId, correctChoiceIds) in correctAnswers) {
            val userChoiceIds = userAnswers[questionId] ?: emptySet()

            // Set equality checking in Kotlin compares the contents regardless of order.
            // This perfectly handles both single choice (Set size = 1) and multiple choice (Set size > 1).
            if (correctChoiceIds == userChoiceIds) {
                score++
            }
        }
        return score
    }

    /**
     * Calculates the percentage score based on correct answers and total questions.
     *
     * @param correctCount The number of correctly answered questions.
     * @param totalQuestions The total number of questions in the quiz.
     * @return The percentage score from 0.0 to 100.0. Returns 0.0 if totalQuestions is 0 or negative.
     */
    fun calculatePercentage(correctCount: Int, totalQuestions: Int): Double {
        if (totalQuestions <= 0) return 0.0
        return (correctCount.toDouble() / totalQuestions) * 100.0
    }
}