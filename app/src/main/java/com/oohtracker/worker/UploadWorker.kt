package com.oohtracker.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.oohtracker.MainApplication
import com.oohtracker.room.FileDataViewModel
import com.oohtracker.service.UploaderService


class UploadWorker(private val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        //uploadImages()
        startUploaderService(appContext)
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    private fun startUploaderService(appContext: Context) {
        /*val fileDataViewModel: FileDataViewModel? = MainApplication.getFileDataViewModel()
        val size = fileDataViewModel?.allWords?.value?.size
        if (size != null) {
            if (size > 0)

        }*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(
                Intent(
                    appContext,
                    UploaderService::class.java
                )
            )
        } else {
            appContext.startService(
                Intent(
                    appContext,
                    UploaderService::class.java
                )
            )
        }
    }
}
