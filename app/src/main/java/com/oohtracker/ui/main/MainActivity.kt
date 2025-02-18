package com.oohtracker.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.ExifInterface
import android.media.MediaActionSound
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import androidx.work.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.oohtracker.Constants
import com.oohtracker.MainApplication
import com.oohtracker.R
import com.oohtracker.networking.ApiService
import com.oohtracker.room.FileDataViewModel
import com.oohtracker.room.Word
import com.oohtracker.service.UploaderService
import com.oohtracker.ui.login.LoginActivity
import com.oohtracker.ui.main.ViewModels.MainVM
import com.oohtracker.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var heightPercentage: Int = 20
    private lateinit var lastLocation: Location
    private val MinTime: Long = 1000L
    private val MinDistance: Float = 1f
    private var gesturesEnabled = false

    //var locationManager: LocationManager? = null
    //var locationListener: LocationListener? = null
    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var mLocationRequest: LocationRequest

    //var mLastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback

    lateinit var textView: TextView
    lateinit var textViewProxy: TextView
    lateinit var textViewProxyContainer: LinearLayout
    lateinit var camera_capture_button: Button
    lateinit var viewFinder: PreviewView
    lateinit var infoContainer: LinearLayout
    lateinit var rootView: View
    lateinit var progressBar: ProgressBar

    //cam
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var additionalDataDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    //map
    var mGoogleMap: GoogleMap? = null
    var mapFrag: SupportMapFragment? = null
    var mCurrLocationMarker: Marker? = null
    lateinit var imageView: ImageView
    lateinit var previewImage: ImageView
    lateinit var imageBitmap: Bitmap
    lateinit var scaledOriginalBitmap: Bitmap
    var currentImageOrientation: Int = 0
    lateinit var mapImageBitmap: Bitmap
    val df: DateFormat = SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss")
    lateinit var flashButton: Button
    lateinit var buttonSettings: Button

    ////////
    var CampaignID = "0"

    var MAX_ALLOWED_RESOLUTION = 1440
    var previous_MAX_ALLOWED_RESOLUTION = 1440

    val locationTextPeriodic = MutableLiveData<String>()

    var notesSaved = ""
    var locationText = ""
    var locationTextPrevious = ""
    var notesIndicator = "Notes:"
    var lastSavedImageDirectory = ""
    var apiService: ApiService? = null

    val bigJsonObject = JSONObject()

    var mapImageFileName = ""
    var phone = ""
    var timeText = ""
    var serviceRunning = false
    var MapZoomLevel = 16
    var PreviousMapZoomLevel = 16
    var PreviousMapType = 0
    var uploaderBehavior = 1
    var PreviousCameraType = 0
    var CameraType = 0
    var shouldCheckForCamera = false

    private var fileDataViewModel: FileDataViewModel? = null
    var isTitleBarVisible = true
    var infoContainerVisible = true
    var mapHasBeenReady = false
    var fromLaunch = true
    lateinit var audio: AudioManager

    private val timer = object : CountDownTimer(10000, 2000) {
        override fun onTick(millisUntilFinished: Long) {
            try {
                mGoogleMap?.snapshot {
                    imageView.setImageBitmap(it)
                }

                Log.d("Tag", "timer running.")

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onFinish() {
        }

    }


    //    Timer works fine. Here, I use Timer to search text after 1.5s and update UI. Hope that helps.
    private lateinit var timerForText: Unit

    lateinit var mainHandler: Handler
    private val updateTextTask = object : Runnable {
        override fun run() {
            val date: String = df.format(Date())
            locationText =
                "$locationTextPrevious \nTime: $date \nCampaign ID: $CampaignID"
            CoroutineScope(Dispatchers.Main).launch {
                textView.text = locationText
            }
            // Log.d("Tag", "Hello, time:$date")
            mainHandler.postDelayed(this, 1000)
        }
    }

    /*
    _timerForText  = Timer().schedule(60*1000){
        val date: String = df.format(Date())

         locationText = "$locationTextPrevious\nTime: $date"
    }*/

    val param = LinearLayout.LayoutParams( /*width*/
        ViewGroup.LayoutParams.WRAP_CONTENT,  /*height*/
        ViewGroup.LayoutParams.WRAP_CONTENT,  /*weight*/
        1.0f
    )
    lateinit var cameraControl: CameraControl
    private var flashFlag: Int = 0

    lateinit var prefMan: SharedPreferences

    //var exif: ExifInterface? = null
    var connectivityManager: ConnectivityManager? = null
    lateinit var LocationManagerService: LocationManager

    private val mainVM: MainVM by viewModels()
    var showSettings = false

    // Create a Constraints that defines when the task should run
    var uploaderConstraints: Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /*val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)*/
//        toolbar.setBackgroundColor(Color.TRANSPARENT)
//        supportActionBar?.setBackgroundDrawable(AppCompatResources.getDrawable(this, R.drawable.gradient))
        /* val bar = supportActionBar
         bar?.setBackgroundDrawable(ColorDrawable(Color.YELLOW))
         */

        prefMan = PreferenceManager.getDefaultSharedPreferences(this)
        Log.d("Tag", "ID:$CampaignID , $MAX_ALLOWED_RESOLUTION")
        showSettings = prefMan.getBoolean(Constants.showSettings, false)
        if (showSettings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            prefMan.edit().putBoolean(Constants.showSettings, false).apply()
        }
        fileDataViewModel = MainApplication.getFileDataViewModel()

        ///
        phone = prefMan.getString(Constants.PhoneLogIn, "").toString()
        //MapZoomLevel = prefMan.getString(Constants.MapZoomLevel, "16")?.toInt() ?: 16
        if (phone.isBlank()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
        LocationManagerService = getSystemService(LOCATION_SERVICE) as LocationManager
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        textView = findViewById(R.id.textView)
        textViewProxy = findViewById(R.id.textViewProxy)
        textViewProxyContainer = findViewById(R.id.textViewProxyContainer)
        camera_capture_button = findViewById(R.id.camera_capture_button)
        viewFinder = findViewById(R.id.viewFinder)
        imageView = findViewById(R.id.map_proxy)
        infoContainer = findViewById(R.id.infoContainer)
        rootView = findViewById(R.id.rootView)
        progressBar = findViewById(R.id.progressBar)
        previewImage = findViewById(R.id.previewImage)
        imageBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        scaledOriginalBitmap = imageBitmap
//        mapImageBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        previewImage.visibility = View.INVISIBLE

        ///
        /*if (MAX_ALLOWED_RESOLUTION < 1920) heightPercentage = 26
        else
            heightPercentage = 20*/
        param.height = ((MAX_ALLOWED_RESOLUTION * heightPercentage) / 100)
        param.width = ((MAX_ALLOWED_RESOLUTION * 70) / 100)
        textViewProxyContainer.layoutParams = param
        textViewProxyContainer.requestLayout()
        textViewProxyContainer.setBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.text_background
            )
        )
        ///


        viewFinder.setOnLongClickListener {
            toggleInfoBar()
            true
        }

        flashButton = findViewById(R.id.buttonFlash)
        buttonSettings = findViewById(R.id.buttonSettings)
        flashButton.bringToFront()
        buttonSettings.bringToFront()
        flashButton.setOnClickListener {
            cycleBetweenFlashModes()
        }
        buttonSettings.setOnClickListener {
            val i = Intent(this, SettingsActivity::class.java)
            startActivity(i)
        }

        connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        mainHandler = Handler(Looper.getMainLooper())
        /* Timer().schedule(object : TimerTask() {
             override fun run() {
                if (isTitleBarVisible)
                    toggleTitleBar()
             }
         }, 4000)*/

        ///

        toggleTitleBar()

        locationTextPeriodic.observe(this) {
            textView.text = it
        }
        //val optionsButton: Button = findViewById(R.id.buttonOptionsMenu)
        //optionsButton.visibility = View.INVISIBLE
        /*optionsButton.setOnClickListener {
            showPopup(it)
        }*/
        ///
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.ACCESS_NETWORK_STATE,
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) { /* ... */
                    setup()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken?
                ) { /* ... */
                    Toast.makeText(
                        this@MainActivity,
                        "All permissions required for core features.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }).check()
    }

    fun getDateTimeInUTC(): String {
        val dateInMillis = System.currentTimeMillis()

        val format = "yyyy-MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(format, Locale.getDefault())

        return sdf.format(Date(dateInMillis))

    }

    private fun cycleBetweenFlashModes() {
        if (imageCapture == null) return
        flashFlag++
        if (flashFlag > 2) flashFlag = 0
        Log.d("Tag", "FlashMode: $flashFlag")

        when (flashFlag) {
            0 -> {
                flashButton.setBackgroundResource(R.drawable.ic_baseline_flash_auto_24)
                cameraControl.enableTorch(false)
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
            }
            1 -> {
                flashButton.setBackgroundResource(R.drawable.ic_baseline_flash_on_24)
                cameraControl.enableTorch(true)
//                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
            }
            2 -> {
                flashButton.setBackgroundResource(R.drawable.ic_baseline_flash_off_24)
                cameraControl.enableTorch(false)
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
            }
        }


    }

    fun toggleInfoBar() {
        // toggleTitleBar()

        infoContainerVisible = !infoContainerVisible
        if (!infoContainerVisible) {
            infoContainer.visibility = View.INVISIBLE
//            infoContainer.trans
        } else
            infoContainer.visibility = View.VISIBLE
    }

    fun toggleTitleBar() {
        isTitleBarVisible = !isTitleBarVisible
        if (!isTitleBarVisible)
            supportActionBar?.hide()
        else
            supportActionBar?.show()

        //toggleInfoBar()
    }

    private fun isGPSEnabled(): Boolean {
        return LocationManagerService.isProviderEnabled(LocationManager.GPS_PROVIDER) || LocationManagerService.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    @SuppressLint("MissingPermission") //This method is called only after permissions check.
    private fun setup() {

        mapFrag = supportFragmentManager.findFragmentById(R.id.map_mini) as SupportMapFragment?
        mapFrag!!.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initRetrofitClient()
        val token = prefMan.getString(Constants.Token, "")

        if (token.equals("")) {
            mainVM.manageToken(prefMan)
        }/* else {
            Log.d("Tag", "start token: $token")
        }*/
        /*val isEnabled =
            isGPSEnabled()*/
        if (!isGPSEnabled()) {
            buildAlertMessageNoGps()
        }

        mLocationRequest = LocationRequest.create().apply {
            interval = MinTime
            fastestInterval = (MinTime / 2)
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            smallestDisplacement = 5f
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                updateLocation(locationResult.locations[0])
            }

        }


        fusedLocationClient.lastLocation.addOnCompleteListener {

            if (it.result != null) {

                updateLocation(it.result)
            }
        }

        Looper.myLooper()?.let {
            fusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                locationCallback,
                it
            )
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener {
            startPhotoProcessing()
            //showNotesDialogueWithPhotosTaken()
        }

        /*camera_capture_button.setOnLongClickListener {
            startPhotoProcessing()
            true
        }*/

        previewImage.setOnClickListener {
            showImagePreview()
        }
        outputDirectory = getOutputDirectory()
        //additionalDataDirectory = getOutputDirectoryAdditionalDataImage()
        cameraExecutor = Executors.newSingleThreadExecutor()

        /*viewFinder.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_UP) {
                return@setOnTouchListener false
            }

            val cameraControl = CameraX.getCameraControl(CameraX.LensFacing.BACK) // you can set it to front
            val factory = TextureViewMeteringPointFactory(textureView)
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder.from(point).build()
            cameraControl.startFocusAndMetering(action)
            return@setOnTouchListener true
        }
*/

        ////

        /*val characteristics: CameraCharacteristics = manager.getCameraCharacteristics(cameraId)
        val capabilities = characteristics
            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

        val isManualFocusSupported: Boolean = IntStream.of(capabilities)
            .anyMatch { x -> x === CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR }

        if (isManualFocusSupported) {
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            previewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
        }*/

        ////
        CameraType = prefMan.getString(getString(R.string.SelectCamera), "0")?.toInt() ?: 0
        startCamera(CameraType)
        shouldCheckForCamera = true
    }


    private fun initRetrofitClient() {
        val client = OkHttpClient.Builder().build()
        apiService =
            Retrofit.Builder().baseUrl(Constants.BaseURL).client(client).build().create(
                ApiService::class.java
            )
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton(
                "Yes"
            ) { dialog, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton(
                "Exit"
            ) { dialog, id ->
                dialog.cancel()
                finish()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun startPhotoProcessing() {
        if (!isGPSEnabled()) {
            buildAlertMessageNoGps()
            return
        }

        if (progressBar.visibility == View.VISIBLE) {
            return
        }

        if (bigJsonObject.isNull("latitude")) {
            Toast.makeText(
                this,
                "Location not retrieved yet. Wait for location.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val instant: Instant = Instant.now()
            timeText =
                instant.toString()//getDateTimeInUTC() //df.format(System.currentTimeMillis())

            val uKey =
                prefMan.getString(
                    getString(R.string.key_title_edittext_preference),
                    "000000"
                )

            try {
                bigJsonObject.put("Key", uKey)
                bigJsonObject.put("clicked_time", timeText)
                bigJsonObject.put("CampaignID", CampaignID)
                bigJsonObject.put("notes", notesSaved)
                bigJsonObject.put("phone", phone)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }


            takePhoto()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            setup()
        } else {
            Toast.makeText(
                this,
                "Permissions not granted by the user.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun updateLocation(location: Location) {
        val geocoder = Geocoder(applicationContext, Locale.getDefault())
        try {
            val listAddresses =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            var address: String? = ""
            if (listAddresses != null && listAddresses.size > 0) {


                val addr = listAddresses[0].getAddressLine(0)
                if (addr.isNotBlank()) {
                    address = "$addr "
                }
                /*if (listAddresses[0].thoroughfare != null) {
                    address = listAddresses[0].thoroughfare + " "
                }
                if (listAddresses[0].locality != null) {
                    address += listAddresses[0].locality + " "
                }
                if (listAddresses[0].postalCode != null) {
                    address += listAddresses[0].postalCode + " "
                }
                if (listAddresses[0].adminArea != null) {
                    address += listAddresses[0].adminArea
                }*/
            }
            //Log.i("getAddressLine", listAddresses[0].getAddressLine(0))
            //Log.i("Address", address!!)

            lastLocation = location

            val date: String = df.format(location.time)
            val instant: Instant = Instant.now()
            val dateServer: String = instant.toString()
            //val currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())

            val lat = "%.4f".format(location.latitude)
            val lon = "%.4f".format(location.longitude)
            locationTextPrevious = "Location: ${lat},$lon \n$address"
            locationText =
                "$locationTextPrevious\nTime: $date"

            try {
                bigJsonObject.put("latitude", lat)
                bigJsonObject.put("longitude", lon)
                bigJsonObject.put("address", address)
                bigJsonObject.put("locationTimeStamp", dateServer)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            reRenderText()

            //Remove markers from past location
            mCurrLocationMarker?.remove()

            //Place current location marker
            val latLng = LatLng(location.latitude, location.longitude)
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            markerOptions.title(address)
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
            mCurrLocationMarker = mGoogleMap?.addMarker(markerOptions)

            //move map camera
            mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //cam

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        /*val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/" + resources.getString(R.string.app_name)
                )
            }
        }


        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()*/


        // Set up image capture listener, which is triggered after photo has
        // been taken

//        GlobalScope.launch {
        /*   imageCapture.takePicture(
               outputOptions,
               ContextCompat.getMainExecutor(baseContext),
               object : ImageCapture.OnImageSavedCallback {
                   override fun onError(exc: ImageCaptureException) {
                       Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                       Toast.makeText(
                           baseContext,
                           "Photo capture failed: ${exc.message}",
                           Toast.LENGTH_SHORT
                       ).show()
                   }

                   override fun
                           onImageSaved(output: ImageCapture.OutputFileResults) {
                       val msg = "Original Photo save succeeded: ${output.savedUri}"
                       Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                       Log.d(TAG, msg)
                       ///

                          //val intent = Intent(this@MainActivity, DetailsActivity::class.java)
                          //intent.putExtra("ImagePath", output.savedUri.toString())
                          //startActivity(intent)
                       ///

                       var bitmap: Bitmap? = null
                       try {
                           val f: File = File(output.savedUri.toString())
                           val options = BitmapFactory.Options()
                           options.inPreferredConfig = Bitmap.Config.ARGB_8888
                           bitmap = BitmapFactory.decodeStream(FileInputStream(f), null, options)
                           previewImage.setImageBitmap(bitmap)
                       } catch (e: java.lang.Exception) {
                           e.printStackTrace()
                       }

                       if (previewImage.visibility != View.VISIBLE)
                           previewImage.visibility = View.VISIBLE
                       progressBar.visibility = View.INVISIBLE

                   }
               }
           )*/
//        }

        ///

        playSound()

        ///
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    previewImage.visibility = View.INVISIBLE

                    CoroutineScope(Dispatchers.Default).launch {
//                        val date: String = instant.toString()

                        //get bitmap from image
                        val bitmap = imageProxyToBitmap(image)

                        image.close()


                        //here was the map.snapshot + save info image method.

                        CoroutineScope(Dispatchers.IO).launch {

                            scaledOriginalBitmap = scaleToMaxAllowedDimension(bitmap)

                            /* val uri = mainVM.savePhotoToExternalStorage(
                                 UUID.randomUUID().toString(),
                                 imageBitmap,
                                 contentResolver
                             )*/

                            val lat = bigJsonObject.getString("latitude")
                            val lon = bigJsonObject.getString("longitude")
                            val fileName =
                                "ImgOOHT_ORG_" + CampaignID + "_" + phone + "_" + System.currentTimeMillis()
                                    .toString() + ".jpg"

                            mapImageFileName =
                                "ImgOOHT_Map_" + CampaignID + "_" + phone + "_" + System.currentTimeMillis()
                                    .toString() + ".jpg"

                            bigJsonObject.put("mapImageName", mapImageFileName)

                            val path =
                                mainVM.saveImage(
                                    scaledOriginalBitmap,
                                    baseContext,
                                    fileName,
                                    lat,
                                    lon,
                                    false
                                )


                            if (path.isNotBlank()) {


                                fileDataViewModel?.insert(
                                    Word(
                                        path,
                                        bigJsonObject.toString()
                                    )
                                )

                                CoroutineScope(Dispatchers.Main).launch {

                                    showNotesDialogueWithDataImageTaken()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        applicationContext,
                                        "Error while saving.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        }

                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    if (progressBar.visibility != View.VISIBLE) {

                        progressBar.visibility = View.INVISIBLE

                        Toast.makeText(applicationContext, "Error", Toast.LENGTH_LONG).show()
                    }
                }
            })

        ///
        /*
        val mImageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "YOUR_DIRECTORY"
        )
        val isDirectoryCreated = mImageDir.exists() || mImageDir.mkdirs()
        if (isDirectoryCreated) {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .toString() + "/YOUR_DIRECTORY", "YOUR_IMAGE.jpg"
            )
            val outputFileOptionsBuilder = ImageCapture.OutputFileOptions.Builder(file)
            imageCapture.takePicture(outputFileOptionsBuilder.build(),
                { obj: Runnable -> obj.run() }, object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val options = BitmapFactory.Options()
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                                val bitmap =
                                    BitmapFactory.decodeStream(FileInputStream(file), null, options)
                                previewImage.setImageBitmap(bitmap)
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }

                            if (previewImage.visibility != View.VISIBLE)
                                previewImage.visibility = View.VISIBLE
                            progressBar.visibility = View.INVISIBLE
                        }

                    }

                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                    }
                })
        }*/

        /////////

    }

    private fun saveInfoImage() {

        mGoogleMap?.snapshot {
            //imageBitmap.recycle()

            //imageBitmap = writeInfo(bitmap, it)
            //                            val scaledBitmap = scaleToMaxAllowedDimensionNew(bitmap)


            val lat = bigJsonObject.getString("latitude")
            val lon = bigJsonObject.getString("longitude")

            var str = locationText
            if (notesSaved.isNotBlank()) {
                str = "$locationText | Note: $notesSaved"
            }

            textViewProxy.text = str

            //save image original
            //saveBitmap(scaledOriginalBitmap, true)
//                            saveImage(scaledOriginalBitmap,  UUID.randomUUID().toString(), "desc")

            CoroutineScope(Dispatchers.IO).launch {
                /*mainVM.savePhotoToExternalStorage(
                    UUID.randomUUID().toString(),
                    scaledOriginalBitmap,
                    contentResolver
                )*/

//                imageBitmap = scaledOriginalBitmap
                imageBitmap = writeInfo(scaledOriginalBitmap, it)

                val fileName =
                    "ImgOOHT_Data_" + CampaignID + "_" + phone + "_" + System.currentTimeMillis()
                        .toString() + ".jpg"


                val path = mainVM.saveImage(
                    imageBitmap, baseContext, fileName, lat, lon,
                    false
                )
                val paths = path.split("-")
                lastSavedImageDirectory = paths[0]

                /*it?.let {
                    val fileNameMap =
                        "ImgOOHT_Map_" + CampaignID + "_" + phone + "_" + System.currentTimeMillis()
                            .toString() + ".jpg"
                    val tempIt = it
                    val path = mainVM.saveImage(tempIt, baseContext, fileNameMap, lat, lon)


                //val paths = path.split("-")
                //lastSavedImageDirectory = paths[0]
                //val newImageDataAddr = paths[0]+"-D_"+paths[1]

                //val previousPath = """/external/images/media/356"""
                //val file  = FileUtils().getFile(applicationContext, Uri.parse(previousPath))
                Log.d("Tag", "path --- $path")
//                                val myBitmap = BitmapFactory.decodeFile()
//                                previewImage.setImageBitmap()

                //scaledOriginalBitmap.recycle()

                fileDataViewModel?.insert(
                    Word(
                        path,
                        bigJsonObject.toString()
                    )
                )
            }*/

                ///
                CoroutineScope(Dispatchers.Main).launch {
                    progressBar.visibility = View.INVISIBLE
                    previewImage.setImageBitmap(imageBitmap)

                    if (previewImage.visibility != View.VISIBLE) {

                        previewImage.visibility = View.VISIBLE
                    }
                }

                if (uploaderBehavior == 1) {
                    delay(100)
                    scheduleWorker()
                }
                //startUploaderService()


            }


            it?.let {
                CoroutineScope(Dispatchers.IO).launch {

                    //val tempIt = it
                    val path1 = mainVM.saveImage(it, baseContext, mapImageFileName, lat, lon, true)


                    //val paths = path1.split("-")

                    //Log.d("Tag", "path --- $paths[0]")


                    fileDataViewModel?.insert(
                        Word(
                            path1,
                            bigJsonObject.toString()
                        )
                    )
                }
            }

//                            val MAX_ALLOWED_RES = textView.width * 3

            /*val scaledBitmap =
                scaleToMaxAllowedDimensionNew(
                    scaledOriginalBitmap,
                    MAX_ALLOWED_RESOLUTION
                )*/

            //scaledOriginalBitmap.recycle()
            //scaledBitmap.recycle()

            //img.setImageBitmap(infoImage)
            // previewImage.setImageBitmap(imageBitmap)
            //onBtnSavePng(infoImage)

            //lastSavedImageDirectory = saveImage(scaledOriginalBitmap, UUID.randomUUID().toString(), "desc").toString()

            //save imageData
            //saveBitmap(imageBitmap, false)
        }

    }

    private fun writeInfo(bitmap: Bitmap, map_bitmap: Bitmap?): Bitmap {
        val mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        //scaledBitmap.recycle()
        val canvas = Canvas(mutableImage)
        //val paint = Paint()

        //paint.color = Color.WHITE
        //paint.textSize = 10F
        //val rect = Rect()
        //val rect1 = Rect()

        /*val w = textView.width
        val h = textView.height
        textView.width = bitmap.width / 2
        textView.height = ((bitmap.height * 20) / 100)*/

        //infoContainer.isDrawingCacheEnabled();
        //textViewProxy.getDrawingRect(rect)
        //textViewProxy.isDrawingCacheEnabled = true

        val textImage: Bitmap = getBitmapFromView(textViewProxyContainer)
//        val textImage: Bitmap = Bitmap.createBitmap(textViewProxy.drawingCache)
        //img.setImageBitmap(bitmap)
        //textViewProxy.isDrawingCacheEnabled = false
        /*textView.setBackgroundColor(
            ContextCompat.getColor(
                this,
                R.color.text_background_transparent
            )
        )*/

        /*textView.width = w
        textView.height = h*/
        if (map_bitmap != null) {
            //val percentage20 = (((bitmap.width * 20) / 100))
//           val  map_bitmap_resized = resizeAnythingToAnySize(map_bitmap, (((bitmap.width * 20) / 100) - 20), ((bitmap.height * 20) / 100))
            val map_bitmap_resized = resizeAnythingToAnySize(
                map_bitmap,
                (bitmap.width - textViewProxyContainer.width),
                textViewProxyContainer.height
            )


            //textImage = resizeAnythingToAnySize(textImage, (((bitmap.width * 50) / 100) - 20), ((bitmap.height * 20) / 100))

            /* canvas.drawBitmap(
                 textImage,
                 bitmap.width.toFloat() - textImage.width,
                 bitmap.height.toFloat() - textImage.height,
                 null
             )*/
            canvas.drawBitmap(
                textImage,
                bitmap.width.toFloat() - textViewProxyContainer.width,
                bitmap.height.toFloat() - textViewProxyContainer.height,
                null
            )
            canvas.drawBitmap(
                map_bitmap_resized,
                0f,
                bitmap.height.toFloat() - map_bitmap_resized.height,
                null
            )
            map_bitmap.recycle()
            map_bitmap_resized.recycle()
            textImage.recycle()
            // bitmap.recycle()

        }
        //
        /*infoContainer.drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        infoContainer.setDrawingCacheEnabled(true)
        val infoImage: Bitmap = Bitmap.createBitmap(infoContainer.getDrawingCache())
        infoContainer.setDrawingCacheEnabled(false)*/


        /*
        if (map_bitmap != null) {

            /* paint.color = Color.DKGRAY
             paint.style = Paint.Style.FILL
             canvas.drawRect(
                 10f,
                 bitmap1.height.toFloat(),
                 scaledBitmap.height.toFloat(),
                 scaledBitmap.width.toFloat(),
                 paint
             )*/
        }*/

//        var testBitmap = Bitmap.createBitmap(bitmap);
/*
        mGoogleMap?.snapshot {

//            testBitmap = it?.let { it1 -> Bitmap.createBitmap(it1) }

            it?.let { it1 -> canvas.drawBitmap(it1, bitmap.width.toFloat() - (it.width + textImage.width),bitmap.height.toFloat() - it.height , null) }

        }*/
        /*
        val x = bitmap.width
        val y = bitmap.height

        Log.d("Tag",
            "bitmap, width: $x height: $y, rect: " + rect.centerX() + " " + rect.centerY()
                    + " " + rect.left +" " +  rect.right +" " +  rect.top +" " +  rect.bottom
        )*/

//        val mutableImage1 = mutableImage.copy(Bitmap.Config.ARGB_8888, false)
//        mutableImage.recycle()

        return mutableImage
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }


    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix()
        currentImageOrientation = image.imageInfo.rotationDegrees

        matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())

        return Bitmap.createBitmap(
            imageBitmap,
            0,
            0,
            imageBitmap.width,
            imageBitmap.height,
            matrix,
            true
        )
    }

    private fun startCamera(cameraSelect: Int) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }


            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = if (cameraSelect == 0) CameraSelector.DEFAULT_BACK_CAMERA
            else CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                cameraControl = camera.cameraControl
                //cameraControl.enableTorch(flashFlag)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            ///
            ///
        }, ContextCompat.getMainExecutor(this))

    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else filesDir
    }

    private fun getInternalDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(
                it,
                resources.getString(R.string.app_name) + "/DataImages/" + CampaignID
            ).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else filesDir
    }

    private fun getInternalDirectoryOriginalFiles(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(
                it,
                resources.getString(R.string.app_name) + "/OriginalImages/" + CampaignID
            ).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else filesDir
    }

    private fun getOutputDirectoryAdditionalDataImage(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name) + "/Locations").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        prefMan.edit().putString(getString(R.string.notesSaved), "")
            .apply()
        cameraExecutor.shutdown()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        imageBitmap.recycle()
        scaledOriginalBitmap.recycle()

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    companion object {
        private const val TAG = "CameraXMap_Freelance"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }


    /////////////////Map
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        mGoogleMap = googleMap
        mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL


        //mapHasBeenReady = true
        ////
        mGoogleMap?.uiSettings?.isMapToolbarEnabled = false
        mGoogleMap?.uiSettings?.setAllGesturesEnabled(gesturesEnabled)

        //Initialize Google Play Services
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            //Location Permission already granted
            //buildGoogleApiClient();
            mGoogleMap?.isMyLocationEnabled = true
        }

        mGoogleMap?.setOnCameraIdleListener {
            Log.d("Tag", "Camera is idle now.")
            updateUIForNFromMap()
        }
        mGoogleMap?.setOnCameraMoveCanceledListener {
            Log.d("Tag", "CameraMove Canceled now.")
            //updateUIForNFromMap()

        }

    }

    private fun updateUIForNFromMap() {
        timer.cancel()
        timer.start()
    }

    private fun toggleGestures() {
        gesturesEnabled = !gesturesEnabled
        mGoogleMap?.uiSettings?.setAllGesturesEnabled(gesturesEnabled)
    }

    private fun setMapType(i: Int) {
        if (i == 0) {
            mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
        } else {
            mGoogleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
        }
        timer.cancel()
        timer.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.maps_menue, menu)

        return super.onCreateOptionsMenu(menu)
    }

    //not used in favour of preference screen options.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.Normal -> {
                setMapType(0)
                true
            }
            R.id.hybrid -> {
                setMapType(1)
                true
            }
            R.id.About -> {
                showHelp()
                true
            }
            /*R.id.settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                true
            }
            R.id.Note -> {
                showNotesDialogue()
                true
            }*/
            R.id.LogOut -> {
                showLogOutDialogue()
                true
            }
            R.id.ToggleUploader -> {
                toggleUploaderService()
                true
            }
            R.id.viewEnqueuedFiles -> {
                val i = Intent(this, DetailsActivity::class.java)
                startActivity(i)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleUploaderService() {
        Log.d("Tag", "toggleUploaderService")

        if (connectivityManager == null) {
            Log.d("Tag", "connectivityManager == null")
            return
        }
        if (!mainVM.isInternetConnected(connectivityManager)) {
            Log.d("Tag", "no internet")
            Toast.makeText(this, "no internet", Toast.LENGTH_LONG).show()
            return
        }


        serviceRunning = prefMan.getBoolean(Constants.ServiceRunning, false)
        //serviceRunning = !serviceRunning

        Log.d("Tag", "toggleUploaderService . $serviceRunning")

        if (serviceRunning) {
            Log.d("Tag", "toggleUploaderService . $serviceRunning ... internal")

            startUploaderService()

            prefMan.edit().putBoolean(Constants.ServiceRunning, false).apply()

        } else {
            stopService(
                Intent(
                    this,
                    UploaderService::class.java
                )
            )
            prefMan.edit().putBoolean(Constants.ServiceRunning, true).apply()

        }
    }

    private fun startUploaderService() {
        if (connectivityManager == null) {
            Log.d("Tag", "connectivityManager == null")
            return
        }
        if (!mainVM.isInternetConnected(connectivityManager)) {
            Log.d("Tag", "Not connected to internet")
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(baseContext, "Not connected to internet", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(
                Intent(
                    this,
                    UploaderService::class.java
                )
            )
        } else {
            startService(
                Intent(
                    this,
                    UploaderService::class.java
                )
            )
        }
    }

    private fun showNotesDialogue() {
        notesSaved = prefMan.getString(getString(R.string.notesSaved), "").toString()
        val dialogBuilder = AlertDialog.Builder(this)
        // ...Irrelevant code for customizing the buttons and title
        val dialogView = layoutInflater.inflate(R.layout.notes_dialog, null)
        dialogBuilder.setView(dialogView)

        val editText: EditText = dialogView.findViewById(R.id.editTextTextNotes)
        editText.setText(notesSaved)

        val NotesSave: Button = dialogView.findViewById(R.id.buttonNotesSave)
        val NotesCancel: Button = dialogView.findViewById(R.id.buttonNotesCancel)
        val alertDialog = dialogBuilder.create()
        NotesSave.setOnClickListener {
            prefMan.edit().putString(getString(R.string.notesSaved), editText.text.toString())
                .apply()
            alertDialog.dismiss()
            reRenderText()
        }

        NotesCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()

    }


    private fun showLogOutDialogue() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Do you want to log Out?")
            .setMessage("Are you sure you want to Log Out? \nYou will have to verify again to log in.")

            // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton("Just Log Out. ⚠️") { dialog, _ ->
                prefMan.edit().putBoolean(Constants.rfc0rw2e1ra78fpwe, false).apply()
                prefMan.edit().putString(getString(R.string.notesSaved), "").apply()
                dialog.dismiss()
                finish()
                Toast.makeText(this, "Logged out.", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Remove enqueued files and log out ✔️") { dialog, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    fileDataViewModel?.deleteAll()
                }

                prefMan.edit().putBoolean(Constants.rfc0rw2e1ra78fpwe, false).apply()
                prefMan.edit().putString(getString(R.string.notesSaved), "").apply()
                dialog.dismiss()
                finish()
                Toast.makeText(this, "Logged out.", Toast.LENGTH_LONG).show()
            }
            // A null listener allows the button to dismiss the dialog and take no further action.
            .setNegativeButton("Cancel. ✔️") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)

        dialog.show()

    }


    private fun showNotesDialogueWithDataImageTaken() {
        notesSaved = prefMan.getString(getString(R.string.notesSaved), "").toString()
        val dialogBuilder = AlertDialog.Builder(this)
        // ...Irrelevant code for customizing the buttons and title
        val dialogView = layoutInflater.inflate(R.layout.notes_dialog, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setCancelable(false)

        val editText: EditText = dialogView.findViewById(R.id.editTextTextNotes)


        val NotesSave: Button = dialogView.findViewById(R.id.buttonNotesSave)
        val NotesCancel: Button = dialogView.findViewById(R.id.buttonNotesCancel)
        val NotesDel: Button = dialogView.findViewById(R.id.buttonNotesDel)
        val alertDialog = dialogBuilder.create()
        editText.setText(notesSaved)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                NotesDel.isEnabled = s?.toString()?.length!! > 0
            }
        })

        ///
        //startPhotoProcessing()
        ///

        NotesSave.setOnClickListener {
            prefMan.edit().putString(getString(R.string.notesSaved), editText.text.toString())
                .apply()
            alertDialog.dismiss()
            reRenderText()
//            startPhotoProcessing()
            saveInfoImage()
        }

        NotesDel.setOnClickListener {
            editText.setText("")
        }

        NotesCancel.setOnClickListener {
            alertDialog.dismiss()
//            startPhotoProcessing()
            saveInfoImage()
        }

        alertDialog.show()

    }


    private fun showImagePreview() {
        val dialogBuilder = AlertDialog.Builder(this)
        // ...Irrelevant code for customizing the buttons and title
        val dialogView = layoutInflater.inflate(R.layout.preview_image, null)
        dialogBuilder.setView(dialogView)

        val imageView: ImageView = dialogView.findViewById(R.id.imageViewPreviewDialog)
        val buttonOpenGallery: MaterialButton = dialogView.findViewById(R.id.buttonOpenGallery)
        val buttonCancelDialog: MaterialButton = dialogView.findViewById(R.id.buttonCancelDialog)
        imageView.setImageBitmap(imageBitmap)

        dialogBuilder.setCancelable(true)

        /*dialogBuilder.setPositiveButton(
            "Start Uploader"
        ) { _, _ ->
            //multipartImageUpload()
            startUploaderService()
        }*/

        buttonOpenGallery.setOnClickListener { _ ->
            openPath(lastSavedImageDirectory)
        }
        val alertDialog = dialogBuilder.create()

        buttonCancelDialog.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun openPath(path: String) {
        val uri: Uri =
            Uri.parse(path)

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(uri, "image/*")
        try {

            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        /*val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
            startActivity(intent)*/
        /*
        if (intent.resolveActivityInfo(packageManager, 0) != null) {
                   startActivity(intent)
                   Log.d("My", path+ " , opening path.")
               }
               else
                   Log.d("My", path+ " ,nothing can open it.")*/

        /*val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
        startActivity(intent)*/

        /*if (intent.resolveActivityInfo(packageManager, 0) != null) {
            startActivity(intent)
            Log.d("My", path+ " , opening path.")
        }
        else
            Log.d("My", path+ " ,nothing can open it.")*/
    }


    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("App Version: 0.1\nDeveloper: Rifat\nOwner: Mr. Sanjeev") // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                "Ok"
            ) { dialog, which ->
                // Continue with delete operation
                dialog.dismiss()
            } // A null listener allows the button to dismiss the dialog and take no further action.
            .show()
    }

    private fun georeferenceImage(image_file: File, location: Location): Boolean {
        try {
            val exif = ExifInterface(image_file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, getLat(location))
            exif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                if (location.latitude < 0) "S" else "N"
            )
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, getLon(location))
            exif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                if (location.longitude < 0) "W" else "E"
            )
            //exif.setLatLong(location.getLatitude(), location.getLongitude());
            //exif.setAltitude(location.getAltitude());
            exif.saveAttributes()
        } catch (e: IOException) {
            return false
        }
        return true
    }

    private fun getLon(location: Location): String? {
        val degMinSec =
            Location.convert(location.longitude, Location.FORMAT_SECONDS).split(":".toRegex())
                .toTypedArray()
        return degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000"
    }

    private fun getLat(location: Location): String? {
        val degMinSec =
            Location.convert(location.latitude, Location.FORMAT_SECONDS).split(":".toRegex())
                .toTypedArray()
        return degMinSec[0] + "/1," + degMinSec[1] + "/1," + degMinSec[2] + "/1000"
    }

    /*

        fun onBtnSavePng(bm: Bitmap) {
            try {
                val fileName: String = "HelloWorld" + ".jpg"
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
                    values.put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    val directory =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    val file = File(outputDirectory, fileName)
                    values.put(MediaStore.MediaColumns.DATA, file.absolutePath)
                }
                val uri: Uri? =
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri).use { output ->

                        bm.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.d("onBtnSavePng", e.toString()) // java.io.IOException: Operation not permitted
            }
        }

        fun testWithFiles() {
            val filename = "filename.jpg"
    //        val dir: File = this.filesDir
            val dir: File = getOutputDirectoryAdditionalDataImage()
            val file = File(dir, filename)

            try {
                Log.d(TAG, "The file path = " + file.absolutePath)
                file.createNewFile()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        fun screenshot(view: View, filename: String): File? {
            val date = Date()

            // Here we are initialising the format of our image name
            val format = android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", date)
            try {
                // Initialising the directory of storage
                val dirpath = Environment.getExternalStorageDirectory().toString() + ""
                val file = File(dirpath)
                if (!file.exists()) {
                    val mkdir = file.mkdir()
                }

                // File name
                val path = "$dirpath/$filename-$format.jpeg"
                view.setDrawingCacheEnabled(true)
                val bitmap: Bitmap = Bitmap.createBitmap(view.getDrawingCache())
                //img.setImageBitmap(bitmap)
                view.setDrawingCacheEnabled(false)
                val imageurl = File(path)
                val outputStream = FileOutputStream(imageurl)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                outputStream.flush()
                outputStream.close()
                return imageurl
            } catch (io: FileNotFoundException) {
                io.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }
    */
