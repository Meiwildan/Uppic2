package com.example.uppic

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.icu.text.CaseMap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.uppic.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.coroutineContext

private const val REQUEST_CODE = 72
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var title: String

    private val storageReference = FirebaseStorage.getInstance().getReference("uploads")

    private var imageUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setImageViewHome()
        initAction()
    }

    private fun deleteImage(title: String) = CoroutineScope(Dispatchers.IO).launch {
        try {
            storageReference.child(title).delete().await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully deleted image", Toast.LENGTH_SHORT).show()
                resetlayout()
            }
        }catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.inputTextTitle.error = e.message
                binding.progressBarLoadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun downloadImage(title: String) = CoroutineScope(Dispatchers.IO).launch {
        try{
            val maxDownloadSize = 5L * 1024 * 1024
            val bytes = storageReference.child(title).getBytes(maxDownloadSize).await()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            withContext(Dispatchers.Main){
                binding.imageViewHome.load(bitmap){
                    crossfade(true)
                    crossfade(500)
                    transformations(RoundedCornersTransformation(15f))
                }
                binding.progressBarLoadingIndicator.visibility = View.GONE
            }
        } catch (e: Exception){
            withContext(Dispatchers.Main) {
                binding.inputTextTitle.error = e.message
                binding.progressBarLoadingIndicator.visibility = View.GONE
                setImageViewHome()
            }
        }
    }

    private fun resetlayout () {
        setImageViewHome()
        imageUri = null
        binding.inputTextTitle.error = null
        binding.editTextTitle.text?.clear()
        binding.progressBarLoadingIndicator.visibility = View.GONE
        binding.textViewIndicatorLoading.visibility = View.GONE
    }

    private fun uploadImage(title : String) = CoroutineScope(Dispatchers.IO).launch {
        try{
            imageUri.let { uri ->
                if (uri != null) {
                    storageReference.child(title).putFile(uri)
                        .addOnProgressListener {
                            val progress: Int = ((100 * it.bytesTransferred) / it.totalByteCount).toInt()
                            binding.progressBarLoadingIndicator.progress = progress
                            val indicatorText = "Loading... $progress%"
                            binding.textViewIndicatorLoading.text = indicatorText
                        }.await()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Success Uploaded !", Toast.LENGTH_SHORT).show()
                        delay(3000L)
                        resetlayout()
                    }
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                resetlayout()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE){
            data?.data?.let {
                imageUri = it
                binding.imageViewHome.load(imageUri) {
                    crossfade(true)
                    crossfade(500)
                    transformations(RoundedCornersTransformation(15f))
                }
            }
        }
    }

    private fun initAction() {
        binding.buttonSelectImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type="image/*"
                startActivityForResult(it, REQUEST_CODE)
            }
        }

        binding.buttonUploadImage.setOnClickListener {
            title = binding.editTextTitle.text.toString().trim()
            if (imageUri != null) {
                if (title.isBlank() || title.isEmpty()) {
                    binding.inputTextTitle.error = "*required"
                } else {
                    binding.progressBarLoadingIndicator.isIndeterminate = false
                    binding.progressBarLoadingIndicator.visibility = View.VISIBLE
                    binding.textViewIndicatorLoading.visibility = View.VISIBLE
                    binding.inputTextTitle.error = null
                    uploadImage(title)
                }
            } else {
                Toast.makeText(this, "Select Image!", Toast.LENGTH_SHORT).show()

            }
            }
        binding.buttonDownloadImage.setOnClickListener{
            title = binding.editTextTitle.text.toString().trim()
            if (title.isEmpty() || title.isEmpty()) {
                binding.inputTextTitle.error = "*Required"
            } else {
                binding.progressBarLoadingIndicator.isIndeterminate = true
                binding.progressBarLoadingIndicator.visibility = View.VISIBLE
                binding.inputTextTitle.error = null
                downloadImage(title)
            }
        }
        binding.buttonDeleteImage.setOnClickListener{
            title = binding.editTextTitle.text.toString().trim()
            if (title.isEmpty() || title.isEmpty()) {
                binding.inputTextTitle.error = "*Required"
            } else {
                binding.progressBarLoadingIndicator.isIndeterminate = true
                binding.progressBarLoadingIndicator.visibility = View.VISIBLE
                binding.inputTextTitle.error = null
                deleteImage(title)
            }
        }
    }

    private fun setImageViewHome() {
        binding.imageViewHome.load(ContextCompat.getDrawable(this, R.drawable.shape)) {
            crossfade(true)
            crossfade(500)
            transformations(RoundedCornersTransformation(15f))
        }
    }
}