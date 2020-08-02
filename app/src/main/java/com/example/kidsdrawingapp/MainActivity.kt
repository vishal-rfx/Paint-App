package com.example.kidsdrawingapp

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.android.synthetic.main.dialog_color_palette.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    //private var mImageButtonCurrent : ImageButton? = null
    private var colorPalette:Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(20.toFloat())

        //mImageButtonCurrent = ll_0[1] as ImageButton

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_color_palette.setOnClickListener {
            showColorPaletteDialog()
        }

        ib_gallery.setOnClickListener {
            if(isReadStorageAllowed()){
                // run our code to get the image from the gallery
                // Intents are used to move from one activity to another
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoIntent,GALLARY)
            }
            else{
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }

        ib_export.setOnClickListener {
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }
            else{
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLARY){
                try {
                    if (data!!.data != null){
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }else{
                        Toast.makeText(
                            this,
                            "Error in getting the image or its corrupted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }catch (e : Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)// Context in which it should be displayed
        brushDialog.setContentView(R.layout.dialog_brush_size) // To set its content
        brushDialog.setTitle("Brush size: ")
        brushDialog.show()

        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener{
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener{
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener{
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

    }

    private fun showColorPaletteDialog(){
        colorPalette = Dialog(this)
        colorPalette!!.setContentView(R.layout.dialog_color_palette)
        colorPalette!!.setTitle("Select color: ")
        colorPalette!!.show()

    }

    fun paintClicked(view: View){
        val imageButton = view as ImageButton
        val colorTag = imageButton.tag.toString()
        drawing_view.setColor(colorTag)
        colorPalette!!.dismiss()

    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,
                    "Need permission to add a Background image",
                    Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

   /* override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this,"Permission denied",Toast.LENGTH_LONG).show()
            }
        }
    }*/
    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun isWriteStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }




    private inner class BitmapAsyncTask(val mBitmap: Bitmap): AsyncTask<Any, Void, String>(){

        private lateinit var mProgressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg p0: Any?): String {
          return  saveToGallery()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if(!result!!.isEmpty()){
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully at $result",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else{

                Toast.makeText(
                    this@MainActivity,
                    "something went wrong while saving",
                    Toast.LENGTH_SHORT
                ).show()
            }
            shareImage(Uri.parse(result))

        }


        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }

        private fun shareImage(uri: Uri){
            val intent = Intent(Intent.ACTION_SEND).apply{
                type="image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(
                Intent.createChooser(
                    intent, "Share"
                )
            )
        }

        @SuppressLint("SimpleDateFormat")
        private fun createFilename(filename : String) : String{
            val formatter = SimpleDateFormat("YYYYMMdd-HHmm.ssSSS")
            val dateString = formatter.format(Date()) + "_"

            return "$dateString$filename.jpg"

        }

        private fun saveToGallery(): String {
            var result : String = ""

            var resolver = this@MainActivity.contentResolver

            val foldername = packageManager.getApplicationLabel(applicationInfo).toString().replace(" ", "")
            val filename = createFilename(foldername)
            val saveLocation = Environment.DIRECTORY_PICTURES + File.separator + foldername

            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

                // RELATIVE_PATH and IS_PENDING are introduced in API 29.
                values.put(MediaStore.Images.Media.RELATIVE_PATH, saveLocation)
                values.put(MediaStore.Images.Media.IS_PENDING, true)


                val uri: Uri? = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                if (uri != null) {
                    //val outstream = resolver.openOutputStream(uri)

                    if (mBitmap != null) {
                        saveImageToStream(mBitmap, resolver.openOutputStream(uri))
                    }

                    values.put(MediaStore.Images.Media.IS_PENDING, false)

                    this@MainActivity.contentResolver.update(uri, values, null, null)

                    result = uri.toString()
                }
            }

            return result
        }

        private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {

            if (outputStream != null) {
                try {
                    mBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()
                } catch (e: Exception) {
                    Log.e("**Exception", "Could not write to stream")
                    e.printStackTrace()
                }
            }
        }
    }

    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLARY = 2
    }

}