// Update the texture view based on rotation
    private fun updateTransform(textureView: TextureView) {
        val matrix = Matrix()
        val cX = textureView.measuredWidth / 2f
        val cY = textureView.measuredHeight / 2f
        val rotationDgr: Float
        val rotation = textureView.rotation.toInt()
        rotationDgr = when (rotation) {
            Surface.ROTATION_0 -> 0f
            Surface.ROTATION_90 -> 90f
            Surface.ROTATION_180 -> 180f
            Surface.ROTATION_270 -> 270f
            else -> return
        }
        matrix.postRotate(rotationDgr, cX, cY)
        textureView.setTransform(matrix)
    }

    private fun scaleToMaxAllowedDimension(bitmap: Bitmap): Bitmap {
        /*val matrix = Matrix()


        matrix.postRotate(90F)
    //        matrix.postRotate(viewFinder.rotation)

        *//*  val cX = viewFinder.measuredWidth / 2f
          val cY = viewFinder.measuredHeight / 2f
          val rotationDgr: Float
          val rotation = viewFinder.rotation.toInt()
          rotationDgr = when (rotation) {
              Surface.ROTATION_0 -> 0f
              Surface.ROTATION_90 -> 90f
              Surface.ROTATION_180 -> 180f
              Surface.ROTATION_270 -> 270f
              else -> 0f
          }
          matrix.postRotate(rotationDgr)*//*

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
*/

        val outWidth: Int
        val outHeight: Int
        val inWidth = bitmap.width
        val inHeight = bitmap.height
        if (inWidth < inHeight) {
            outWidth = MAX_ALLOWED_RESOLUTION
            outHeight = inHeight * MAX_ALLOWED_RESOLUTION / inWidth
        } else {
            outHeight = MAX_ALLOWED_RESOLUTION
            outWidth = inWidth * MAX_ALLOWED_RESOLUTION / inHeight
        }
        return Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false)
    }

    /**
     * reduces the size of the image
     * @param image
     * @return
     */
    fun scaleToMaxAllowedDimensionNew(image: Bitmap, MAX_ALLOWED_RES: Int): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = MAX_ALLOWED_RES
            height = (width / bitmapRatio).toInt()
        } else {
            height = MAX_ALLOWED_RES
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, false)
    }

    fun resizeAnythingToAnySize(image: Bitmap, w: Int, h: Int): Bitmap {
        var width = image.width
        var height = image.height

        Log.v("Pictures", "Width and height are $width--$height")

        if (width > height) {
            // landscape
            val ratio = width.toFloat() / MAX_ALLOWED_RESOLUTION
            width = MAX_ALLOWED_RESOLUTION
            height = (height / ratio).toInt()
        } else if (height > width) {
            // portrait
            val ratio = height.toFloat() / MAX_ALLOWED_RESOLUTION
            height = MAX_ALLOWED_RESOLUTION
            width = (width / ratio).toInt()
        }

        Log.v("Pictures", "after scaling Width and height are $width--$height")
        return Bitmap.createScaledBitmap(image, w, h, false)

    }

    fun resizeBitmapImageForFitSquare(imagex: Bitmap): Bitmap {
        val maxResolution: Int = MAX_ALLOWED_RESOLUTION
        var image = imagex
        if (maxResolution <= 0) return image
        val width = image.width
        val height = image.height
        val ratio =
            if (width >= height) maxResolution.toFloat() / width else maxResolution.toFloat() / height
        val finalWidth = (width.toFloat() * ratio).toInt()
        val finalHeight = (height.toFloat() * ratio).toInt()
        image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
        return if (image.width == image.height) image else {
            //fit height and width
            var left = 0
            var top = 0
            if (image.width != maxResolution) left = (maxResolution - image.width) / 2
            if (image.height != maxResolution) top = (maxResolution - image.height) / 2
            val bitmap = Bitmap.createBitmap(maxResolution, maxResolution, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(image, left.toFloat(), top.toFloat(), null)
            canvas.save()
            canvas.restore()
            bitmap
        }
    }

    private fun reRenderText() {

        notesSaved = prefMan.getString(getString(R.string.notesSaved), "").toString()
        CampaignID = prefMan.getString(getString(R.string.GroupID), "0").toString()
//        MapZoomLevel = prefMan.getString(Constants.MapZoomLevel, "16").toString().toInt()
        MAX_ALLOWED_RESOLUTION =
            prefMan.getString(getString(R.string.SaveQuality), "1440")?.toInt() ?: 1440
        MapZoomLevel = prefMan.getInt(Constants.MapZoomLevel, 16)

        val MapTypePref = prefMan.getString(getString(R.string.MapTypePref), "0")?.toInt()
        if (MapTypePref != null && !fromLaunch) {
            if (MapTypePref != PreviousMapType)
                setMapType(MapTypePref)

            PreviousMapType = MapTypePref
        }

        uploaderBehavior =
            prefMan.getString(getString(R.string.autoUploader), "0").toString().toInt()


        if (uploaderBehavior == 1) {
            val size = fileDataViewModel?.allWords?.value?.size
            if (size != null) {
                if (size > 0) {
                    scheduleWorker()
                }
            }
        } else {
            WorkManager.getInstance(this).cancelUniqueWork("periodicUploadWorker")
        }


        if (PreviousMapZoomLevel != MapZoomLevel) {
            mGoogleMap?.animateCamera(CameraUpdateFactory.zoomTo(MapZoomLevel.toFloat()))
            PreviousMapZoomLevel = MapZoomLevel
        }
        Log.d(
            "Tag",
            "MapZoomLevel $MapZoomLevel , uploaderBehavior: $uploaderBehavior , timeText: $timeText"
        )
        if (shouldCheckForCamera) {
            CameraType = prefMan.getString(getString(R.string.SelectCamera), "0")?.toInt() ?: 0
            if (PreviousCameraType != CameraType) {
                startCamera(CameraType)
            }
            PreviousCameraType = CameraType
        }

//        val mainScreenSTR = "$locationText \nCampaign ID:$CampaignID"
        val mainScreenSTR = locationText

        var str = mainScreenSTR
        if (notesSaved.isNotBlank()) {
//            str = "$locationText \nCampaign ID: $CampaignID \nNote: $notesSaved"
            str = "$locationText \nNote: $notesSaved"
        }

        textViewProxy.text = str
        textView.text = mainScreenSTR

        /*if (MAX_ALLOWED_RESOLUTION < 1920)
            heightPercentage = 30
        else
            heightPercentage = 20*/
        /*if (MAX_ALLOWED_RESOLUTION < 1920) heightPercentage = 26
        else
            heightPercentage = 20*/
        if (MAX_ALLOWED_RESOLUTION != previous_MAX_ALLOWED_RESOLUTION) {
            previous_MAX_ALLOWED_RESOLUTION = MAX_ALLOWED_RESOLUTION
            val h = ((MAX_ALLOWED_RESOLUTION * heightPercentage) / 100)
            val w = ((MAX_ALLOWED_RESOLUTION * 70) / 100)
//            val params = mapFrag?.view?.layoutParams
//            params?.height = h
//            params?.width = h
            Log.d(
                "Tag",
                "textViewProxy size: h ${textViewProxy.height} , w  ${textViewProxy.width}"
            )
//            if (textViewProxy.height < h) {
            param.height = h
            param.width = w
            //param.gravity = Gravity.CENTER

//            }

            /*
            textViewProxy.height =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, h.toFloat(), resources.displayMetrics)
                    .toInt()
            textViewProxy.width =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, w.toFloat(), resources.displayMetrics)
                    .toInt()*/
            textViewProxyContainer.layoutParams = param
            textViewProxyContainer.requestLayout()

            param.width = w + 140
            textViewProxy.layoutParams = param
            textViewProxy.requestLayout()
            Log.d(
                "Tag",
                "textView size changed: h $h , w $w"
            )
        }
        /*try {
            mGoogleMap?.snapshot {
                imageView.setImageBitmap(it)
            }

            Log.d("Tag", "timer running.")

        } catch (e: Exception) {
            e.printStackTrace()
        }*/
        /*if (bigJsonObject.has("latitude"))
            updateUIForNFromMap()*/
    }

    override fun onResume() {
        super.onResume()
        reRenderText()
        mainHandler.post(updateTextTask)
    }

    private fun playSound() {
        when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                val sound = MediaActionSound()
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }
            AudioManager.RINGER_MODE_SILENT -> {}
            AudioManager.RINGER_MODE_VIBRATE -> {}
        }
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
        flashButton.setBackgroundResource(R.drawable.ic_baseline_flash_off_24)
        flashFlag = 0
        mapHasBeenReady = false
        fromLaunch = false

        mainHandler.removeCallbacks(updateTextTask)

    }

    private fun showPopup(v: View) {
        v.setBackgroundResource(R.drawable.ic_baseline_menu_open_24)

        PopupMenu(this, v).apply {
            setOnMenuItemClickListener { item ->
                when (item?.itemId) {
                    R.id.Normal -> {
                        setMapType(0)
                        true
                    }
                    R.id.hybrid -> {
                        setMapType(1)
                        true
                    }
                    R.id.About -> {
                        showHelp()
                        true
                    }
                    R.id.LogOut -> {
                        showLogOutDialogue()
                        true
                    }
                    R.id.ToggleUploader -> {
                        toggleUploaderService()
                        true
                    }
                    R.id.viewEnqueuedFiles -> {
                        val i = Intent(v.context, DetailsActivity::class.java)
                        startActivity(i)
                        true
                    }
                    else -> false
                }
            }
            setOnDismissListener {
                v.setBackgroundResource(R.drawable.ic_baseline_menu_24)
            }
            inflate(R.menu.maps_menue)
            show()
        }
    }

