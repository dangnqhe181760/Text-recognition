package com.example.textrecognition

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.textrecognition.ui.theme.TextRecognitionTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


const val WEB_CLIENT_ID = "645927644676-9furiq6n4mplruuk3dtlg9qcf5u2l59l.apps.googleusercontent.com"


class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = Firebase.auth
        setContent {
            val navController: NavHostController = rememberNavController()
            TextRecognitionTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager: CredentialManager = CredentialManager.create(context)
                val startDestination = if (auth.currentUser == null) {
                    NavigationRoutes.Login.route
                } else {
                    NavigationRoutes.Home.route
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(route = NavigationRoutes.Login.route) {
                        LoginScreen(onSignInClick = {
                            val googleOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(WEB_CLIENT_ID)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleOption)
                                .build()
                            scope.launch {
                                Log.d("MainActivity", "start getting credential")
                                try {
                                    Log.d("MainActivity", "Requesting credential")
                                    val result = credentialManager.getCredential(context = context, request = request)
                                    Log.d("MainActivity", "Credential result: $result")
                                    val credential = result.credential
                                    val googleIdTokenCredential = GoogleIdTokenCredential
                                        .createFrom(credential.data)
                                    val googleIdToken = googleIdTokenCredential.idToken

                                    val firebaseCredential =
                                        GoogleAuthProvider.getCredential(googleIdToken, null)

                                    auth.signInWithCredential(firebaseCredential)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                navController.popBackStack()
                                                navController.navigate(NavigationRoutes.Home.route)
                                            }
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT)
                                        .show()
                                    e.printStackTrace()
                                }
                            }
                        }
                        )
                    }
                    composable(route = NavigationRoutes.Home.route) {
                        HomeScreen(
                            currentUser = auth.currentUser,
                            onSignOutClick = {
                                auth.signOut()
                                scope.launch {
                                    credentialManager.clearCredentialState(
                                        ClearCredentialStateRequest()
                                    )
                                }
                                navController.popBackStack()
                                navController.navigate(NavigationRoutes.Login.route)
                            }
                        )
                    }
                }
            }
        }
    }
}
