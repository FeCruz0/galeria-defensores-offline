package com.galeria.defensores.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.galeria.defensores.data.FirebaseAuthManager
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteAccountDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Digite sua senha para confirmar"

        val dialog = AlertDialog.Builder(context)
            .setTitle("Excluir Conta")
            .setMessage("Tem certeza? Esta ação é irreversível e apagará todos os seus dados.")
            .setView(input)
            .setPositiveButton("Excluir", null) // Set null to override later
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val password = input.text.toString()
                if (password.isNotBlank()) {
                    button.isEnabled = false // Prevent double clicks
                    performDelete(password, button)
                } else {
                    Toast.makeText(context, "Senha necessária.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        return dialog
    }

    private fun performDelete(password: String, dbButton: android.widget.Button) {
        val userId = SessionManager.currentUser?.id ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 0. Check for active tables
                val hasActiveTables = withContext(Dispatchers.IO) {
                    com.galeria.defensores.data.TableRepository.checkUserHasActiveTables(userId)
                }

                if (hasActiveTables) {
                    Toast.makeText(context, "Você possui mesas ativas com jogadores. Exclua as mesas ou transfira a posse antes de deletar sua conta.", Toast.LENGTH_LONG).show()
                    dbButton.isEnabled = true
                    return@launch
                }

                // 1. Re-authenticate FIRST
                withContext(Dispatchers.IO) {
                    FirebaseAuthManager.reauthenticate(password)
                }

                // 2. Delete Firestore Data
                withContext(Dispatchers.IO) {
                    UserRepository.deleteUser(userId)
                }

                // 3. Delete Auth Account
                withContext(Dispatchers.IO) {
                    FirebaseAuthManager.deleteAccount(password) 
                }

                // 4. Logout locally
                SessionManager.logout()

                if (isAdded && context != null) {
                   Toast.makeText(context, "Conta excluída com sucesso.", Toast.LENGTH_LONG).show()
                }

                // 5. Navigate to Login
                if (isAdded) {
                    dismiss() // Dismiss dialog
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, LoginFragment())
                        .run { 
                            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            commit()
                        }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                dbButton.isEnabled = true // Re-enable button on error
                
                if (isAdded && context != null) {
                    val msg = if (e.message?.contains("password", true) == true || e.message?.contains("credential", true) == true) {
                        "Senha incorreta ou erro de autenticação."
                    } else {
                        "Erro ao excluir: ${e.message}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