//////////////////////////////////

    private fun timerTextUpdate() {
        mainHandler.post(object : Runnable {
            override fun run() {

                mainHandler.postDelayed(this, 1000)
            }
        })

    }

//////////////////////////////////

    //schedule worker
    private fun scheduleWorker() {
        val periodicUploadWork =
            PeriodicWorkRequest.Builder(UploadWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(uploaderConstraints)
                .build()
        //WorkManager.getInstance(this).enqueue(periodicWork)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodicUploadWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicUploadWork
        )

    }

//////////////////////////////////


}


/*

    private fun multipartImageUpload() {
        try {
//            val filesDir: File = applicationContext.filesDir
//            val file = File(filesDir, "image" + ".jpeg")
            val file = File(lastSavedImageDirectory)
            /*val bos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            val bitmapdata: ByteArray = bos.toByteArray()
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()*/
            val reqFile: RequestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("upload", file.name, reqFile)
            val name = "upload".toRequestBody("text/plain".toMediaTypeOrNull())
            val token = prefMan.getString(Constants.Token, "")


            val message = bigJsonObject.toString()
                .toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

            val req: Call<ResponseBody?>? =
                apiService!!.postImage("Bearer $token", name, message, body)

            Log.d("Tag", "uploading file...")

            req?.enqueue(object : retrofit2.Callback<ResponseBody?> {
                override fun
                        onResponse(
                    call: Call<ResponseBody?>,
                    response: Response<ResponseBody?>
                ) {
                    Log.d("Tag", "token: $token, resp: ${response.body()}")
                    when {
                        response.code() == 200 -> {
                            //textView.text = "Uploaded Successfully!"
                            Log.d("Tag", "Uploaded Successfully!")
                            Toast.makeText(
                                applicationContext, "Uploaded Successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        response.code() == 449 -> {
                            manageToken(true, prefMan)
                            Log.d("Tag", "resp: 449")

                        }
                        else -> {
                            Log.d("Tag", "issue:" + response.code())

                            Toast.makeText(
                                applicationContext,
                                response.code().toString() + " ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }

                override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                    //textView.text = "Upload Failed!"
                    //textView.setTextColor(Color.RED)
                    Toast.makeText(applicationContext, "Request failed", Toast.LENGTH_SHORT).show()
                    t.printStackTrace()
                }

            })
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }


    }
 */


    fun saveBitmap(bitmap: Bitmap, original: Boolean) {

        try {
            val file: File?

            CampaignID = prefMan.getString(getString(R.string.GroupID), "0").toString()
            if (original) {

                val fileName: String =
                    "image_PN_" + phone + "_CID_" + CampaignID + "_" + System.currentTimeMillis() + ".jpg"

                //

                //
                file = File(
                    getInternalDirectoryOriginalFiles(), fileName

                ) // the File to save , append increasing numeric counter to prevent files from getting overwritten.

                FileOutputStream(file).use {
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        it
                    ) // bmp is your Bitmap instance

                }

                lastSavedImageDirectory = file.absolutePath

                copyExif(file.absolutePath)

            } else {
                val fileName: String =
                    "imageData_" + CampaignID + "_" + System.currentTimeMillis() + ".jpg"

                file = File(
                    getInternalDirectory(), fileName

                ) // the File to save , append increasing numeric counter to prevent files from getting overwritten.

                FileOutputStream(file).use {
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        it
                    ) // bmp is your Bitmap instance

                }

            }

            //Toast.makeText(this, "Saved at: " + file.absolutePath, Toast.LENGTH_LONG).show()

            ///////////////////////////////////////////////////////////////////////////////////////
/*            val values = ContentValues()
            values.put(Images.Media.TITLE, file.name)
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            values.put(
                Images.ImageColumns.BUCKET_ID,
                file.toString().toLowerCase(Locale.US).hashCode()
            )
            values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, file.name.toLowerCase(Locale.US))
            values.put("_data", file.absolutePath)

            val cr = contentResolver
            cr.insert(Images.Media.EXTERNAL_CONTENT_URI, values)*/

            val values = ContentValues()
            values.put(Images.Media.TITLE, file.name)
            values.put(Images.Media.DISPLAY_NAME, file.name)

            if (notesSaved != null) {
                values.put(Images.Media.DESCRIPTION, notesSaved)
            }

            values.put(Images.Media.MIME_TYPE, "image/jpeg")
            values.put(Images.Media.DATE_ADDED, System.currentTimeMillis())

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(
                    Images.ImageColumns.BUCKET_ID,
                    file.toString().lowercase(Locale.US).hashCode()
                )
                values.put(
                    Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    file.name.lowercase(Locale.getDefault())
                )
            }

            values.put("_data", file.absolutePath)

             contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values)


            ///////////////////////////


            if (!original) {
                GlobalScope.launch(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                }
            } else {

                val uKey =
                    prefMan.getString(getString(R.string.key_title_edittext_preference), "000000")

                try {
                    bigJsonObject.put("Key", uKey)
                    bigJsonObject.put("clicked_time", timeText)
                    bigJsonObject.put("CampaignID", CampaignID)
                    bigJsonObject.put("notes", notesSaved)
                    bigJsonObject.put("phone", phone)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                fileDataViewModel?.insert(Word(file.absolutePath, bigJsonObject.toString()))

                if (uploaderBehavior == 1) {
                    toggleUploaderService()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            GlobalScope.launch(Dispatchers.Main) {
                progressBar.visibility = View.GONE
            }
        }
    }

    fun saveImage(source: Bitmap,
        title: String?, description: String?
    ): String? {
        val snapshot: File
        val url: Uri?
        try {
            val pictures =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val rpi = File(pictures, getString(R.string.app_name))
            if (!rpi.exists()) if (!rpi.mkdirs()) return null
            snapshot = File(rpi, title)
            val stream: OutputStream = FileOutputStream(snapshot)
            source.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            //lastLocation?.let { georeferenceImage(snapshot, it) }
            val values = ContentValues()
            values.put(Images.Media.TITLE, title)
            values.put(Images.Media.DISPLAY_NAME, title)
            if (description != null) {
                values.put(Images.Media.DESCRIPTION, description)
            }
            values.put(Images.Media.MIME_TYPE, "image/jpeg")
            values.put(Images.Media.DATE_ADDED, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(
                    Images.ImageColumns.BUCKET_ID,
                    snapshot.toString().toLowerCase(Locale.US).hashCode()
                )
                values.put(
                    Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    snapshot.name.toLowerCase(Locale.getDefault())
                )
            }
            values.put("_data", snapshot.absolutePath)
            url = contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (ex: java.lang.Exception) {
            return null
        }
        return url?.path.toString()
    }

 */


