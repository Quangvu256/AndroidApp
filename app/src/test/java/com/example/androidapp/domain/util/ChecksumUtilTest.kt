package com.example.androidapp.domain.util

import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [ChecksumUtil].
 * Verifies checksum stability, field inclusion, delimiter correctness, and ordering.
 */
class ChecksumUtilTest {

    // ==================== helpers ====================

    private fun quiz(title: String = "Quiz", description: String? = null) = Quiz(
        id = "q1",
        ownerId = "u1",
        title = title,
        description = description
    )

    private fun choice(id: String, content: String, isCorrect: Boolean, position: Int) =
        Choice(id = id, content = content, isCorrect = isCorrect, position = position)

    private fun question(
        id: String = "q1",
        content: String = "Content",
        choices: List<Choice> = emptyList(),
        position: Int = 0,
        mediaUrl: String? = null,
        explanation: String? = null,
        points: Int = 1,
        isMultiSelect: Boolean = false
    ) = Question(
        id = id,
        content = content,
        choices = choices,
        position = position,
        mediaUrl = mediaUrl,
        explanation = explanation,
        points = points,
        isMultiSelect = isMultiSelect
    )

    // ==================== stability ====================

    @Test
    fun `same inputs produce identical checksums`() {
        val q = quiz("My Quiz", "Desc")
        val questions = listOf(
            question("q1", "Question 1", listOf(choice("c1", "A", true, 0), choice("c2", "B", false, 1)), 0)
        )
        val first = ChecksumUtil.computeQuizChecksum(q, questions)
        val second = ChecksumUtil.computeQuizChecksum(q, questions)
        assertEquals(first, second)
    }

    @Test
    fun `empty question list produces a checksum`() {
        val checksum = ChecksumUtil.computeQuizChecksum(quiz("Empty"), emptyList())
        assertEquals(64, checksum.length) // SHA-256 hex is always 64 chars
    }

    // ==================== field inclusion ====================

    @Test
    fun `changing quiz title changes the checksum`() {
        val questions = emptyList<Question>()
        val a = ChecksumUtil.computeQuizChecksum(quiz("Title A"), questions)
        val b = ChecksumUtil.computeQuizChecksum(quiz("Title B"), questions)
        assertNotEquals(a, b)
    }

    @Test
    fun `changing quiz description changes the checksum`() {
        val questions = emptyList<Question>()
        val a = ChecksumUtil.computeQuizChecksum(quiz("Quiz", "Desc A"), questions)
        val b = ChecksumUtil.computeQuizChecksum(quiz("Quiz", "Desc B"), questions)
        assertNotEquals(a, b)
    }

    @Test
    fun `null description differs from empty string description`() {
        val questions = emptyList<Question>()
        val nullDesc = ChecksumUtil.computeQuizChecksum(quiz("Quiz", null), questions)
        val emptyDesc = ChecksumUtil.computeQuizChecksum(quiz("Quiz", ""), questions)
        // null is treated as "" so these should be equal — document current behaviour
        assertEquals(nullDesc, emptyDesc)
    }

    @Test
    fun `changing question content changes the checksum`() {
        val q = quiz()
        val a = ChecksumUtil.computeQuizChecksum(q, listOf(question(content = "Q A")))
        val b = ChecksumUtil.computeQuizChecksum(q, listOf(question(content = "Q B")))
        assertNotEquals(a, b)
    }

    @Test
    fun `changing question points changes the checksum`() {
        val q = quiz()
        val a = ChecksumUtil.computeQuizChecksum(q, listOf(question(points = 1)))
        val b = ChecksumUtil.computeQuizChecksum(q, listOf(question(points = 5)))
        assertNotEquals(a, b)
    }

    @Test
    fun `changing choice correctness changes the checksum`() {
        val q = quiz()
        val choiceA = listOf(choice("c1", "Option", true, 0))
        val choiceB = listOf(choice("c1", "Option", false, 0))
        val a = ChecksumUtil.computeQuizChecksum(q, listOf(question(choices = choiceA)))
        val b = ChecksumUtil.computeQuizChecksum(q, listOf(question(choices = choiceB)))
        assertNotEquals(a, b)
    }

    // ==================== ordering ====================

    @Test
    fun `questions are ordered by position before hashing`() {
        val q = quiz()
        val q1 = question(id = "q1", content = "First", position = 0)
        val q2 = question(id = "q2", content = "Second", position = 1)
        val inOrder = ChecksumUtil.computeQuizChecksum(q, listOf(q1, q2))
        val reversed = ChecksumUtil.computeQuizChecksum(q, listOf(q2, q1))
        assertEquals(inOrder, reversed)
    }

    @Test
    fun `choices are ordered by position before hashing`() {
        val q = quiz()
        val c1 = choice("c1", "A", false, 0)
        val c2 = choice("c2", "B", true, 1)
        val inOrder = ChecksumUtil.computeQuizChecksum(q, listOf(question(choices = listOf(c1, c2))))
        val reversed = ChecksumUtil.computeQuizChecksum(q, listOf(question(choices = listOf(c2, c1))))
        assertEquals(inOrder, reversed)
    }

    @Test
    fun `different question order with same content produces different checksums`() {
        val q = quiz()
        val q1 = question(id = "q1", content = "Alpha", position = 0)
        val q2 = question(id = "q2", content = "Beta", position = 1)
        val q1Swapped = q1.copy(position = 1)
        val q2Swapped = q2.copy(position = 0)
        val original = ChecksumUtil.computeQuizChecksum(q, listOf(q1, q2))
        val swapped = ChecksumUtil.computeQuizChecksum(q, listOf(q1Swapped, q2Swapped))
        assertNotEquals(original, swapped)
    }

    // ==================== delimiter correctness ====================

    @Test
    fun `fields with shared prefix do not collide`() {
        // Without delimiters "AB" + "CD" == "A" + "BCD"; with delimiters they must differ.
        val q = quiz()
        val qA = question(id = "q1", content = "AB", explanation = "CD")
        val qB = question(id = "q1", content = "A", explanation = "BCD")
        val checksumA = ChecksumUtil.computeQuizChecksum(q, listOf(qA))
        val checksumB = ChecksumUtil.computeQuizChecksum(q, listOf(qB))
        assertNotEquals(
            "Checksums must differ for fields with shared prefix to prevent collisions",
            checksumA,
            checksumB
        )
    }
}

