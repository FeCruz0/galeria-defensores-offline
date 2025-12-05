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
import java.util.regex.Pattern

class RegisterFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutName = view.findViewById<TextInputLayout>(R.id.layout_name)
        val nameInput = view.findViewById<TextInputEditText>(R.id.input_name)
        
        val layoutEmail = view.findViewById<TextInputLayout>(R.id.layout_email)
        val emailInput = view.findViewById<TextInputEditText>(R.id.input_email)
        
        val layoutPassword = view.findViewById<TextInputLayout>(R.id.layout_password)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.input_password)

        val layoutConfirmPassword = view.findViewById<TextInputLayout>(R.id.layout_confirm_password)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.input_confirm_password)
        
        val registerButton = view.findViewById<Button>(R.id.btn_register)
        val googleButton = view.findViewById<Button>(R.id.btn_register_google)
        val loginText = view.findViewById<TextView>(R.id.text_login)

        // Password Requirements TextViews
        val textReqLength = view.findViewById<TextView>(R.id.text_req_length)
        val textReqCase = view.findViewById<TextView>(R.id.text_req_case)
        val textReqNumber = view.findViewById<TextView>(R.id.text_req_number)
        val textReqSpecial = view.findViewById<TextView>(R.id.text_req_special)

        // Real-time Password Validation
        passwordInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val password = s.toString()
                updatePasswordRequirements(password, textReqLength, textReqCase, textReqNumber, textReqSpecial)
            }
        })

        // Real-time Username Validation (Debounced)
        var usernameCheckJob: kotlinx.coroutines.Job? = null
        nameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                usernameCheckJob?.cancel()
                val name = s.toString().trim()
                
                if (name.isNotEmpty()) {
                    if (name.contains(" ")) {
                        layoutName.error = "Não pode conter espaços"
                    } else if (name.length < 5) {
                        layoutName.error = "Mínimo de 5 caracteres"
                    } else if (name.length > 15) {
                        layoutName.error = "Máximo de 15 caracteres"
                    } else if (!name.matches(Regex("^[a-zA-Z0-9]+$"))) {
                        layoutName.error = "Apenas letras e números"
                    } else if (name.contains(Regex("(.)\\1\\1"))) {
                        layoutName.error = "Muitos caracteres repetidos"
                    } else {
                        // Clear error temporarily while checking or if valid length
                        layoutName.error = null 
                        usernameCheckJob = viewLifecycleOwner.lifecycleScope.launch {
                            kotlinx.coroutines.delay(500) // Debounce 500ms
                            val isTaken = UserRepository.isUsernameTaken(name)
                            if (isTaken) {
                                layoutName.error = "Nome de usuário já existe"
                            } else {
                                layoutName.error = null
                            }
                        }
                    }
                } else {
                    layoutName.error = null
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Reset errors (except name if it's already showing taken or short or repeated)
            if (layoutName.error != "Nome de usuário já existe") {
                layoutName.error = null
            }
            layoutEmail.error = null
            layoutPassword.error = null
            layoutConfirmPassword.error = null

            var isValid = true

            if (name.isEmpty()) {
                layoutName.error = "Digite seu nome"
                isValid = false
            } else if (name.contains(" ")) {
                layoutName.error = "Não pode conter espaços"
                isValid = false
            } else if (name.length < 5) {
                layoutName.error = "Mínimo de 5 caracteres"
                isValid = false
            } else if (name.length > 15) {
                layoutName.error = "Máximo de 15 caracteres"
                isValid = false
            } else if (!name.matches(Regex("^[a-zA-Z0-9]+$"))) {
                layoutName.error = "Apenas letras e números"
                isValid = false
            } else if (name.contains(Regex("(.)\\1\\1"))) {
                layoutName.error = "Muitos caracteres repetidos"
                isValid = false
            }

            if (email.isEmpty()) {
                layoutEmail.error = "Digite seu e-mail"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                layoutEmail.error = "E-mail inválido"
                isValid = false
            }

            if (password.isEmpty()) {
                layoutPassword.error = "Digite uma senha"
                isValid = false
            } else {
                val passwordError = validatePasswordStrength(password)
                if (passwordError != null) {
                    layoutPassword.error = "Senha não atende aos requisitos"
                    isValid = false
                }
            }

            if (confirmPassword.isEmpty()) {
                layoutConfirmPassword.error = "Confirme sua senha"
                isValid = false
            } else if (password != confirmPassword) {
                layoutConfirmPassword.error = "As senhas não coincidem"
                isValid = false
            }

            if (isValid && layoutName.error == null) {
                // Double check for unique username before submitting
                viewLifecycleOwner.lifecycleScope.launch {
                    val isTaken = UserRepository.isUsernameTaken(name)
                    if (isTaken) {
                        layoutName.error = "Nome de usuário já existe"
                    } else {
                        FirebaseAuthManager.register(name, email, password,
                            onSuccess = { user -> handleSuccessfulLogin(user) },
                            onError = { error -> showErrorDialog("Erro Cadastro", error) }
                        )
                    }
                }
            }
        }

        googleButton.setOnClickListener {
            val signInClient = FirebaseAuthManager.getGoogleSignInClient(requireActivity())
            googleSignInLauncher.launch(signInClient.signInIntent)
        }

        loginText.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun updatePasswordRequirements(
        password: String,
        reqLength: TextView,
        reqCase: TextView,
        reqNumber: TextView,
        reqSpecial: TextView
    ) {
        val green = android.graphics.Color.parseColor("#4CAF50") // Green
        val red = android.graphics.Color.parseColor("#F44336")   // Red
        val gray = android.graphics.Color.parseColor("#808080")  // Gray (Default)

        fun updateColor(view: TextView, isValid: Boolean) {
            view.setTextColor(if (isValid) green else if (password.isEmpty()) gray else red)
        }

        updateColor(reqLength, password.length >= 8)
        updateColor(reqCase, password.matches(".*[A-Z].*".toRegex()) && password.matches(".*[a-z].*".toRegex()))
        updateColor(reqNumber, password.matches(".*[0-9].*".toRegex()))
        // Expanded special characters regex
        updateColor(reqSpecial, password.matches(".*[@#\$%^&+=!.,:;()\\[\\]{}<>?|/\\\\-_].*".toRegex()))
    }

    private fun validatePasswordStrength(password: String): String? {
        if (password.length < 8) return "Mínimo 8 caracteres"
        if (!password.matches(".*[A-Z].*".toRegex())) return "Precisa de uma letra maiúscula"
        if (!password.matches(".*[a-z].*".toRegex())) return "Precisa de uma letra minúscula"
        if (!password.matches(".*[0-9].*".toRegex())) return "Precisa de um número"
        // Expanded special characters regex
        if (!password.matches(".*[@#\$%^&+=!.,:;()\\[\\]{}<>?|/\\\\-_].*".toRegex())) return "Precisa de um caractere especial"
        return null
    }

    private fun handleSuccessfulLogin(user: com.galeria.defensores.models.User) {
        viewLifecycleOwner.lifecycleScope.launch {
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
