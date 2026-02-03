package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.galeria.defensores.R
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.models.User
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserEditFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var layoutUsername: com.google.android.material.textfield.TextInputLayout
    private lateinit var etAbout: EditText
    private lateinit var etCep: EditText
    private lateinit var etCity: EditText
    private lateinit var etState: EditText
    private lateinit var etCountry: EditText
    private lateinit var etPasswordConfirm: EditText
    private lateinit var btnSearchCep: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_edit, container, false)
        
        // Initialize views
        etUsername = view.findViewById(R.id.input_username)
        // Find the parent TextInputLayout for error display. 
        // Note: In XML, input_username is inside a TextInputLayout. 
        // We can find it by traversing parent or giving it an ID in XML.
        // For now, let's assume valid ID or find parent.
        // Ideally we should have given an ID to the TextInputLayout in XML.
        // Let's rely on finding by ID if I added it, or user might need to add it.
        // Checking XML previously: I did NOT give an ID to the TextInputLayout in the last edit.
        // usage: output of Step 343 shows <com.google.android.material.textfield.TextInputLayout ...> without ID.
        // I will need to use code to find it or just update XML. 
        // Updating XML is safer but takes a step. 
        // Finding parent is easy:
        layoutUsername = etUsername.parent.parent as com.google.android.material.textfield.TextInputLayout

        etAbout = view.findViewById(R.id.input_about)
        etCep = view.findViewById(R.id.input_cep)
        etCity = view.findViewById(R.id.input_city)
        etState = view.findViewById(R.id.input_state)
        etCountry = view.findViewById(R.id.input_country)
        // etPasswordConfirm = view.findViewById(R.id.input_password_confirm) // Not needed
        btnSearchCep = view.findViewById(R.id.btn_search_cep)
        btnSave = view.findViewById(R.id.btn_save_changes)
        btnCancel = view.findViewById(R.id.btn_back)

        // Populate Fields
        SessionManager.currentUser?.let { user ->
            etUsername.setText(user.name)
            etAbout.setText(user.about)
            etCep.setText(user.cep)
            etCity.setText(user.city)
            etState.setText(user.state)
            etCountry.setText(user.country.ifEmpty { "Brasil" })
        }
        
        // Real-time Username Validation to match RegisterFragment
        var usernameCheckJob: kotlinx.coroutines.Job? = null
        etUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                usernameCheckJob?.cancel()
                val name = s.toString().trim()
                val validationError = validateUsernameFormat(name)
                
                if (validationError != null) {
                    layoutUsername.error = validationError
                } else if (name != SessionManager.currentUser?.name) {
                     // Only check uniqueness if it's different from current
                    usernameCheckJob = viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.delay(500) // Debounce
                        val isTaken = UserRepository.isUsernameTaken(name)
                        if (isTaken) {
                            layoutUsername.error = "Nome de usuário já existe"
                        } else {
                            layoutUsername.error = null
                        }
                    }
                } else {
                    layoutUsername.error = null
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Listeners
        btnSearchCep.setOnClickListener {
            val cep = etCep.text.toString()
            if (cep.length >= 8) {
                fetchAddressByCep(cep)
            }
        }

        btnSave.setOnClickListener {
            handleSave()
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun fetchAddressByCep(cep: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://viacep.com.br/ws/$cep/json/")
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    val data = urlConnection.inputStream.bufferedReader().readText()
                    val json = JSONObject(data)
                    
                    if (!json.has("erro")) {
                        withContext(Dispatchers.Main) {
                            etCity.setText(json.optString("localidade"))
                            etState.setText(json.optString("uf"))
                        }
                    } else {
                         withContext(Dispatchers.Main) {
                            Toast.makeText(context, "CEP não encontrado", Toast.LENGTH_SHORT).show()
                        }
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validateUsernameFormat(name: String): String? {
        if (name.isEmpty()) return "Digite um nome de usuário"
        if (name.contains(" ")) return "Não pode conter espaços"
        if (name.length < 5) return "Mínimo de 5 caracteres"
        if (name.length > 15) return "Máximo de 15 caracteres"
        if (!name.matches(Regex("^[a-zA-Z0-9]+$"))) return "Apenas letras e números"
        if (name.contains(Regex("(.)\\1\\1"))) return "Muitos caracteres repetidos"
        return null
    }

    private fun handleSave() {
        // Validation simplified for offline
        val currentUser = SessionManager.currentUser ?: return
        val newUsername = etUsername.text.toString().trim()
        
        // Reading UI fields
        val newAbout = etAbout.text.toString().trim()
        val newCep = etCep.text.toString().trim()
        val newCity = etCity.text.toString().trim()
        val newState = etState.text.toString().trim()
        val newCountry = etCountry.text.toString().trim()

        // Username Format Validation
        val formatError = validateUsernameFormat(newUsername)
        if (formatError != null) {
            layoutUsername.error = formatError
            Toast.makeText(context, formatError, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Directly save
        val usernameChanged = newUsername != currentUser.name
        lifecycleScope.launch {
            saveProfile(usernameChanged, newUsername, newAbout, newCep, newCity, newState, newCountry)
        }
    }

    private suspend fun saveProfile(
        usernameChanged: Boolean,
        newUsername: String,
        newAbout: String,
        newCep: String,
        newCity: String,
        newState: String,
        newCountry: String
    ) {
        try {
            val currentUser = SessionManager.currentUser ?: return
            
            val updatedUser = currentUser.copy(
                name = newUsername,
                about = newAbout,
                cep = newCep,
                city = newCity,
                state = newState,
                country = newCountry,
                lastUsernameChangeAt = if (usernameChanged) System.currentTimeMillis() else currentUser.lastUsernameChangeAt
            )

            UserRepository.updateUser(updatedUser)
            SessionManager.refreshUser() // Update local session
            
            // Ensure context is available before Toast
            context?.let {
                Toast.makeText(it, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            }
            parentFragmentManager.popBackStack()
        } catch (e: Exception) {
            e.printStackTrace()
             // Catch explicit exceptions
            context?.let {
                 Toast.makeText(it, "Erro ao salvar perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (t: Throwable) {
             // Catch ANY runtime error/crash potential including low level issues
             t.printStackTrace()
        }
    }
}
