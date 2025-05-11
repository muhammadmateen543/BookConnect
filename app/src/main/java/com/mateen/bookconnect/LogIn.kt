package com.mateen.bookconnect

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var btlogin: RelativeLayout
    private lateinit var btgotosignup: TextView
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth=FirebaseAuth.getInstance()
        setContentView(R.layout.activity_log_in)
        btlogin=findViewById(R.id.btlogin)
        btgotosignup=findViewById(R.id.bt_goto_signup)
        etEmail=findViewById(R.id.email)
        etPassword=findViewById(R.id.password)
        btlogin.setOnClickListener({
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            when {
                email.isEmpty() -> ShowError(this).showError("Email is required.")
                password.isEmpty() -> ShowError(this).showError("Password is required.")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ShowError(
                    this
                ).showError("Invalid email address.")
                password.length < 8 -> ShowError(this).showError("Password must be at least 8 characters.")
                else ->
                {
                    auth.signInWithEmailAndPassword(etEmail.text.toString(), etPassword.text.toString()).addOnCompleteListener {
                            task->
                        if (task.isSuccessful)
                        {
                            var intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else
                        {
                            ShowError(this).showError("Unable to Login.")
                        }
                    }
                }
            }
        })
        btgotosignup.setOnClickListener {
            var intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }
}