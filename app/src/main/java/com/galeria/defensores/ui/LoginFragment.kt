package com.galeria.defensores.ui

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.galeria.defensores.R
import com.galeria.defensores.data.FirebaseAuthManager
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    // Google Sign In Launcher
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    FirebaseAuthManager.firebaseAuthWithGoogle(idToken,
                        onSuccess = { user ->
                            handleSuccessfulLogin(user)
                        },
                        onError = { error ->
                            showErrorDialog("Erro Google", error)
                        }
                    )
                }
            } catch (e: ApiException) {
                showErrorDialog("Erro Google", "Sign in failed: ${e.statusCode}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutEmail = view.findViewById<TextInputLayout>(R.id.layout_email)
        val emailInput = view.findViewById<TextInputEditText>(R.id.input_email)
        
        val layoutPassword = view.findViewById<TextInputLayout>(R.id.layout_password)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.input_password)
        
        val loginButton = view.findViewById<Button>(R.id.btn_login)
        val googleButton = view.findViewById<Button>(R.id.btn_login_google)
        val forgotPasswordText = view.findViewById<TextView>(R.id.text_forgot_password)
        val registerText = view.findViewById<TextView>(R.id.text_register)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Reset errors
            layoutEmail.error = null
            layoutPassword.error = null

            var isValid = true

            if (email.isEmpty()) {
                layoutEmail.error = "Digite seu e-mail"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                layoutEmail.error = "E-mail inválido"
                isValid = false
            }

            if (password.isEmpty()) {
                layoutPassword.error = "Digite sua senha"
                isValid = false
            }

            if (isValid) {
                FirebaseAuthManager.login(email, password, 
                    onSuccess = { user -> handleSuccessfulLogin(user) },
                    onError = { error ->
                        showErrorDialog("Login falhou", error)
                    }
                )
            }
        }

        googleButton.setOnClickListener {
            val signInClient = FirebaseAuthManager.getGoogleSignInClient(requireActivity())
            googleSignInLauncher.launch(signInClient.signInIntent)
        }

        forgotPasswordText.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ForgotPasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        registerText.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun handleSuccessfulLogin(user: com.galeria.defensores.models.User) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Ensure user exists in Firestore
            UserRepository.registerUser(user)
            SessionManager.refreshUser()
            navigateToTableList()
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()
        dialog.show()
    }

    private fun navigateToTableList() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TableListFragment())
            .commit()
    }
}
