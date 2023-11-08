package com.constantlearningdad.cameraxexperiment

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.constantlearningdad.cameraxexperiment.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    //if using CameraController
    private lateinit var cameraController: LifecycleCameraController

    //if using CameraProvider
//    private var imageCapture : ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!hasPermissions(baseContext)){
            //Request camera-related permissions
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }
        else
            startCamera()

        binding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera(){
        val previewView : PreviewView = binding.viewFinder
        cameraController = LifecycleCameraController(baseContext)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        previewView.controller = cameraController
    }

    private fun takePhoto() {
        //create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        //Create output options objects which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}",exception)
                }

                override fun onImageSaved(output : ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
                    Log.d(TAG,msg)
                }
            }
        )
    }


    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        {
            permissions ->

            var permissionGranted = true

            permissions.entries.forEach{
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }

            if (!permissionGranted)
                Toast.makeText(this, "Permission request denied",Toast.LENGTH_LONG).show()
            else
                startCamera()
        }


    //equivalent to static variables
    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA
            )
                //the .apply is only required for older devices that have external storage
                .apply {
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                        add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }.toTypedArray()

        fun hasPermissions(context: Context) = REQUIRED_PERMISSIONS.all{
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}