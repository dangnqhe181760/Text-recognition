package com.example.textrecognition

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.textrecognition.ui.theme.TextRecognitionTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.FileOutputStream


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
//                        RealTimeTextRecognitionScreen()
                        GalleryImagePicker(onClick = {
                            navController.navigate(NavigationRoutes.Home.route)
                        })
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
                            FaceDetection()
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
fun GalleryImagePicker(
    onClick: () -> Unit
) {
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
        Button(onClick = {
            onClick()
            Log.d("LoginScreen", "Button Clicked")
        }) {
            Text(
                text = "Face Detection",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
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
            color = Color.Black,
            modifier = Modifier.padding(16.dp)
        )
        copyToClipboard(context, detectedText)
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

fun copyToClipboard(context: Context, text: String) {
    val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("password", text)
    clipboardManager.setPrimaryClip(clip)
}

@Composable
fun FaceDetection() {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Step 1: Create an activity launcher to pick image from gallery
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // This is the URI of the image picked from the gallery
        selectedImageUri = uri
    }

    // UI for picking and displaying image
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Button(onClick = {
            // Step 2: Launch the gallery
            pickImageLauncher.launch("image/*")
        }) {
            Text("Pick Image from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Display the selected image and detect faces
        selectedImageUri?.let { uri ->
            val bitmap = getBitmapFromUri(context, uri)
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Trigger face detection and crop the detected face
                DetectAndDisplayFace(bitmap = bmp)
            }
        }
    }
}

// Helper function to convert URI to Bitmap
fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun DetectAndDisplayFace(bitmap: Bitmap) {
    var detectedFaceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    // Perform face detection
    LaunchedEffect(bitmap) {
        detectFace(bitmap) { faceBitmap ->
            detectedFaceBitmap = faceBitmap
            // Optionally save to Pictures folder
            saveBitmapToPicturesFolder(context, faceBitmap)
        }
    }

    // Display the detected face if available
    detectedFaceBitmap?.let { faceBmp ->
        Text(text = "Detected Face:")
        Image(bitmap = faceBmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(150.dp))
    }
}

// Function to detect face and return the cropped face bitmap
fun detectFace(image: Bitmap, onFaceCropped: (Bitmap) -> Unit) {
    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()

    val inputImage = InputImage.fromBitmap(image, 0)
    val detector = FaceDetection.getClient(options)

    detector.process(inputImage)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces.first()
                val faceRect = face.boundingBox

                // Crop the face from the original image
                val croppedFace = Bitmap.createBitmap(
                    image,
                    faceRect.left.coerceAtLeast(0),
                    faceRect.top.coerceAtLeast(0),
                    faceRect.width().coerceAtMost(image.width - faceRect.left),
                    faceRect.height().coerceAtMost(image.height - faceRect.top)
                )

                onFaceCropped(croppedFace)
            }
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
        }
}

// Function to save the cropped face to the Pictures folder
fun saveBitmapToPicturesFolder(context: Context, bitmap: Bitmap) {
    val picturesDirectory =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val file = File(picturesDirectory, "detected_face.jpg")

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
}