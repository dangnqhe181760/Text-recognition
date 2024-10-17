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
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.textrecognition.ui.theme.TextRecognitionTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat


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
                        RealTimeTextRecognitionScreen()
//                        LoginScreen(onSignInClick = {
//                            val googleOption = GetGoogleIdOption.Builder()
//                                .setFilterByAuthorizedAccounts(false)
//                                .setServerClientId(WEB_CLIENT_ID)
//                                .build()
//                            val request = GetCredentialRequest.Builder()
//                                .addCredentialOption(googleOption)
//                                .build()
//                            scope.launch {
//                                Log.d("MainActivity", "start getting credential")
//                                try {
//                                    Log.d("MainActivity", "Requesting credential")
//                                    val result = credentialManager.getCredential(context = context, request = request)
//                                    Log.d("MainActivity", "Credential result: $result")
//                                    val credential = result.credential
//                                    val googleIdTokenCredential = GoogleIdTokenCredential
//                                        .createFrom(credential.data)
//                                    val googleIdToken = googleIdTokenCredential.idToken
//
//                                    val firebaseCredential =
//                                        GoogleAuthProvider.getCredential(googleIdToken, null)
//
//                                    auth.signInWithCredential(firebaseCredential)
//                                        .addOnCompleteListener { task ->
//                                            if (task.isSuccessful) {
//                                                navController.popBackStack()
//                                                navController.navigate(NavigationRoutes.Home.route)
//                                            }
//                                        }
//                                } catch (e: Exception) {
//                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT)
//                                        .show()
//                                    e.printStackTrace()
//                                }
//                            }
//                        }
//                        )
                    }
                    composable(route = NavigationRoutes.Home.route) {
                        Column {
                            GalleryImagePicker()
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
}
@Composable
fun GalleryImagePicker() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedText by remember { mutableStateOf("") }
    val textColor = if (isSystemInDarkTheme()) Color.White else Color.Black

    // Launcher to pick an image from the gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
            uri?.let {
                // Process image for text recognition
                val inputImage = uriToInputImage(context, it)
                inputImage?.let { image ->
                    recognizeTextFromImage(image) { resultText ->
                        detectedText = resultText
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Pick Image from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show the selected image
        imageUri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show the detected text
        Text(
            text = "Detected Text: $detectedText",
            color = textColor,
            modifier = Modifier.padding(16.dp)
        )
    }
}
// Convert the Uri to InputImage for text recognition
fun uriToInputImage(context: Context, imageUri: Uri): InputImage? {
    return try {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        InputImage.fromBitmap(bitmap, 0)
    } catch (e: Exception) {
        Log.e("TextRecognition", "Failed to load image: ${e.message}")
        null
    }
}

// Recognize text from the image using ML Kit
fun recognizeTextFromImage(inputImage: InputImage, onResult: (String) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val detectedText = visionText.text
            onResult(detectedText)
        }
        .addOnFailureListener { e ->
            Log.e("TextRecognition", "Text recognition failed: ${e.message}")
        }
}

@Composable
fun RealTimeTextRecognitionScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var recognizedText by remember { mutableStateOf("") }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // UI Layout: Column containing the camera preview at the top and recognized text below
    Column(modifier = Modifier.fillMaxSize()) {
        // Camera Preview at the top
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraExecutor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Set up the preview use case to display the camera preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Set up image analysis use case for real-time text recognition
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analyzer ->
                            analyzer.setAnalyzer(cameraExecutor, { imageProxy ->
                                processImageProxy(imageProxy) { detectedText ->
                                    recognizedText = detectedText
                                }
                            })
                        }

                    // Select back camera as the default
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Bind the camera to the lifecycle
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, cameraExecutor)

                previewView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // Adjust the height of the camera preview as needed
        )

        Spacer(modifier = Modifier.height(100.dp))

        // Scrollable recognized text below the camera preview
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Make it scrollable
        ) {
            Text(text = "Recognized Text:", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = recognizedText) // Display the real-time recognized text here
        }
    }
}

@OptIn(ExperimentalGetImage::class)
fun processImageProxy(
    imageProxy: ImageProxy,
    onTextDetected: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        // Convert ImageProxy to InputImage
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Initialize TextRecognizer
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Process the image
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Extract text from the visionText result
                val detectedText = visionText.text
                onTextDetected(detectedText)
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognition", "Text recognition failed: ${e.message}")
            }
            .addOnCompleteListener {
                // Close the imageProxy after processing
                imageProxy.close()
            }
    }
}
