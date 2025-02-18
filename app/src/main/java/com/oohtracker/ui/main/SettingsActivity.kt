package com.oohtracker.ui.main

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.oohtracker.R


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        // below line is to change
        // the title of our action bar.

        // below line is to change
        // the title of our action bar.
        /*setSupportActionBar(findViewById(R.id.my_toolbarSettings))
        */
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // below line is used to check if
        // frame layout is empty or not.

        // below line is used to check if
        // frame layout is empty or not.
        if (findViewById<View?>(R.id.idFrameLayout) != null) {
            if (savedInstanceState != null) {
                return
            }
            // below line is to inflate our fragment.
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.idFrameLayout, SettingsFragment())
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}