package com.example.anton.aplicacionkotlin

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

class DescargarArchivos : AppCompatActivity() {
    private var listView: ListView? = null
    private var arrayAdapter: ArrayAdapter<*>? = null
    private val files_on_server = ArrayList<String>()
    private var handler: Handler? = null
    private var selected_file: String? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descargar_archivos)
        comprobarPermisos()
        rellenarLista()
    }

    private fun comprobarPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                return
            }
        }
        rellenar()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            rellenar()
        } else {
            comprobarPermisos()
        }
    }
    private fun rellenarLista(){
        val t = Thread(Runnable {
            val client = OkHttpClient()
            val request = Request.Builder()
                    .url("http://192.168.1.200/javahamuerto2/download_file.php?list_files")
                    .build()
            var response: Response?
            try {
                response = client.newCall(request).execute()
                val array = JSONArray(response!!.body().string())
                for (i in 0 until array.length()) {
                    val file_name = array.getString(i)
                    if (files_on_server.indexOf(file_name) == -1)
                        files_on_server.add(file_name)
                }
                handler!!.post(Runnable { arrayAdapter!!.notifyDataSetChanged() })
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        })
        t.start()
    }
    private fun rellenar() {
        listView = findViewById(R.id.listView) as ListView
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, files_on_server)
        listView!!.setAdapter(arrayAdapter)
        handler = Handler()
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Downloading...")
        progressDialog!!.setMax(100)
        progressDialog!!.setCancelable(false)
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        listView!!.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            selected_file = (view as TextView).text.toString()
            val t = Thread(Runnable {
                val client = OkHttpClient()
                val request = Request.Builder()
                        .url("http://192.168.1.200/javahamuerto2/files/" + selected_file)
                        .build()
                var response: Response?
                try {
                    response = client.newCall(request).execute()
                    val file_size = response!!.body().contentLength().toFloat()
                    val inputStream = BufferedInputStream(response?.body()?.byteStream())
                    val stream = FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/Download/" + selected_file)
                    val data = ByteArray(8192)
                    var total = 0f
                    var read_bytes = 0
                    handler!!.post(Runnable { progressDialog!!.show() })
                    read_bytes = inputStream.read(data)
                    while (read_bytes != -1) {
                        total = total + read_bytes
                        stream.write(data, 0, read_bytes)
                        progressDialog?.setProgress((total / file_size * 100).toInt())
                        read_bytes = inputStream.read(data)
                    }
                    progressDialog?.dismiss()
                    stream.flush()
                    stream.close()
                    response!!.body().close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
            t.start()
        }
    }
}
