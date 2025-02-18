package com.oohtracker.ui.login

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.oohtracker.Constants
import com.oohtracker.R
import com.oohtracker.databinding.ActivityVerificationPart2Binding
import com.oohtracker.ui.main.MainActivity
import java.util.*

class VerificationActivityPart2 : AppCompatActivity() {
    private val OTPVerifyViewModel: VerificationViewModelP2 by viewModels()

    var deviceId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityVerificationPart2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val OTPVerification = intent.getStringExtra(Constants.OTPVerificationProcess).toString()
        val OTPVerificationPhoneNumber = intent.getStringExtra(Constants.Phone).toString()
        val prefMan = PreferenceManager.getDefaultSharedPreferences(this)
        deviceId = Settings.Secure.getString(
            applicationContext.contentResolver, Settings.Secure.ANDROID_ID
        )

        if (deviceId.isBlank()) deviceId = UUID.randomUUID().toString().replace("-", "")

        var verificationAttemptCount = 0

        binding.OTPVerify.setOnClickListener {
            val inputs = binding.TOPEditText.text.toString()
            verificationAttemptCount++

            if (inputs == OTPVerification) {
                showLoginStat("Registering.")
                OTPVerifyViewModel.startRegistration(
                    OTPVerificationPhoneNumber,
                    OTPVerification,
                    deviceId
                )

            } else {
                showLoginStat("Failed to verify. Try again. You have ${2 - verificationAttemptCount} try left.")
                if (verificationAttemptCount > 1) {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }
            }
        }

        binding.progressBar2.bringToFront()

        binding.TOPEditText.afterTextChanged {
            binding.OTPVerify.isEnabled = OTPVerifyViewModel.checkValidity(it)
        }

        OTPVerifyViewModel.RegistrationFormState.observe(this) { it ->

            if (it == "E") {
                showLoginStat("Error.")
            } else {
                //val t = System.currentTimeMillis().toString()
                //val s = t[t.length-1].toString()

                //val UID = UUID.randomUUID().toString().replace("-", s)
                val randoms = StringBuilder()
                (0..9).forEach { n ->
                    randoms.append((n..20).random())
                }
                prefMan.edit().putBoolean(Constants.rfc0rw2e1ra78fpwe, true)
                    .putString(Constants.SecureOTP, it)
                    .putString(Constants.UIDLogIn, deviceId)
                    .putString(getString(R.string.GroupID), randoms.substring(0, 4))
                    .putString(
                        getString(R.string.key_title_edittext_preference),
                        randoms.substring(5, 9)
                    )
                    .putBoolean(Constants.showSettings, true)
                    .putString(Constants.PhoneLogIn, OTPVerificationPhoneNumber).apply()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra(Constants.showSettings, true)
                finishAffinity()
                startActivity(intent)
            }
        }

        binding.resendOTPText.setOnClickListener {
            finish()
        }

        OTPVerifyViewModel._showProgress.observe(this) {
            if (it)
                binding.progressBar2.visibility = View.VISIBLE
            else
                binding.progressBar2.visibility = View.INVISIBLE
        }

    }


    private fun showLoginStat(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }
}