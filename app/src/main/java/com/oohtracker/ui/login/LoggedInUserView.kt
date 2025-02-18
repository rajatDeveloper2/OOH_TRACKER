package com.oohtracker.ui.login

/**
 * User details post authentication that is exposed to the UI
 */
data class LoggedInUserView(
    val newOTP: String
    //... other data fields that may be accessible to the UI
)