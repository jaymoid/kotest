package io.kotest.assertions.eq

/**
 * Determine is a class is a data
 */
actual fun Any?.isDataClass(): Boolean = this != null && this::class.isData
