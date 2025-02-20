
// class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

//     val URLToVerifyP1 =
//         "https://2factor.in/API/V1/cfd6dbca-321e-11e6-b006-00163ef91450/SMS/" // +phone / otp / p2
//     val URLToVerifyP2 = "/LZOTP"

//     private val _loginForm = MutableLiveData<LoginFormState>()
//     val loginFormState: LiveData<LoginFormState> = _loginForm

//     private val _loginResult = MutableLiveData<LoginResult>()
//     val loginResult: LiveData<LoginResult> = _loginResult
//     var client: OkHttpClient = OkHttpClient()

//     fun loginDataChanged(phoneNumber: String) {
//         if (!isPhoneNumberValid(phoneNumber)) {
//             _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
//         } else {
//             _loginForm.value = LoginFormState(isDataValid = true)
//         }
//     }

//     // A placeholder password validation check
//     private fun isPhoneNumberValid(password: String): Boolean {
//         return password.length in 10..11
//     }

//     fun phoneVerify(phoneNumber: String) {

//         ////
//         //val a = generateRandomNumbers(5, 9, 1)
//         //al newOTP = generateRandomNumbers(0, a.toInt(), 4)
//         val newOTP = generateRandomNumbersNew()
//         val newURL = "$URLToVerifyP1$phoneNumber/$newOTP$URLToVerifyP2"
//         val body = "".toRequestBody(null)
//         val request: Request = Request.Builder()
//             .url(newURL)
//             .post(body)
//             .build()
//         var decodeString = ""
//         viewModelScope.launch {

//             withContext(Dispatchers.IO) {

//                 try {

//                     client.newCall(request).execute().use { response ->
//                         decodeString = response.body!!.string()
//                         Log.d("Tag", "$decodeString $newOTP")

//                     }
//                     try {
//                         val obj = JSONObject(decodeString)
//                         val status = obj.getString("Status")
//                         Log.d("My App", status) //!"Error"
//                         if (!status.equals("Error")) {
//                             _loginResult.postValue(
//                                 LoginResult(success = LoggedInUserView(newOTP = newOTP))
//                             )
//                         } else
//                             _loginResult.postValue(LoginResult(error = obj.getString("Details")))

//                     } catch (t: Throwable) {
//                         Log.e("My App", "Could not parse malformed JSON: \"$decodeString\"")
//                     }
//                 }catch (e:Exception){
//                     e.printStackTrace()
//                     _loginResult.postValue(LoginResult(error = "Error"))

//                 }
//             }

//         }


//     }
