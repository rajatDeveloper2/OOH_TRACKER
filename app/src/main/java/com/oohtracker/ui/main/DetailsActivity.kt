package com.oohtracker.ui.main

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oohtracker.MainApplication
import com.oohtracker.R
import com.oohtracker.adapter.FileDataAdapter
import com.oohtracker.room.FileDataViewModel
import com.oohtracker.room.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class DetailsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private var fileDataViewModel: FileDataViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        fileDataViewModel = MainApplication.getFileDataViewModel()

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fileDataViewModel?.allWords?.observe(this) { root ->
            if (root != null) {
                val definitionAdapter =
                    FileDataAdapter(root)
                recyclerView.adapter = definitionAdapter
            }
        }


        /*findViewById<Button>(R.id.ButtonAdd).setOnClickListener {

            fileDataViewModel?.insert(
                Word(
                    "_-_" + System.currentTimeMillis().toString() + "_Test_0_0_000",
                    "M"
                )
            )

            Log.d("Tag", "Inserted something.")

        }

        findViewById<Button>(R.id.buttonCheck).setOnClickListener {
            Log.d("Tag", fileDataViewModel?.allWords?.value?.size.toString())
        }

        findViewById<Button>(R.id.buttonDel).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {

                fileDataViewModel?.delete(
                    fileDataViewModel?.allWords?.value?.get(0)?.word
                )

                Log.d("Tag", "deleting")
            }
        }*/

    }
}