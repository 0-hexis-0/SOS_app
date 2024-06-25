package com.example.sos

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.widget.*
import androidx.core.content.ContextCompat
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.geometry.Point
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.yandex.mapkit.Animation
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import okhttp3.*
import java.io.IOException
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {
    lateinit var mapview: MapView
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapObjects: MapObjectCollection
    private lateinit var userMarker: PlacemarkMapObject
    private lateinit var phoneNumberEditText: EditText
    private lateinit var sosButton: Button
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val TELEGRAM_BOT_TOKEN = "7413186571:AAGliCfbCYIVYWdPGaVjIcbEh8JJ2jZ49Ks"
    private val CHAT_ID = "711136853"
    private var isPhoneNumberInputVisible = false
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null
    lateinit var reasonSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("13164b6f-68f0-4b4d-ad4e-040178426e83")
        MapKitFactory.initialize(this)
        setContentView(R.layout.fragment_main_window)
        mapview = findViewById(R.id.mapview)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        sosButton = findViewById(R.id.sosButton)
        reasonSpinner = findViewById(R.id.reasonSpinner)

        mapObjects = mapview.map.mapObjects.addCollection()
        mapview.map.move(
            CameraPosition(Point(43.682770, 40.266281), 14.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 3f), null
        )
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val reasons = arrayOf("Падение. Возможен перелом.", "Вылетел с трасы. Возможны повреждения.", "Попал под лавину", "Другое")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reasons)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reasonSpinner.adapter = adapter

        sosButton.setOnClickListener {
            if (isPhoneNumberInputVisible) {
                val phoneNumber = phoneNumberEditText.text.toString()
                val reason = reasonSpinner.selectedItem.toString()
                if (userLatitude != null && userLongitude != null && isValidPhoneNumber(phoneNumber)) {
                    sendLocationToTelegramBot(userLatitude!!, userLongitude!!, phoneNumber, reason)
                } else {
                    Toast.makeText(this, "Пожалуйста, введите корректный номер телефона", Toast.LENGTH_SHORT).show()
                }
            } else {
                phoneNumberEditText.visibility = EditText.VISIBLE
                reasonSpinner.visibility = Spinner.VISIBLE
                isPhoneNumberInputVisible = true
                getCurrentLocation()
            }
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this, "В разрешении на определение местоположения отказано", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnCompleteListener(this) { task: Task<android.location.Location?> ->
            if (task.isSuccessful) {
                val location = task.result
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    val userLocation = Point(userLatitude!!, userLongitude!!)
                    mapview.map.move(
                        CameraPosition(userLocation, 14.0f, 0.0f, 0.0f)
                    )


                    if (::userMarker.isInitialized) {
                        mapObjects.remove(userMarker)
                    }
                    userMarker = mapObjects.addPlacemark(userLocation)
                    userMarker.setIcon(ImageProvider.fromResource(this, R.drawable.user_location))

                    Toast.makeText(
                        this, "Ваша позиция: (${location.latitude}, ${location.longitude})", Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "Включите геолокацию", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Не удается определить местоположение", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun sendLocationToTelegramBot(latitude: Double, longitude: Double, phoneNumber: String, reason: String) {
        val url = "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage"
        val client = OkHttpClient()

        val mapUrl = "https://yandex.ru/maps/?pt=$longitude,$latitude&z=14&l=map"
        val message = "Телефон: $phoneNumber\nПричина: $reason\nКарта: $mapUrl"

        val requestBody = FormBody.Builder()
            .add("chat_id", CHAT_ID)
            .add("text", message)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity, "Не удалось отправить местоположение", Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, "Местоположение отправлено, ожидайте", Toast.LENGTH_SHORT
                        ).show()

                        phoneNumberEditText.setText("")
                        phoneNumberEditText.visibility = EditText.GONE
                        reasonSpinner.visibility = Spinner.GONE
                        isPhoneNumberInputVisible = false
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, "Не удалось отправить местоположение.", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }



    private fun isValidPhoneNumber(phoneNumber: String): Boolean {

        val pattern = Pattern.compile("^((\\+7|7|8)+([0-9]){10})\$")
        return pattern.matcher(phoneNumber).matches()
    }



    override fun onStop() {
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapview.onStart()
    }
}
