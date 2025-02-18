package com.oohtracker.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.Spanned
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.google.android.material.button.MaterialButton
import com.oohtracker.Constants
import com.oohtracker.MainApplication
import com.oohtracker.R
import com.oohtracker.service.UploaderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsFragment : PreferenceFragmentCompat() {
    var prevNum = 16
    var preference: SharedPreferences? = null
    //var previousKey = ""
    //var previousCampaign = ""
    var connectivityManager: ConnectivityManager? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        preference = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val phone: EditTextPreference? = findPreference("Phone")
        phone?.summary = preference?.getString(Constants.PhoneLogIn, "Null")

        val UID: EditTextPreference? = findPreference("UID")
        UID?.summary = preference?.getString(Constants.UIDLogIn, "Null")

        val GroupID: EditTextPreference? = findPreference("GroupID")
        val Key: EditTextPreference? = findPreference("Key")
        //val MapZoom: SeekBarPreference? = findPreference("MapZoom")
        val About: Preference? = findPreference("AboutButtonPref")
        //val MapType: SeekBarPreference? = findPreference("MapTypePref")
        val viewEnqueuedFiles: Preference? = findPreference("viewEnqueuedFiles")
        val ToggleUploader: Preference? = findPreference("ToggleUploader")
//        val DataViewPercentage: SeekBarPreference? = findPreference("DataViewPercentage")

        /*previousKey =
            preference?.getString(getString(R.string.key_title_edittext_preference), "000").toString()
        previousCampaign = preference?.getString(getString(R.string.GroupID), "0").toString()*/

        connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        About?.setOnPreferenceClickListener {
            showAbout()
            true
        }
        viewEnqueuedFiles?.setOnPreferenceClickListener {
            val i = Intent(context, DetailsActivity::class.java)
            startActivity(i)
            true
        }
        ToggleUploader?.setOnPreferenceClickListener {
            toggleUploaderService()

            true
        }

        GroupID?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue.toString().isEmpty()) {
                //editCampaignValues()
                //preference.summary = previousCampaign
                Toast.makeText(context, "Can not be empty", Toast.LENGTH_LONG).show()
                return@setOnPreferenceChangeListener false
            }
            true
        }
        Key?.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue.toString().isEmpty()) {
               // editKeyValues()
                //preference.summary = previousCampaign
                Toast.makeText(context, "Can not be empty", Toast.LENGTH_LONG).show()
                return@setOnPreferenceChangeListener false
            }
            true
        }

        val filterArray = arrayOfNulls<InputFilter>(1)
        filterArray[0] = LengthFilter(25)
        val filterArray1 = arrayOfNulls<InputFilter>(1)
        filterArray1[0] = LengthFilter(12)

        GroupID?.setOnBindEditTextListener {
            it.filters = filterArray
        }

        /*MapZoom?.max = 21
        MapZoom?.min = 10*/

        Key?.setOnBindEditTextListener {
            it.filters = filterArray1
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }


    }

  /*  private fun editCampaignValues() {
        preference?.edit()
            ?.putString(getString(R.string.GroupID), previousCampaign)?.apply()
    }
    private fun editKeyValues() {
        preference?.edit()
            ?.putString(getString(R.string.key_title_edittext_preference), previousKey)?.apply()
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnSample:MaterialButton = view.findViewById(R.id.btnSample)
        val btnBack:MaterialButton = view.findViewById(R.id.btnBack)
        btnSample.setOnClickListener( View.OnClickListener() {
            showLogOutDialogue()
           // Toast.makeText(requireContext(), "Log out Button clicked!!", Toast.LENGTH_LONG).show()
        })
        btnBack.setOnClickListener {
            activity?.finish()
        }
    }

    private fun showLogOutDialogue() {
        val dialog = context?.let {
            AlertDialog.Builder(it)
                .setTitle("Do you want to log Out?")
                .setMessage("Are you sure you want to Log Out? \nYou will have to verify again to log in.")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton("Just Log Out. ⚠️") { dialog, _ ->
                    preference?.edit()?.putBoolean(Constants.rfc0rw2e1ra78fpwe, false)?.apply()
                    preference?.edit()?.putString(getString(R.string.notesSaved), "")?.apply()
                    dialog.dismiss()
                    activity?.finishAffinity()
                    Toast.makeText(context, "Logged out.", Toast.LENGTH_LONG).show()
                }
                .setNeutralButton("Clear files and log out ✔️") { dialog, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        MainApplication.getFileDataViewModel()?.deleteAll()
                    }

                    preference?.edit()?.putBoolean(Constants.rfc0rw2e1ra78fpwe, false)?.apply()
                    preference?.edit()?.putString(getString(R.string.notesSaved), "")?.apply()
                    dialog.dismiss()
                    activity?.finishAffinity()
                    Toast.makeText(context, "Logged out.", Toast.LENGTH_LONG).show()
                }
                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton("Cancel. ✔️") { dialog, _ ->
                    dialog.dismiss()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
        }

        dialog?.show()

    }


    private fun showAbout() {
        context?.let {
            /*AlertDialog.Builder(it)
                .setTitle("About")
                .setMessage("App Version: 0.1\nDeveloper: Rifat\nOwner: Mr. Sanjeev") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    "Ok"
                ) { dialog, which ->
                    // Continue with delete operation
                    dialog.dismiss()
                } // A null listener allows the button to dismiss the dialog and take no further action.
                .show()*/
                val dialog = Dialog(it)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(true)
                dialog.setContentView(R.layout.about_dialog)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                val dialogBtn_remove = dialog.findViewById<TextView>(R.id.txtClose)
                dialogBtn_remove.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()


        }
    }

    private fun toggleUploaderService() {
        Log.d("Tag", "toggleUploaderService")

        if (connectivityManager == null) {
            Log.d("Tag", "connectivityManager == null")
            return
        }
        if (!isInternetConnected(connectivityManager)) {
            Log.d("Tag", "no internet")
            Toast.makeText(context, "not connected to internet", Toast.LENGTH_LONG).show()
            return
        }


        val serviceRunning =  preference?.getBoolean(Constants.ServiceRunning, false)
        //serviceRunning = !serviceRunning

        Log.d("Tag", "toggleUploaderService . $serviceRunning")

        if (serviceRunning == false) {
            Log.d("Tag", "toggleUploaderService . $serviceRunning ... internal")
            Toast.makeText(context, "Service started", Toast.LENGTH_SHORT).show()

            startUploaderService()

            //preference?.edit()?.putBoolean(Constants.ServiceRunning, true)?.apply()

        } else {
           context?.stopService(
                Intent(
                    context,
                    UploaderService::class.java
                )
            )
            //preference?.edit()?.putBoolean(Constants.ServiceRunning, false)?.apply()
            Toast.makeText(context, "Uploader stopped.", Toast.LENGTH_LONG).show()

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
    private fun startUploaderService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(
                Intent(
                    context,
                    UploaderService::class.java
                )
            )
        } else {
            context?.startService(
                Intent(
                    context,
                    UploaderService::class.java
                )
            )
        }
    }
}