package com.example.expensio.Auth

import com.example.expensio.R
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private var emailEditText: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        emailEditText = findViewById(R.id.editText_email)
        val submitButton: Button = findViewById(R.id.button_reset_password)
        submitButton.setOnClickListener(View.OnClickListener {
            val email = emailEditText?.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                emailEditText?.setError("Email is required.")
                return@OnClickListener
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Password reset email sent.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "Failed to send password reset email.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        })
    }
}