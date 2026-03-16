package com.example.androidapp.domain.util

/**
 * Represents the result of a quiz validation process.
 *
 * @property isValid True if the quiz meets all business requirements, false otherwise.
 * @property errorMessage A clear description of the validation failure, or null if valid.
 */
data class QuizValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Utility object for validating a quiz before saving it to the database or remote server.
 */
object QuizValidator {

    private const val MIN_QUESTIONS = 1
    private const val MIN_CHOICES = 2
    private const val MAX_CHOICES = 10

    /**
     * Validates the structure of a quiz based on strict business rules.
     * Rules: Minimum 1 question, each question must have 2-10 choices,
     * and each question must have at least 1 correct choice.
     *
     * @param Q The type representing a Question model.
     * @param C The type representing a Choice model.
     * @param questions The list of questions to validate.
     * @param getChoices A selector function to extract the list of choices from a question.
     * @param isCorrect A selector function to determine if a choice is marked as correct.
     * @return A QuizValidationResult indicating success or detailing the specific error.
     */
    fun <Q, C> validate(
        questions: List<Q>,
        getChoices: (Q) -> List<C>,
        isCorrect: (C) -> Boolean
    ): QuizValidationResult {
        if (questions.size < MIN_QUESTIONS) {
            return QuizValidationResult(
                isValid = false,
                errorMessage = "A quiz must have at least $MIN_QUESTIONS question."
            )
        }

        questions.forEachIndexed { index, question ->
            val choices = getChoices(question)
            val questionNumber = index + 1

            if (choices.size !in MIN_CHOICES..MAX_CHOICES) {
                return QuizValidationResult(
                    isValid = false,
                    errorMessage = "Question $questionNumber must have between $MIN_CHOICES and $MAX_CHOICES choices."
                )
            }

            val correctCount = choices.count { isCorrect(it) }
            if (correctCount < 1) {
                return QuizValidationResult(
                    isValid = false,
                    errorMessage = "Question $questionNumber must have at least 1 correct choice."
                )
            }
        }

        return QuizValidationResult(isValid = true)
    }
}