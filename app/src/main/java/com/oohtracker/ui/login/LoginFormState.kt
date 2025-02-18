package com.oohtracker.ui.login

/**
 * Data validation state of the login form.
 */
data class LoginFormState(
    val phoneNumberError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)