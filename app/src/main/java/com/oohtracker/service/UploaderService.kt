package com.oohtracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.oohtracker.Constants
import com.oohtracker.MainApplication
import com.oohtracker.R
import com.oohtracker.networking.ApiService
import com.oohtracker.room.FileDataViewModel
import com.oohtracker.room.Word
import com.oohtracker.ui.main.MainActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


class UploaderService : LifecycleService() {
    private var fileDataViewModel: FileDataViewModel? = null
    private val NOTIFICATION_ID = 1248_1632
    lateinit var prefMan: SharedPreferences
    var apiService: ApiService? = null
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    private var words: List<Word>? = null
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var builder: NotificationCompat.Builder
    private var managingToken = false
    private val wordsThatAreBeingUploaded = mutableListOf<String>()

    private fun createNotification(context: Context): Notification {
        builder =
            NotificationCompat.Builder(context, MainApplication.PRIMARY_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
        val intent: Intent = Intent(context, MainActivity::class.java)
        builder.setContentTitle(context.getString(R.string.settings_status_on_summary))
            .setTicker(context.getString(R.string.notification_status)).color =
            ContextCompat.getColor(context, R.color.black)
        val flags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, flags))
        return builder.build()
    }

/*

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
*/

    override fun onCreate() {
        super.onCreate()
//        Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
        Log.d("Tag", "Service started")
        notificationManager = NotificationManagerCompat.from(this)

        startForeground(
            NOTIFICATION_ID,
            createNotification(this)
        )

        fileDataViewModel = MainApplication.getFileDataViewModel()
        prefMan = PreferenceManager.getDefaultSharedPreferences(this)

        prefMan.edit()?.putBoolean(Constants.ServiceRunning, true)?.apply()

        val client = OkHttpClient.Builder().build()
        apiService =
            Retrofit.Builder().baseUrl(Constants.BaseURL).client(client).build().create(
                ApiService::class.java
            )

        fileDataViewModel?.allWords?.observe(this) { root ->
            if (root != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    words = root
                    startUploading()
                }
            }
        }
//        startUploading()

    }

    private fun startUploading() {
        /*words?.forEach { word ->
        }*/

//        words = fileDataViewModel?.allWords?.value


        //
        val size = words?.size
        Log.d("Tag", "started Uploading...size: $size , ... ${words?.size}")

        if (size != null) {
            if (size > 0) {
                builder.setContentText("Upload progress remains for ${words?.size} files.")

                val wordToCheck = words?.get(0)
                if (wordToCheck != null && !wordsThatAreBeingUploaded.contains(wordToCheck.word)) {
                    wordsThatAreBeingUploaded.add(wordToCheck.word)
                    wordToCheck.let {
                        if (Build.VERSION.SDK_INT >= 29) {
                            multipartImageUploadWithUri(
                                wordToCheck.word,
                                wordToCheck.wordMeaning
                            )
                        } else
                            multipartImageUpload(
                                wordToCheck.word,
                                wordToCheck.wordMeaning
                            )
                    }
                } else {
                    words?.forEach {
                        if (!wordsThatAreBeingUploaded.contains(it.word)) {
                            it.let {

                                wordsThatAreBeingUploaded.add(it.word)

                                if (Build.VERSION.SDK_INT >= 29) {
                                    multipartImageUploadWithUri(
                                        it.word,
                                        it.wordMeaning
                                    )
                                } else {
                                    multipartImageUpload(
                                        it.word,
                                        it.wordMeaning
                                    )
                                }
                                return@forEach
                            }
                        }
                    }
                }
            } else {
                builder.setContentText("Uploaded all files.")
                Log.d("Tag", "reschedule or what?...size: $size , ... ${words?.size}")

                initiateStopSequenceOn0()

            }
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build())

    }

    private fun initiateStopSequence() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(1500)
