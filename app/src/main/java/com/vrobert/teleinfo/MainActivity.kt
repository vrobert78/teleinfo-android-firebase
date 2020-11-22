package com.vrobert.teleinfo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.github.anastr.speedviewlib.SpeedView
import com.github.anastr.speedviewlib.components.Section
import com.github.anastr.speedviewlib.components.Style
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.util.*


const val EXTRA_MESSAGE = "com.vrobert.teleinfo.MESSAGE"

class MainActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var myListener: ValueEventListener

    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 123

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // already signed in
            Log.d(TAG, "Already signed in")
            basicReadWrite()
            initGauge()
        } else {
            Log.d(TAG, "Not logged")
            createSignInIntent()
        }

    }

    private fun initGauge() {
        setContentView(R.layout.activity_main)

        var speedometer: SpeedView = findViewById(R.id.speedView)
        speedometer.clearSections()
        speedometer.addSections(
            Section(0f, .11f, Color.GREEN, 40f, Style.BUTT),
            Section(.11f, .4f, Color.YELLOW, 40f, Style.BUTT),
            Section(.4f, .75f, Color.parseColor("#ffa500"), 40f, Style.BUTT),
            Section(.75f, 1f, Color.RED, 40f, Style.BUTT)
        )
        speedometer.tickNumber = 10
        speedometer.onPrintTickLabel = { tickPosition: Int, tick: Float ->
            if (tick >= 1000)
                String.format(Locale.getDefault(), "%.1f kW", tick / 1000f)
            else
                null
            // null means draw default tick label.
            // also you can return SpannableString to change color, textSize, lines...
        }

        speedometer.unit="Watts"
        speedometer.withTremble = false

    }

    fun signOut(view: View) {
        // [START auth_fui_signout]

        Log.d(TAG, "Logout")
        database.getReference("HOMETIC").removeEventListener(myListener)

        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                createSignInIntent()
            }
        // [END auth_fui_signout]
    }


    private fun createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
        // [END auth_fui_create_intent]
    }


    // [START auth_fui_result]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "in onActivityResult")

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser

                Log.d(TAG, "ok")

                basicReadWrite()
                initGauge()
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...

                Log.d(TAG, "nok")

                createSignInIntent()
            }
        }
    }
    // [END auth_fui_result]

    private fun basicReadWrite() {
        database = Firebase.database

        myListener = database.getReference("HOMETIC").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                getAndDisplay(dataSnapshot, "PAPP", " Watts", R.id.Puissance)
                getAndDisplay(dataSnapshot, "DATETIME", "", R.id.Horodatage)
                getAndDisplay(dataSnapshot, "PTEC", "", R.id.HPHC)
                getAndDisplay(dataSnapshot, "HCHC", "", R.id.HC)
                getAndDisplay(dataSnapshot, "HCHP", "", R.id.HP)

                val value = dataSnapshot.child("PAPP").getValue<Float>()

                var speedometer: SpeedView = findViewById(R.id.speedView)
                speedometer.speedTo(value!!)


                //Log.d(TAG, "Value is: $value")
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })

    }

    fun getAndDisplay(dataSnapshot: DataSnapshot, key: String, unite: String, id: Int) {
        val value = dataSnapshot.child(key).getValue()

        findViewById<TextView>(id).apply {
            text = value.toString() + unite
        }
    }
}