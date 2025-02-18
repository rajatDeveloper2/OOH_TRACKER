package com.oohtracker.ui.main.ViewModels

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oohtracker.Constants
import com.oohtracker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.*


class MainVM : ViewModel() {
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

    fun getMapImage() {
        val url =
            "https://maps.googleapis.com/maps/api/staticmap?key=AIzaSyCIXRxjPHdKKMUs8pm1R8_3iyTAXQ4J5oU&sensor=false&center=28.58756,77.39678&zoom=15&**size=1280x600**&markers=color:green|label:X|28.58756,77.39678|.png"
    }


    fun manageToken(prefMan: SharedPreferences) {

        viewModelScope.launch {

            val client = OkHttpClient()
            val otp = prefMan.getString(Constants.SecureOTP, "")
            val UUID = prefMan.getString(Constants.UIDLogIn, "")
            val phone = prefMan.getString(Constants.PhoneLogIn, "")

            val newURL = Constants.BaseURL + "api/token"
            Log.d("Tag", "manageToken otp: $otp") //

            ///
            // create your json here
            val jsonObject = JSONObject()
            try {
                jsonObject.put("otp", otp)
                jsonObject.put("phone", phone)
                jsonObject.put("uuid", UUID)

                // put your json here
                val body: RequestBody = jsonObject.toString().toRequestBody(JSON)
                ///
                val request: Request = Request.Builder()
                    .url(newURL)
                    .post(body)
                    .build()
                var decodeString = ""
                Log.d("Tag", "manageToken body: $body , $newURL , $phone") //

                withContext(Dispatchers.IO) {

                    client.newCall(request).execute().use { response ->
                        decodeString = response.body!!.string()
                    }
                    try {
                        Log.d("Tag", "manageToken decodeString: $decodeString") //
                        val obj = JSONObject(decodeString)
                        val token = obj.getString("token")
                        Log.d("Tag", token) //
                        if (!token.equals("")) {
                            prefMan.edit().putString(Constants.Token, token).apply()
                        } else {
                            Log.e("Tag", "Error")
                        }
                    } catch (t: Throwable) {
                        Log.e("Tag", "Could not parse malformed JSON: \"$decodeString\"")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    @SuppressLint("MissingPermission")
    fun isInternetConnected(connectivityManager: ConnectivityManager?): Boolean {
        var status = false
        if (connectivityManager != null) {
            if (connectivityManager.activeNetwork != null && connectivityManager.getNetworkCapabilities(
                    connectivityManager.activeNetwork
                ) != null
            ) {
                // connected to the internet
                status = true
            }
        }
        return status
    }

    //////////////////////////////////////////////////////////

    /// @param folderName can be your app's name
    suspend fun saveImage(
        bitmap: Bitmap,
        context: Context,
        fileName: String,
        longitude: String,
        latitude: String,
        isMap: Boolean
    ): String {
        if (android.os.Build.VERSION.SDK_INT >= 29) {

            if (!isMap) {
                val values = contentValues(fileName, longitude, latitude, isMap)

                values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/" + context.getString(R.string.app_name)
                )

                values.put(MediaStore.Images.Media.IS_PENDING, true)
                // RELATIVE_PATH and IS_PENDING are introduced in API 29.

                val uri: Uri? =
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
                        .also { uri ->
                            if (uri != null) {


                                if (uri != null) {
                                    saveImageToStream(
                                        bitmap,
                                        context.contentResolver.openOutputStream(uri)
                                    )
                                    values.put(MediaStore.Images.Media.IS_PENDING, false)
//                                if (!isMap)
                                    context.contentResolver.update(uri, values, null, null)

                                }


                                //val split = file.path.split(":".toRegex()).toTypedArray() //split the path.


                                val path = "$uri-$fileName"

                                //test accessing the file using this path or anything that can be saved as a string.
                                //val file = File(path)
                                Log.d("Tag", "v--- uri.path - ${uri.path}, \n $path")

                                return path //save this string for later use.


                            }
                        }
            } else {

                val filesDir = context.filesDir
                val rpi = File(filesDir, context.getString(R.string.app_name))
                if (!rpi.exists()) (rpi.mkdirs())

                val fileMap = File(rpi, fileName)
                val stream: OutputStream = FileOutputStream(fileMap)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 99, stream)
                stream.flush()
                stream.close()

                return fileMap.absolutePath + "-" + fileMap.name

            }


        } else {
            val directory = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/" + context.getString(R.string.app_name)
            )
            // getExternalStorageDirectory is deprecated in API 29

            if (!directory.exists()) {
                directory.mkdirs()
            }
            //val fileName = System.currentTimeMillis().toString() + ".jpg"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            if (file.absolutePath != null) {
                val values = contentValues(fileName, longitude, latitude, isMap)
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                // .DATA is deprecated in API 29
                if (!isMap)
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )
            }
            Log.d(
                "Tag",
                "vals: absolutePath - ${file.absolutePath}, ${file.path} , ${directory.absolutePath}, ${directory.path} , ${file.name}"
            )
            return file.absolutePath
        }
        return ""
    }

    private fun contentValues(
        fileName: String,
        longitude: String,
        latitude: String,
        isMap: Boolean
    ): ContentValues {
        val values = ContentValues()
        if (!isMap)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")


        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.ImageColumns.LONGITUDE, longitude)
        values.put(MediaStore.Images.ImageColumns.LATITUDE, latitude)
        values.put(MediaStore.Images.ImageColumns.DESCRIPTION, ("lat: $latitude , lon: $longitude"))
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}