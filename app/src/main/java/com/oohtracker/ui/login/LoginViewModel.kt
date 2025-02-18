package com.oohtracker.ui.login

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oohtracker.R
import com.oohtracker.data.LoginRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.SecureRandom
import java.util.*
import kotlin.math.ceil


class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    val URLToVerifyP1 =
        "https://2factor.in/API/V1/cfd6dbca-321e-11e6-b006-00163ef91450/SMS/" // +phone / otp / p2
    val URLToVerifyP2 = "/LZOTP"

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult
    var client: OkHttpClient = OkHttpClient()

    fun loginDataChanged(phoneNumber: String) {
        if (!isPhoneNumberValid(phoneNumber)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    // A placeholder password validation check
    private fun isPhoneNumberValid(password: String): Boolean {
        return password.length in 10..11
    }

    fun phoneVerify(phoneNumber: String) {

        ////
        //val a = generateRandomNumbers(5, 9, 1)
        //al newOTP = generateRandomNumbers(0, a.toInt(), 4)
        val newOTP = generateRandomNumbersNew()
        val newURL = "$URLToVerifyP1$phoneNumber/$newOTP$URLToVerifyP2"
        val body = "".toRequestBody(null)
        val request: Request = Request.Builder()
            .url(newURL)
            .post(body)
            .build()
        var decodeString = ""
        viewModelScope.launch {

            withContext(Dispatchers.IO) {

                try {

                    client.newCall(request).execute().use { response ->
                        decodeString = response.body!!.string()
                        Log.d("Tag", "$decodeString $newOTP")

                    }
                    try {
                        val obj = JSONObject(decodeString)
                        val status = obj.getString("Status")
                        Log.d("My App", status) //!"Error"
                        if (!status.equals("Error")) {
                            _loginResult.postValue(
                                LoginResult(success = LoggedInUserView(newOTP = newOTP))
                            )
                        } else
                            _loginResult.postValue(LoginResult(error = obj.getString("Details")))

                    } catch (t: Throwable) {
                        Log.e("My App", "Could not parse malformed JSON: \"$decodeString\"")
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    _loginResult.postValue(LoginResult(error = "Error"))

                }
            }

        }


    }

    private fun generateRandomNumbers(min: Int, max: Int, randomNumberCount: Int): String {
        // min & max will be changed as per your requirement. In my case, I've taken min = 2 & max = 32

        var dif = max - min
        if (dif < randomNumberCount * 3) {
            dif = randomNumberCount * 3
        }
        val margin = ceil((dif.toFloat() / randomNumberCount).toDouble()).toInt()
        //val randomNumberList: MutableList<Int> = ArrayList()
        val stringBuilder = StringBuilder(randomNumberCount)
        val random = Random()
        for (i in 0 until randomNumberCount) {
            val range = margin * i + min // 2, 5, 8
            var randomNum: Int = random.nextInt(margin)
            if (randomNum == 0) {
                randomNum = 1
            }
            val number = randomNum + range
            //randomNumberList.add(number)
            stringBuilder.append(number)
        }

        return stringBuilder.toString()

    }

    private fun generateRandomNumbersNew(): String{

        val randoms = StringBuilder()
        (0..16).forEach {
            randoms.append ((it..17).random())
        }
        //val randoms = (0..100).random()

        val random = SecureRandom()
        val beginIndex: Int =
            random.nextInt((6..(randoms.length - 6)).random()) //Begin index + length of your string < data length

        val endIndex = beginIndex + 4 //Length of string which you want


        return randoms.substring(beginIndex, endIndex)

    }
}