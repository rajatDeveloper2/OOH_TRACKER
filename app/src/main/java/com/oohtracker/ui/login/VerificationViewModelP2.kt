package com.oohtracker.ui.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.oohtracker.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject


class VerificationViewModelP2(application: Application) : AndroidViewModel(application) {
    private val _registrationForm = MutableLiveData<String>()
    val _showProgress = MutableLiveData<Boolean>()
    val RegistrationFormState: LiveData<String> = _registrationForm
    var client: OkHttpClient = OkHttpClient()

    fun checkValidity(it: String): Boolean {
        return it.length > 3
    }

    fun startRegistration(phone: String, num: String, uuid: String){
        val JSON: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
        ////
        val jsonObject = JSONObject()
        try {
            jsonObject.put("phone", phone)
            jsonObject.put("otp", num)
            jsonObject.put("uuid", uuid)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val body = jsonObject.toString().toRequestBody(JSON)

        val request: Request = Request.Builder()
            .url(Constants.BaseURL+"api/registerOTP/")
            .post(body)
            .build()
        var decodeString = ""

        _showProgress.postValue(true)

        viewModelScope.launch {

            withContext(Dispatchers.IO) {
                try {

                    client.newCall(request).execute().use { response ->
                        decodeString = response.body!!.string()
                        //Log.d("My App", "$decodeString $newOTP")
                        if (response.code != 201){
                            _registrationForm.postValue("E")
                        }

                    }
                    try {
                        val obj = JSONObject(decodeString)
                        val secureOTP = obj.getString("secureOTP")
                        Log.d("My App", secureOTP) //!"Error"
                        if (secureOTP.equals("")) {
                            _registrationForm.postValue("E")
                        } else
                            _registrationForm.postValue(secureOTP)

                        _showProgress.postValue(false)

                    } catch (t: Throwable) {
                        Log.e("My App", "Could not parse malformed JSON: \"$decodeString\"")
                        _showProgress.postValue(false)
                        _registrationForm.postValue("E")

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _showProgress.postValue(false)

                }
            }

        }
    }

}