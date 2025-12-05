package com.galeria.defensores.ui

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.galeria.defensores.R
import com.galeria.defensores.data.FirebaseAuthManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutEmail = view.findViewById<TextInputLayout>(R.id.layout_email)
        val emailInput = view.findViewById<TextInputEditText>(R.id.input_email)
        val sendButton = view.findViewById<Button>(R.id.btn_send_reset)
        val backText = view.findViewById<TextView>(R.id.text_back_login)

        sendButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            layoutEmail.error = null

            if (email.isEmpty()) {
                layoutEmail.error = "Digite seu e-mail"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                layoutEmail.error = "E-mail inválido"
            } else {
                FirebaseAuthManager.sendPasswordResetEmail(email,
                    onSuccess = {
                        Toast.makeText(context, "E-mail de recuperação enviado! Verifique sua caixa de entrada.", Toast.LENGTH_LONG).show()
                        parentFragmentManager.popBackStack() // Go back to login
                    },
                    onError = { error ->
                        showErrorDialog("Erro Recuperação", error)
                    }
                )
            }
        }

        backText.setOnClickListener {
            parentFragmentManager.popBackStack()
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
}
