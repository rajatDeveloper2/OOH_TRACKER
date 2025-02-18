package com.oohtracker.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.hbb20.CountryCodePicker.PhoneNumberValidityChangeListener
import com.oohtracker.Constants
import com.oohtracker.databinding.ActivityLoginBinding
import com.oohtracker.ui.main.MainActivity


class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    lateinit var phoneNumber: EditText
    var num = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefMan = PreferenceManager.getDefaultSharedPreferences(this)
        val isVerified = prefMan.getBoolean(Constants.rfc0rw2e1ra78fpwe, false)
        if (isVerified) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val phoneInputView = binding.myPhoneInput

        phoneNumber = binding.phoneNumber
        val login = binding.login
        val loading = binding.loading

        loginViewModel =
            ViewModelProvider(this, LoginViewModelFactory())[LoginViewModel::class.java]

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.phoneNumberError != null) {
                phoneNumber.error = getString(loginState.phoneNumberError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginStat(loginResult.error)
            }
            if (loginResult.success != null) {
                updateUiWithUser(loginResult.success)
                showLoginStat("OTP sent to $num")
            }
            setResult(Activity.RESULT_OK)

        })

        /*phoneNumber.afterTextChanged {
            loginViewModel.loginDataChanged(
                phoneNumber.text.toString()
            )
        }*/

        /*phoneInputView.setOnValidityChange { _, isValid ->
            login.isEnabled = isValid
        }*/

        //phoneInputView.setEmptyDefault("IN")

        binding.ccp.registerCarrierNumberEditText( binding.editTextCarrierNumber)
        binding.ccp.setPhoneNumberValidityChangeListener(PhoneNumberValidityChangeListener {
            login.isEnabled = it
        })

        login.setOnClickListener {
            loading.visibility = View.VISIBLE
//            loginViewModel.phoneVerify(phoneNumber.text.toString())
//            num = phoneInputView.number.replace("+", "")
            num = binding.ccp.fullNumber.toString()
            Log.d("Tag", "phoneInputView.number: $num")

//            return@setOnClickListener
            loginViewModel.phoneVerify(num)
            login.isEnabled = false
            phoneNumber.isFocusable = false
            phoneNumber.isFocusableInTouchMode =
                false; // user touches widget on phone with touch screen
            phoneNumber.isClickable = false; // user navigates with wheel and selects widget

            val timer = object : CountDownTimer(15000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    login.text = "Resend OTP in ${millisUntilFinished / 1000} Second/s"
                }

                override fun onFinish() {
                    login.text = "Resend OTP"
                    login.isEnabled = true
                    phoneNumber.isFocusable = true
                    phoneNumber.isFocusableInTouchMode =
                        true // user touches widget on phone with touch screen
                    phoneNumber.isClickable = true
                }
            }
            timer.start()

        }

    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val intent = Intent(this, VerificationActivityPart2::class.java)
        intent.putExtra(Constants.Phone, num)
        intent.putExtra(Constants.OTPVerificationProcess, model.newOTP)
        startActivity(intent)
    }

    private fun showLoginStat(string: String) {
        Toast.makeText(applicationContext, string, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}