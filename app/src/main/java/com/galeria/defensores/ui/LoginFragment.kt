package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.galeria.defensores.R
import com.galeria.defensores.data.FirebaseAuthManager
import com.galeria.defensores.data.SessionManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider

class LoginFragment : Fragment() {

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameInput = view.findViewById<EditText>(R.id.input_name)
        val emailInput = view.findViewById<EditText>(R.id.input_email)
        val passwordInput = view.findViewById<EditText>(R.id.input_password)
        val loginButton = view.findViewById<Button>(R.id.btn_login)
        val forgotPasswordText = view.findViewById<TextView>(R.id.text_forgot_password)

        loginButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotBlank() && password.isNotBlank()) {
                // Try to Login first
                FirebaseAuthManager.login(email, password, 
                    onSuccess = { user ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            SessionManager.refreshUser()
                            navigateToTableList()
                        }
                    },
                    onError = { error ->
                        // If login fails, check if it's a new user (or just wrong password)
                        // For simplicity, if name is provided, assume registration intent
                        if (name.isNotBlank()) {
                            startRegistration(name, email, password)
                        } else {
                            showErrorDialog("Login falhou", "$error\n\nSe for novo usuário, preencha também o Nome para cadastrar.")
                        }
                    }
                )
            } else {
                Toast.makeText(context, "Preencha E-mail e Senha", Toast.LENGTH_SHORT).show()
            }
        }

        forgotPasswordText.setOnClickListener {
            val email = emailInput.text.toString()
            if (email.isNotBlank()) {
                startForgotPassword(email)
            } else {
                Toast.makeText(context, "Digite seu e-mail para recuperar a senha", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copiar Erro") { _, _ ->
                val clipboard = androidx.core.content.ContextCompat.getSystemService(requireContext(), android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("Error Log", message)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, "Erro copiado para a área de transferência", Toast.LENGTH_SHORT).show()
            }
            .create()
        dialog.show()
    }

    private fun startRegistration(name: String, email: String, password: String) {
        FirebaseAuthManager.register(name, email, password,
            onSuccess = { user ->
                // Save to Firestore
                viewLifecycleOwner.lifecycleScope.launch {
                    com.galeria.defensores.data.UserRepository.registerUser(user)
                    navigateToTableList()
                }
            },
            onError = { error ->
                showErrorDialog("Erro Cadastro", error)
            }
        )
    }

    private fun startForgotPassword(email: String) {
        FirebaseAuthManager.sendPasswordResetEmail(email,
            onSuccess = {
                Toast.makeText(context, "E-mail de recuperação enviado! Verifique sua caixa de entrada.", Toast.LENGTH_LONG).show()
            },
            onError = { error ->
                showErrorDialog("Erro Recuperação", error)
            }
        )
    }

    private fun navigateToTableList() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TableListFragment())
            .commit()
    }
}
