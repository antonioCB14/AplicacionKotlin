package com.example.anton.aplicacionkotlin

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.TextView
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import okhttp3.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var mMediaSession: MediaSessionCompat? = null
    var mStateBuilder: PlaybackStateCompat.Builder? = null
    private var bEnviar: Button? = null
    private var bRecibir: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bEnviar = findViewById(R.id.bEnviar) as Button
        bRecibir = findViewById(R.id.bRecibir) as Button

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100)
                return
            }
        }
        habilitarBoton()
    }

    private fun habilitarBoton() {
        bEnviar?.setOnClickListener(View.OnClickListener {
            MaterialFilePicker()
                    .withActivity(this@MainActivity)
                    .withRequestCode(10)
                    .start()
        })
        bRecibir?.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, DescargarArchivos::class.java)
            startActivityForResult(intent,11)
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            habilitarBoton()
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 100)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
            var mensaje = dialogView.findViewById<TextView>(R.id.etMensaje)
            mensaje.text = "Uploading"
            builder.setView(dialogView)
            builder.setCancelable(true)
            val dialog = builder.create()
            dialog.show()

            val t = Thread(Runnable {
                val f = File(data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH))
                val content_type = getMimeType(f.path)
                val file_path = f.absolutePath
                val client = OkHttpClient()
                val file_body = RequestBody.create(MediaType.parse(content_type), f)
                val request_body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("type", content_type)
                        .addFormDataPart("uploaded_file", file_path.substring(file_path.lastIndexOf("/") + 1), file_body)
                        .build()
                val request = Request.Builder()
                        .url("http://192.168.1.200/javahamuerto2/save_file.php")
                        .post(request_body)
                        .build()
                try {
                    val response: Response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw IOException("Error : " + response)
                    }
                    dialog.dismiss()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
            t.start()
        }
    }

    private fun getMimeType(path: String): String {
        var extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
