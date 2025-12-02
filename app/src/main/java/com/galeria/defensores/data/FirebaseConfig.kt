package com.galeria.defensores.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirebaseConfig {
    val firestore: FirebaseFirestore
        get() = Firebase.firestore
}
