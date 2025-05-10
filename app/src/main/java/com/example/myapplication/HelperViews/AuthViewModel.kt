package com.example.myapplication.HelperViews

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class AuthViewModel : ViewModel() {

    //using Firebase Library code to create user account and/or login
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    //Whenever we load the view model, we want the activity
    init{
        checkAuthStatus()
    }

    private fun checkAuthStatus(){
       if (auth.currentUser == null){
           _authState.value = AuthState.Unauthenticated
       }else{
           _authState.value = AuthState.Authenticated
       }
    }

    fun login(email:String, password : String){

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or Password Can't be Empty")
            return
        }

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or Password Can't be Empty")
            return
        }

        if (password.length < 6 ) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }
        if (!(password.length <=12)){
            _authState.value = AuthState.Error("Password must be less than 12 characters long")
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{
                task -> if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                }else{
                    _authState.value = AuthState.Error(
                        task.exception?.message ?: "Could Not Login"
                    )
                }
            }
    }

    fun signup(email:String, password : String){

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email or Password Can't be Empty")
            return
        }

        if (password.length < 6 ) {
            _authState.value = AuthState.Error("Password must be at least 8 characters")
            return
        }
        if (!(password.length <=12)){
            _authState.value = AuthState.Error("Password must be less than 12 characters long")
        }


        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener{
                    task -> if (task.isSuccessful){
                _authState.value = AuthState.Authenticated
            }else{
                _authState.value = AuthState.Error(
                    task.exception?.message ?: "Could Not Signup"
                )
            }
            }
    }

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}










sealed class AuthState{
    //to know which state we are, if we are
    object Authenticated : AuthState() //we can go to HomePage
    object Unauthenticated: AuthState() //we can go to LoginPage
    object Loading : AuthState() // stay in current page
    data class Error(val message : String) : AuthState() // if we get an authentication error,
                        // we can just show the message
}