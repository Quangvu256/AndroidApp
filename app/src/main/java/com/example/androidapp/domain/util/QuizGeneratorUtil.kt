package com.example.androidapp.domain.util

/**
 * Utility object for automatically generating a draft quiz from a larger pool of questions.
 */
object QuizGeneratorUtil {

    /**
     * Selects a random subset of questions from a pool based on specified tags.
     * * @param Q The type representing a Question model.
     * @param questionPool The full list of available questions to draw from.
     * @param targetTags A set of tags to filter the question pool. If empty, questions are drawn from the entire pool.
     * @param desiredCount The number of questions to include in the generated draft.
     * @param getTags A selector function to extract tags from a question.
     * @return A randomly shuffled list of questions up to the desired count.
     */
    fun <Q> generateDraft(
        questionPool: List<Q>,
        targetTags: Set<String>,
        desiredCount: Int,
        getTags: (Q) -> List<String>
    ): List<Q> {
        if (questionPool.isEmpty() || desiredCount <= 0) {
            return emptyList()
        }

        // 1. Filter the pool by tags (if tags are provided)
        val filteredPool = if (targetTags.isEmpty()) {
            questionPool
        } else {
            questionPool.filter { question ->
                val questionTags = getTags(question)
                // Keep the question if it shares at least one tag with the target tags
                questionTags.any { it in targetTags }
            }
        }

        // 2. Shuffle the filtered pool to ensure randomness
        val shuffledPool = filteredPool.shuffled()

        // 3. Take the requested number of questions (or fewer if the pool is smaller than desiredCount)
        return shuffledPool.take(desiredCount)
    }
}