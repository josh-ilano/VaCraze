package com.example.myapplication.Tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FirebaseInput() {
    var noteText by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Enter your note") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                user?.let {
                    val note = hashMapOf(
                        "text" to noteText,
                        "location" to
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    db.collection("users")
                        .document(it.uid)
                        .collection("notes")
                        .add(note)
                        .addOnSuccessListener {
                            message = "Note saved!"
                            noteText = ""
                        }
                        .addOnFailureListener { e ->
                            message = "Error: ${e.message}"
                        }
                } ?: run {
                    message = "User not signed in"
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (message.isNotEmpty()) {
            Text(text = message)
        }
    }
}
