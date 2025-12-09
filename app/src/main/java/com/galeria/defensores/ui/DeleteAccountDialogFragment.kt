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

        return AlertDialog.Builder(context)
            .setTitle("Excluir Conta")
            .setMessage("Tem certeza? Esta ação é irreversível e apagará todos os seus dados.")
            .setView(input)
            .setPositiveButton("Excluir") { _, _ ->
                val password = input.text.toString()
                if (password.isNotBlank()) {
                    performDelete(password)
                } else {
                    Toast.makeText(context, "Senha necessária.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }

    private fun performDelete(password: String) {
        val userId = SessionManager.currentUser?.id ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Re-auth and Delete Auth Account
                withContext(Dispatchers.IO) {
                    FirebaseAuthManager.deleteAccount(password)
                }
                
                // 2. Delete Firestore Data (if Auth deleted successfully)
                withContext(Dispatchers.IO) {
                    UserRepository.deleteUser(userId)
                }

                // 3. Logout locally
                SessionManager.logout()

                Toast.makeText(context, "Conta excluída com sucesso.", Toast.LENGTH_LONG).show()

                // 4. Navigate to Login
                parentFragmentManager.beginTransaction()
                    .replace(com.galeria.defensores.R.id.fragment_container, LoginFragment())
                    .run { 
                        parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        commit()
                    }

            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
