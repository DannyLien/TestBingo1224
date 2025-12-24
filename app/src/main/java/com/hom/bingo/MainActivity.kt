package com.hom.bingo

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.hom.bingo.databinding.ActivityMainBinding
import java.util.Arrays

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {
    private val requestSignIn = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
        }
    }
    private var user: FirebaseUser? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.fab.setOnClickListener { view ->
            //
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(this)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_signout -> {
                auth.signOut()
                true
            }

            R.id.action_exit -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        user = p0.currentUser
        user?.also {
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("uid").setValue(it.uid)
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("displayName").setValue(it.displayName)

        } ?: signUp()
    }

    private fun signUp() {
        val signIn = AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(
                Arrays.asList(
                    EmailBuilder().build(),
                    GoogleBuilder().build(),
                )
            ).setIsSmartLockEnabled(false).build()
        requestSignIn.launch(signIn)
    }

}












