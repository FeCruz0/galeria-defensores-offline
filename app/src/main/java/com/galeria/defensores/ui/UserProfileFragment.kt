package com.galeria.defensores.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.galeria.defensores.R
import com.galeria.defensores.data.SessionManager
import com.galeria.defensores.data.UserRepository
import com.galeria.defensores.models.User
import kotlinx.coroutines.launch

class UserProfileFragment : Fragment() {

    private var userId: String? = null

    // UI Elements
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvAbout: TextView
    private lateinit var tvCep: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvState: TextView
    private lateinit var tvCountry: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnLogout: Button
    private lateinit var btnBack: Button

    companion object {
        private const val ARG_USER_ID = "user_id"

        fun newInstance(userId: String? = null): UserProfileFragment {
            val fragment = UserProfileFragment()
            if (userId != null) {
                val args = Bundle()
                args.putString(ARG_USER_ID, userId)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)
        
        tvName = view.findViewById(R.id.tv_user_name)
        tvEmail = view.findViewById(R.id.tv_user_email)
        tvAbout = view.findViewById(R.id.tv_about)
        tvCep = view.findViewById(R.id.tv_cep)
        tvCity = view.findViewById(R.id.tv_city)
        tvState = view.findViewById(R.id.tv_state)
        tvCountry = view.findViewById(R.id.tv_country)
        btnEdit = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnBack = view.findViewById(R.id.btn_back)

        // Bind Data
        updateUI()

        // Hide Edit/Logout if viewing another user
        val btnDeleteAccount = view.findViewById<Button>(R.id.btn_delete_account)

        // Hide sensitive actions for offline mode
        btnEdit.visibility = View.VISIBLE 
        btnLogout.visibility = View.GONE
        btnDeleteAccount.visibility = View.GONE
        
        btnEdit.setOnClickListener {
             parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserEditFragment())
                .addToBackStack(null)
                .commit()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val currentLoggedInUser = SessionManager.currentUser
        
        if (userId != null && userId != currentLoggedInUser?.id) {
            // Viewing another user
            viewLifecycleOwner.lifecycleScope.launch {
                val user = UserRepository.getUser(userId!!)
                if (user != null) {
                    bindUserData(user)
                } else {
                     Toast.makeText(context, "Usuário não encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Viewing self
            if (currentLoggedInUser != null) {
                bindUserData(currentLoggedInUser)
            }
        }
    }

    private fun bindUserData(user: User) {
        tvName.text = user.name
        tvEmail.text = user.email
        tvAbout.text = if (user.about.isNotEmpty()) user.about else "Sem descrição."
        tvCep.text = if (user.cep.isNotEmpty()) user.cep else "-"
        tvCity.text = if (user.city.isNotEmpty()) user.city else "-"
        tvState.text = if (user.state.isNotEmpty()) user.state else "-"
        tvCountry.text = if (user.country.isNotEmpty()) user.country else "-"
    }
}
