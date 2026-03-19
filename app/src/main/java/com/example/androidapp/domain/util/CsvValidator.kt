package com.example.androidapp.domain.util

/**
 * Represents an error found during CSV data validation.
 *
 * @property lineNumber The exact line number in the CSV file where the error occurred.
 * @property message The detailed description of the validation error.
 */
data class CsvValidationError(
    val lineNumber: Int,
    val message: String
)

/**
 * Utility object for validating parsed CSV data before converting it to domain models.
 */
object CsvValidator {

    /**
     * Validates a list of parsed CSV rows against a set of required column headers.
     * Generates a list of errors with exact line numbers if any required data is missing.
     *
     * @param parsedRows The list of maps representing the CSV rows (output from CsvParser).
     * @param requiredHeaders The list of column headers that must exist and cannot be blank.
     * @return A list of CsvValidationError objects. Returns an empty list if all data is valid.
     */
    fun validate(
        parsedRows: List<Map<String, String>>,
        requiredHeaders: List<String>
    ): List<CsvValidationError> {
        val errors = mutableListOf<CsvValidationError>()

        if (parsedRows.isEmpty()) {
            errors.add(CsvValidationError(0, "The imported file contains no data rows."))
            return errors
        }

        // CSV line numbers typically start at 1.
        // Assuming line 1 is the header, the first data row is line 2.
        parsedRows.forEachIndexed { index, row ->
            val lineNumber = index + 2
            var isRowCompletelyEmpty = true

            for (header in requiredHeaders) {
                val value = row[header]

                if (value.isNullOrBlank()) {
                    errors.add(CsvValidationError(lineNumber, "Missing or blank value for required column: '$header'."))
                } else {
                    isRowCompletelyEmpty = false
                }
            }

            // If a row was parsed but contains absolutely no data in the required columns
            if (isRowCompletelyEmpty && requiredHeaders.isNotEmpty()) {
                errors.add(CsvValidationError(lineNumber, "The row is entirely empty or missing all required fields."))
            }
        }

        return errors
    }
}