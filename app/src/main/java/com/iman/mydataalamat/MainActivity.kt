package com.iman.mydataalamat

//Langkah 1 : Import ekstensi yang akan di gunakan
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.iman.mydataalamat.databinding.ActivityMainBinding
import com.iman.mydataalamat.model.InternalFileRepository
import com.iman.mydataalamat.model.Note
import com.iman.mydataalamat.model.NoteRepository
import java.util.*
import java.util.jar.Attributes

class MainActivity : AppCompatActivity() {
    private val repo: NoteRepository by lazy { InternalFileRepository(this) }

    private lateinit var mainBinding: ActivityMainBinding

    //Langkah 2 : Membuat klien layanan lokasi
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2

    //Langkag 3 : Membuat instance dari Klien Penyedia Lokasi Fusi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mainBinding.btnLocation.setOnClickListener {
            getLocation()
        }

        mainBinding.btnshare.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                val logData1 = mainBinding.AlamatLengkap.text.toString()
                val logData2 = mainBinding.Wilayah.text.toString()
                val logData3 = mainBinding.NamaNegara.text.toString()
                val logData4 = mainBinding.editNama.text.toString()

                putExtra(Intent.EXTRA_TEXT, "Nama : $logData4\n $logData1 :\n $logData2 :\n $logData3 :\n")
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)

        }
        mainBinding.btnCompile.setOnClickListener {
            var logDataSensor =
                mainBinding.editTeksCatatan.text.toString()
            mainBinding.editFileName.text.toString()

            val logData4 =
                mainBinding.editNama.text.toString()
            val logData1 =
                mainBinding.AlamatLengkap.text.toString()
            val logData2 =
                mainBinding.Wilayah.text.toString()
            val logData3 =
                mainBinding.NamaNegara.text.toString()

            mainBinding.editFileName.setText("$logData4")

            logDataSensor = ">$logData1\n >$logData2\n >$logData3\n $logDataSensor"
            mainBinding.editTeksCatatan.setText(logDataSensor)
        }

        mainBinding.btnWrite.setOnClickListener {
            if (mainBinding.editFileName.text.isNotEmpty()) {
                try {
                    repo.addNote(
                        Note(
                            mainBinding.editFileName.text.toString(),
                            mainBinding.editTeksCatatan.text.toString()
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "File Write Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                mainBinding.editFileName.text.clear()
                mainBinding.editTeksCatatan.text.clear()
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }
        mainBinding.btnRead.setOnClickListener {
            if (mainBinding.editFileName.text.isNotEmpty()) {
                try {
                    val note = repo.getNote(mainBinding.editFileName.text.toString())
                    mainBinding.editTeksCatatan.setText(note.noteText)
                } catch (e: Exception) {
                    Toast.makeText(this, "File Read Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }
        mainBinding.btnDelete.setOnClickListener {
            if (mainBinding.editFileName.text.isNotEmpty()) {
                try {
                    if (repo.deleteNote(mainBinding.editFileName.text.toString())) {
                        Toast.makeText(this, "File Deleted", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "File Could Not Be Deleted", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "File Delete Failed", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
                mainBinding.editFileName.text.clear()
                mainBinding.editTeksCatatan.text.clear()
            } else {
                Toast.makeText(this, "Please provide a Filename", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (checkPermissions()) {
//Langkah 4 : // Mendapat lokasi terakhir yang diketahui.
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
//Langkah 5 : Mendapatkan data lokasi dari perangkat (latitude, longitude)
                    val location: Location? = task.result
                    if (location != null) {
//Langkah 6 :  Menggunakan geocoder untuk mendapatkan data lokasi
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val list: List<Address> =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)

//Langkah 7 : Menampilkan hasil dari geocoder
                        mainBinding.apply {
                            editNama.text
                            NamaNegara.text = "NAMA NEGARA\n${list[0].countryName} "
                            Wilayah.text = "WILAYAH/AREA\n${list[0].locality}"
                            AlamatLengkap.text = "ALAMAT LENGKAP\n${list[0].getAddressLine(0)}"
                        }
                    }
                }
            } else {
//Langkah 8 : Menampilkan izin untuk mengaktifkan GPS/lokasi
                Toast.makeText(this, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    //Langkah 9 : Membuat sebuah fungsi untuk mengecek aktif lokasi dari gps atau lokasi dari provider internet yang di gunakan device
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    //Langkah 10 : Mengambil koordinat lokasi device
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    //Langkah 11 : Jika gagal, dapat meruequest kembali lokasi dengan akurasi utama
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }

    //Langkah 12 : Untuk mendapatkan response perizinan dari pengguna
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }
}