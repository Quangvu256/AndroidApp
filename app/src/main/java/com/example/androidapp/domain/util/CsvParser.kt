package com.example.androidapp.domain.util

/**
 * Utility object for parsing CSV data into structured formats to extract quiz questions.
 * Note: File I/O operations (like reading from Android Uri) must be done in the Data layer.
 * This domain utility purely handles the string manipulation and CSV structure extraction.
 */
object CsvParser {

    /**
     * Parses a raw CSV string into a list of maps.
     * Each map represents a row, where the keys are the column headers and the values are the cell contents.
     *
     * @param csvContent The raw string content of the CSV file.
     * @param delimiter The character used to separate values (default is comma).
     * @return A list of rows, mapped by their header names.
     */
    fun parse(csvContent: String, delimiter: Char = ','): List<Map<String, String>> {
        if (csvContent.isBlank()) return emptyList()

        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Extract headers from the first line
        val headers = parseLine(lines.first(), delimiter).map { it.trim() }
        val result = mutableListOf<Map<String, String>>()

        // Parse remaining lines
        for (i in 1 until lines.size) {
            val values = parseLine(lines[i], delimiter)
            val rowMap = mutableMapOf<String, String>()

            for (j in headers.indices) {
                // Handle cases where a row might have fewer columns than the header
                val value = if (j < values.size) values[j].trim() else ""
                rowMap[headers[j]] = value
            }
            result.add(rowMap)
        }

        return result
    }

    /**
     * Parses a single line of a CSV file, properly handling values enclosed in double quotes.
     *
     * @param line The string representation of a single CSV row.
     * @param delimiter The character used to separate values.
     * @return A list of extracted string values from the row.
     */
    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes // Toggle quote state
                char == delimiter && !inQuotes -> {
                    // End of a token
                    result.add(currentToken.toString())
                    currentToken = StringBuilder()
                }
                else -> currentToken.append(char)
            }
        }
        // Add the final token
        result.add(currentToken.toString())

        return result
    }
}