//            if (fileDataViewModel?.allWords?.value?.size == 0)
            stopSelf()

        }
    }

    private fun initiateStopSequenceOn0() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(8000)
            if (fileDataViewModel?.allWords?.value?.size == 0) {

                WorkManager.getInstance(baseContext).cancelUniqueWork("periodicUploadWorker")

                stopSelf()
            }
            else
                startUploading()
        }
    }

    private fun multipartImageUpload(currentFile: String, messageToUpload: String) {
        try {

            val file = File(currentFile)

            //
            val reqFile: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("upload", file.name, reqFile)
            val name = "upload".toRequestBody("text/plain".toMediaTypeOrNull())
            val token = prefMan.getString(Constants.Token, "token")


            val message = messageToUpload
                .toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

            val req: Call<ResponseBody?>? =
                apiService!!.postImage("Bearer $token", name, message, body)

            Log.d("Tag", "uploading file...")

            req?.enqueue(object : retrofit2.Callback<ResponseBody?> {
                override fun onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    Log.d("Tag", "token: $token, resp: ${response.body()}")
                    when {
                        response.code() == 200 -> {
                            Log.d("Tag", "Uploaded Successfully!")
                            /*Toast.makeText(
                                applicationContext, "Uploaded Successfully!",
                                Toast.LENGTH_SHORT
                            ).show()*/

                            /* builder.setContentText( "Uploaded ${words?.size} files."  )

                             val cStat = words?.size?.minus(1)
                             if (cStat == 0)
                                 builder.setContentText( "Uploaded All files."  )

                             notificationManager.notify(NOTIFICATION_ID, builder.build())*/


//                           if Map
                            val isMap = file.name.split("_")[1] == "Map"


                            if (isMap) {
                                file.delete()
                                Log.d("Tag", "deleting a map")
                                //content res del

                                //contentResolver.delete(Uri.fromFile(file), null, null)

                            }

                            //wordsThatAreBeingUploaded.remove(currentFile)

                            fileDataViewModel?.delete(currentFile).also { ////
                                //startUploading()
                                /*val size = fileDataViewModel?.allWords?.value?.size
                                if (size != null) {
                                    startUploading()

//                                    if (size  != 0) {
//                                        builder.setContentText("Uploaded all files.")
//                                        notificationManager.notify(NOTIFICATION_ID, builder.build())

                            //                                    Toast.makeText(this, "No more uploads left.", Toast.LENGTH_LONG).show()
//                                        stopSelf()
//                                        startUploading()

//                                    }
                                }*/
                            }


                        }
                        response.code() == 449 -> {
                            if (!managingToken)
                                manageToken(true, currentFile, messageToUpload)
                            Log.d("Tag", "resp: 449")

                        }
                        else -> {
                            Log.d("Tag", "issue:" + response.code())

                            builder.setContentText(
                                response.code()
                                    .toString() + " , Error while uploading files. Please try again later."
                            )
                            notificationManager.notify(NOTIFICATION_ID, builder.build())

                            Toast.makeText(
                                applicationContext,
                                response.code().toString() + " ",
                                Toast.LENGTH_SHORT
                            ).show()

                            wordsThatAreBeingUploaded.remove(currentFile)

                        }
                    }

                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    Toast.makeText(applicationContext, "Request failed", Toast.LENGTH_SHORT).show()
                    t.printStackTrace()

                    builder.setContentText("Error occurred. Uploader will exit.")
                    notificationManager.notify(NOTIFICATION_ID, builder.build())

                    initiateStopSequence()

                }

            })
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }


    }

    fun manageToken(reSchedule: Boolean, currentFile: String, messageToUpload: String) {

        CoroutineScope(Dispatchers.IO).launch {
            managingToken = true
            val client = OkHttpClient()
            val otp = prefMan.getString(Constants.SecureOTP, "")
            val UUID = prefMan.getString(Constants.UIDLogIn, "")
            val newURL = Constants.BaseURL + "api/token"
            val phone = prefMan.getString(Constants.PhoneLogIn, "").toString()
            Log.d("Tag", "manageToken otp: $otp") //

            /*CoroutineScope(Dispatchers.Main).launch {

                Toast.makeText(
                    applicationContext,
                    "Phone to send on server: $phone",
                    Toast.LENGTH_SHORT
                ).show()
            }*/
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

                            if (reSchedule) {
                                if (Build.VERSION.SDK_INT >= 29) {

                                    multipartImageUploadWithUri(currentFile, messageToUpload)
                                    Log.d("Tag", "reScheduling...")
                                } else {
                                    multipartImageUpload(currentFile, messageToUpload)

                                }
                            } else {
                                Log.d("Tag", "Done...")
                            }
                        } else {
                            Log.e("Tag", "Error")
                        }
                    } catch (t: Throwable) {
                        Log.e("Tag", "Could not parse malformed JSON: \"$decodeString\"")
                    }
                    managingToken = false

                }
            } catch (e: Exception) {
                e.printStackTrace()
                managingToken = false

            }


        }

    }


    private fun multipartImageUploadWithUri(currentFile: String, messageToUpload: String) {
        try {

            val parts = currentFile.split("-")
            val theUri = Uri.parse(parts[0])
            val isMap = parts[1].split("_")[1] == "Map"

            if (!isMap) {
                //val file: File?
                //file = File(currentFile)
                //val reqFileX: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                /*contentResolver.openInputStream(Uri.parse(currentFile)).use {

            }
            val body1 = MultipartBody.Builder()
            body1.addFormDataPart(
                "image/".toMediaTypeOrNull().toString(),
                "test.jpeg",
                ContentUriRequestBody(contentResolver, theUri)
            )
            val bodyToSend = body1.build() // send this object to your retrofit instance

            Log.d(
                "Tag",
                "v--- service - filePath - ${file.path}, isFile ${file.isFile}}"
            )*/
                val fileName_ = parts[1]
                //return
                //
                //val reqFile: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData(
                    "upload",
                    fileName_,
                    ContentUriRequestBody(contentResolver, theUri)
                )
                val name = "upload".toRequestBody("text/plain".toMediaTypeOrNull())
                val token = prefMan.getString(Constants.Token, "")


                val message = messageToUpload
                    .toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

                val req: Call<ResponseBody?>? =
                    apiService!!.postImage("Bearer $token", name, message, body)

                Log.d("Tag", "uploading file...")

                req?.enqueue(object : retrofit2.Callback<ResponseBody?> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        Log.d("Tag", "token: $token, resp: ${response.body()}")
                        when {
                            response.code() == 200 -> {
                                Log.d("Tag", "Uploaded Successfully!")
                                /*Toast.makeText(
                                applicationContext, "Uploaded Successfully!",
                                Toast.LENGTH_SHORT
                            ).show()*/

                                /* builder.setContentText( "Uploaded ${words?.size} files."  )

                             val cStat = words?.size?.minus(1)
                             if (cStat == 0)
                                 builder.setContentText( "Uploaded All files."  )

                             notificationManager.notify(NOTIFICATION_ID, builder.build())*/


//                           if Map


                                /*if (isMap) {

                                    Log.d("Tag", "deleting a map $theUri")
                                    //content res del
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        try {
                                            contentResolver.delete(theUri, null)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                }*/
                                //wordsThatAreBeingUploaded.remove(currentFile)

                                fileDataViewModel?.delete(currentFile).also {

                                    /* val size = fileDataViewModel?.allWords?.value?.size
                                     if (size != null) {
                                         *//*   if (size  == 0) {
                                               builder.setContentText("Uploaded all files.")
                                               notificationManager.notify(NOTIFICATION_ID, builder.build())

                                               //                                    Toast.makeText(this, "No more uploads left.", Toast.LENGTH_LONG).show()
   //                                            stopSelf()

                                           }
                                       }*//*
                                        startUploading()

                                    }*/
                                }
                            }
                            response.code() == 449 -> {
//                            currentFile.path?.let { manageToken(true, it, messageToUpload) }
                                if (!managingToken)
                                    manageToken(true, currentFile, messageToUpload)
                                Log.d("Tag", "resp: 449")

                            }
                            else -> {
                                Log.d("Tag", "issue:" + response.code())

                                builder.setContentText(
                                    response.code()
                                        .toString() + " , Error while uploading files. Please try again later."
                                )
                                notificationManager.notify(NOTIFICATION_ID, builder.build())
                                wordsThatAreBeingUploaded.remove(currentFile)

                                Toast.makeText(
                                    applicationContext,
                                    response.code().toString() + " ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        Toast.makeText(applicationContext, "Request failed", Toast.LENGTH_SHORT)
                            .show()
                        t.printStackTrace()

                        builder.setContentText("Error occurred. Uploader will exit.")
                        notificationManager.notify(NOTIFICATION_ID, builder.build())

                        initiateStopSequence()
                    }

                })
            } else {
                //map file upload

                val file = File(parts[0])

                //
                val reqFile: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("upload", file.name, reqFile)
                val name = "upload".toRequestBody("text/plain".toMediaTypeOrNull())
                val token = prefMan.getString(Constants.Token, "token")


                val message = messageToUpload
                    .toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

                val req: Call<ResponseBody?>? =
                    apiService!!.postImage("Bearer $token", name, message, body)

                Log.d("Tag", "uploading file...")

                req?.enqueue(object : retrofit2.Callback<ResponseBody?> {
                    override fun onResponse(
                        call: Call<ResponseBody?>,
                        response: Response<ResponseBody?>
                    ) {
                        Log.d("Tag", "token: $token, resp: ${response.body()}")
                        when {
                            response.code() == 200 -> {
                                Log.d("Tag", "Uploaded Successfully!")

                                try {

                                    file.delete()
                                    Log.d("Tag", "deleting a map")
                                    //content res del
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                // wordsThatAreBeingUploaded.remove(currentFile)

                                fileDataViewModel?.delete(currentFile).also { ////
                                    /* val size = fileDataViewModel?.allWords?.value?.size
                                     if (size != null) {
                                         *//*  if (size  == 0) {
                                            builder.setContentText("Uploaded all files.")
                                            notificationManager.notify(NOTIFICATION_ID, builder.build())

                                            //                                    Toast.makeText(this, "No more uploads left.", Toast.LENGTH_LONG).show()
//                                            stopSelf()

                                        }
                                    }*//*
                                        startUploading()

                                    }*/
                                }


                            }
                            response.code() == 449 -> {
                                if (!managingToken)
                                    manageToken(true, currentFile, messageToUpload)
                                Log.d("Tag", "resp: 449")

                            }
                            else -> {
                                Log.d("Tag", "issue:" + response.code())

                                builder.setContentText(
                                    response.code()
                                        .toString() + " , Error while uploading files. Please try again later."
                                )
                                notificationManager.notify(NOTIFICATION_ID, builder.build())

                                wordsThatAreBeingUploaded.remove(currentFile)

                                Toast.makeText(
                                    applicationContext,
                                    response.code().toString() + " ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }

                    override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                        Toast.makeText(applicationContext, "Request failed", Toast.LENGTH_SHORT)
                            .show()
                        t.printStackTrace()

                        builder.setContentText("Error occurred. Uploader will exit.")
                        notificationManager.notify(NOTIFICATION_ID, builder.build())

                        initiateStopSequence()
                    }

                })

            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }


    }

/*

    fun readFile(){
        val contentUri = MediaStore.Files.getContentUri("external")

        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"

        val selectionArgs = arrayOf(Environment.DIRECTORY_DOCUMENTS + "/Kamen Rider Decade/")

        val cursor = contentResolver.query(contentUri, null, selection, selectionArgs, null)

        var uri: Uri? = null

        if (cursor!!.count == 0) {
            Toast.makeText(
                this,
                "No file found in \"" + Environment.DIRECTORY_DOCUMENTS + "/Kamen Rider Decade/\"",
                Toast.LENGTH_LONG
            ).show()
        } else {
            while (cursor!!.moveToNext()) {
                val fileName =
                    cursor!!.getString(cursor!!.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                if (fileName == "menuCategory.txt") {
                    val id = cursor!!.getLong(cursor!!.getColumnIndex(MediaStore.MediaColumns._ID))
                    uri = ContentUris.withAppendedId(contentUri, id)
                    break
                }
            }
            if (uri == null) {
                Toast.makeText(
                    this,
                    "\"menuCategory.txt\" not found",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val size: Int = inputStream.available()
                    val bytes = ByteArray(size)
                    inputStream.read(bytes)
                    inputStream.close()
                    val jsonString = String(bytes, StandardCharsets.UTF_8)
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle("File Content")
                    builder.setMessage(jsonString)
                    builder.setPositiveButton("OK", null)
                    builder.create().show()
                } catch (e: IOException) {
                    Toast.makeText(view.getContext(), "Fail to read file", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
*/


    class ContentUriRequestBody(
        private val contentResolver: ContentResolver,
        private val contentUri: Uri
    ) : RequestBody() {

        override fun contentType(): MediaType? {
            val contentType = contentResolver.getType(contentUri)
            return contentType?.toMediaTypeOrNull()
        }

        override fun contentLength(): Long {
            val size = contentUri.length(contentResolver)
            return size
        }

        override fun writeTo(bufferedSink: BufferedSink) {
            val inputStream = contentResolver.openInputStream(contentUri)
                ?: throw IOException("Couldn't open content URI for reading")
            inputStream.source().use { source ->
                bufferedSink.writeAll(source)
            }
        }

        fun Uri.length(contentResolver: ContentResolver)
                : Long {

            val assetFileDescriptor = try {
                contentResolver.openAssetFileDescriptor(this, "r")
            } catch (e: FileNotFoundException) {
                null
            }
            // uses ParcelFileDescriptor#getStatSize underneath if failed
            val length = assetFileDescriptor?.use { it.length } ?: -1L
            if (length != -1L) {
                return length
            }

            // if "content://" uri scheme, try contentResolver table
            if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                return contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        // maybe shouldn't trust ContentResolver for size: https://stackoverflow.com/questions/48302972/content-resolver-returns-wrong-size
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex == -1) {
                            return@use -1L
                        }
                        cursor.moveToFirst()
                        return try {
                            cursor.getLong(sizeIndex)
                        } catch (_: Throwable) {
                            -1L
                        }
                    } ?: -1L
            } else {
                return -1L
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

//        val thisRunning = prefMan.getBoolean(Constants.ServiceRunning, true)
//        if (thisRunning)
        prefMan.edit()?.putBoolean(Constants.ServiceRunning, false)?.apply()
//        Toast.makeText(this, "Service stopped.", Toast.LENGTH_SHORT).show()
        wordsThatAreBeingUploaded.clear()
    }
}
