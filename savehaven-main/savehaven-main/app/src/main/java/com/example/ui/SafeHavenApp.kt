package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.data.Device
import com.example.data.SecurityAlert
import com.example.ui.theme.*
import com.example.viewmodel.SafeHavenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SafeHavenApp(
    viewModel: SafeHavenViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Synchronize authentication flow
    LaunchedEffect(isUserLoggedIn, devices, currentRoute) {
        if (isUserLoggedIn) {
            val isAtAuth = currentRoute == "login" || currentRoute == "signup" || currentRoute == null
            val isAtAddDeviceAutomatically = currentRoute == "add_device" && navController.previousBackStackEntry == null

            if (isAtAuth || isAtAddDeviceAutomatically) {
                if (devices.isEmpty()) {
                    if (currentRoute != "add_device") {
                        navController.navigate("add_device") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                } else {
                    if (currentRoute != "dashboard") {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            } else {
                val hasCameras = devices.any { it.iconName == "videocam" }
                if (!hasCameras && (currentRoute == "live" || currentRoute == "zoom")) {
                    navController.navigate("dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        } else {
            if (currentRoute != "login" && currentRoute != "signup" && currentRoute != null) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (isUserLoggedIn && currentRoute != "add_device") {
                SafeHavenHeader(
                    userName = viewModel.userName.collectAsStateWithLifecycle().value,
                    onAvatarClick = { navController.navigate("profile") }
                )
            } else if (currentRoute == "add_device") {
                SafeHavenAddDeviceHeader(onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                })
            }
        },
        bottomBar = {
            if (isUserLoggedIn && currentRoute != "add_device") {
                SafeHavenBottomBar(
                    currentRoute = currentRoute,
                    hasCameras = devices.any { it.iconName == "videocam" },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentRoute == "devices") {
                FloatingActionButton(
                    onClick = { navController.navigate("add_device") },
                    containerColor = SafeHavenDarkBlue,
                    contentColor = SafeHavenMint,
                    modifier = Modifier
                        .testTag("add_device_fab")
                        .padding(bottom = 60.dp), // Adjust above custom tall bottom navigation
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Device", modifier = Modifier.size(28.dp))
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isUserLoggedIn) "dashboard" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onSignUpNavigate = { navController.navigate("signup") },
                    onForgotPasswordNavigate = { navController.navigate("forgot_password") }
                )
            }
            composable("forgot_password") {
                ForgotPasswordScreen(
                    viewModel = viewModel,
                    onBackToLogin = { navController.popBackStack() }
                )
            }
            composable("signup") {
                SignUpScreen(
                    viewModel = viewModel,
                    onLoginNavigate = { navController.navigate("login") }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onViewAllAlerts = { navController.navigate("alerts") },
                    onNavigateToLive = { navController.navigate("live") },
                    onNavigateToDevices = { navController.navigate("devices") },
                    onNavigateToAlerts = { navController.navigate("alerts") }
                )
            }
            composable("live") {
                LiveScreen(
                    viewModel = viewModel,
                    onNavigateToZoom = { navController.navigate("zoom") }
                )
            }
            composable("zoom") {
                ZoomScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("devices") {
                DevicesScreen(viewModel = viewModel)
            }
            composable("alerts") {
                AlertsScreen(viewModel = viewModel)
            }
            composable("profile") {
                ProfileScreen(viewModel = viewModel)
            }
            composable("add_device") {
                BackHandler(enabled = navController.previousBackStackEntry == null) {
                    navController.navigate("dashboard") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                AddDeviceScreen(
                    viewModel = viewModel,
                    onDeviceAdded = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate("dashboard") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}

// Global Custom Header
@Composable
fun SafeHavenHeader(
    userName: String,
    onAvatarClick: () -> Unit
) {
    Surface(
        color = SafeHavenBackground,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "SafeHaven",
                    tint = SafeHavenGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "SafeHaven",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeHavenDarkBlue,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Interactive Profile Avatar Header Endpoint
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, SafeHavenGreen, CircleShape)
                    .clickable { onAvatarClick() }
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCLQi2RVyG4AejShGYBZ1WYzp3iAVCrnjzx9zvbALu2FrPC1aWiv5avzBiAyRo8E3DXXE5zb5hJSG2JxdnoPniY7YWNYhbwX-c6ImIxPCamBWiqySBvhzAA7B5kWXo8myDSdrCR9NkwqHRif1UIDpGSxo9-nDLnQbmCz4dpEWyL0C4Qi_2PsgHKTBNx1er6hie5PF4CPT9AQzPxx0nbJ4PRXHK0LJeuZWCaidLeueq5S9n8PSxxmkTKvxtQ5VJIdPmEpt84vemWNDp3", // Professional avatar matching Emma Johnson
                    contentDescription = "User Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Add Device Header custom close arrow
@Composable
fun SafeHavenAddDeviceHeader(onBack: () -> Unit) {
    Surface(
        color = SafeHavenBackground,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = SafeHavenDarkBlue
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Add Device",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SafeHavenDarkBlue,
                modifier = Modifier.weight(1.0f)
            )
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Help",
                    tint = SafeHavenDarkBlue
                )
            }
        }
    }
}

// Beautiful Material 3 Bottom Nav Bar with high spatial layouts
@Composable
fun SafeHavenBottomBar(
    currentRoute: String?,
    hasCameras: Boolean,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = SafeHavenBackground,
        tonalElevation = 8.dp,
        modifier = Modifier
            .height(84.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .border(
                width = 1.dp,
                color = SafeHavenSurfaceContainer,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
    ) {
        val navItems = listOfNotNull(
            NavigationItem("dashboard", "Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
            if (hasCameras) NavigationItem("live", "Live", Icons.Default.Videocam, Icons.Outlined.Videocam) else null,
            NavigationItem("devices", "Devices", Icons.Default.Router, Icons.Outlined.Router),
            NavigationItem("alerts", "Alerts", Icons.Default.Notifications, Icons.Outlined.Notifications, hasBadge = true),
            NavigationItem("profile", "Profile", Icons.Default.AccountCircle, Icons.Outlined.AccountCircle)
        )

        navItems.forEach { item ->
            val isSelected = (currentRoute == item.route)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Box {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (isSelected) SafeHavenGreen else SafeHavenDarkBlue.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        if (item.hasBadge && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .background(SafeHavenError, CircleShape)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SafeHavenGreen else SafeHavenDarkBlue.copy(alpha = 0.7f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SafeHavenSecondaryContainer
                )
            )
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val hasBadge: Boolean = false
)


// ============================================
// 1. LOGIN SCREEN (SafeHaven Static Login)
// ============================================
@Composable
fun LoginScreen(
    viewModel: SafeHavenViewModel,
    onSignUpNavigate: () -> Unit,
    onForgotPasswordNavigate: () -> Unit
) {
    var email by remember { mutableStateFlowOf("emma@safehaven.com") }
    var password by remember { mutableStateFlowOf("safehaven123") }
    var passwordVisible by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isAuthenticating by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SafeHavenMint.copy(alpha = 0.15f), SafeHavenBackground),
                    center = Offset(200f, 100f),
                    radius = 800f
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand Logo Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SafeHavenDarkBlue, RoundedCornerShape(16.dp))
                        .drawBehind {
                            // Subdued lock icon on canvas
                        }
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = SafeHavenMint,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SafeHaven",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafeHavenDarkBlue,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your credentials to access your secure home.",
                    fontSize = 14.sp,
                    color = SafeHavenDarkBlue.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(280.dp)
                )
            }

            // Glass-Card containing Form
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "EMAIL OR USERNAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeHavenDarkBlue.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                viewModel.clearLoginError()
                            },
                            placeholder = { Text("emma@safehaven.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SafeHavenGreen)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeHavenGreen,
                                unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input")
                        )
                    }

                    // Password
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASSWORD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeHavenDarkBlue.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Forgot Password?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeHavenGreen,
                                modifier = Modifier.clickable { onForgotPasswordNavigate() }
                            )
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                viewModel.clearLoginError()
                            },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SafeHavenGreen)
                            },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, contentDescription = "Toggle password")
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeHavenGreen,
                                unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )
                    }

                    if (loginError != null) {
                        Text(
                            text = loginError ?: "",
                            color = SafeHavenError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Action Button with micro-animations
                    Button(
                        onClick = {
                            if (!isAuthenticating) {
                                isAuthenticating = true
                                scope.launch {
                                    delay(1000)
                                    val success = viewModel.login(email, password)
                                    if (success) {
                                        showSuccessAnimation = true
                                        delay(500)
                                    }
                                    isAuthenticating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showSuccessAnimation) SafeHavenOnInfo else SafeHavenGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("login_button")
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else if (showSuccessAnimation) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Securely Authenticated", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Biometrics Control Box
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        scope.launch { viewModel.login("emma@safehaven.com", "safehaven123") }
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, SafeHavenOutline.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = "Face ID", tint = SafeHavenDarkBlue.copy(alpha = 0.7f))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Face ID", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        scope.launch { viewModel.login("emma@safehaven.com", "safehaven123") }
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.dp, SafeHavenOutline.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Touch ID", tint = SafeHavenDarkBlue.copy(alpha = 0.7f))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Touch ID", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // Social login divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Divider(modifier = Modifier.weight(1.0f), color = SafeHavenOutline.copy(alpha = 0.3f))
                        Text(
                            text = "OR CONTINUE WITH",
                            fontSize = 10.sp,
                            color = SafeHavenDarkBlue.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Divider(modifier = Modifier.weight(1.0f), color = SafeHavenOutline.copy(alpha = 0.3f))
                    }

                    // Social Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { viewModel.login("emma@safehaven.com", "safehaven123") } },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Google", color = SafeHavenDarkBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { scope.launch { viewModel.login("emma@safehaven.com", "safehaven123") } },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Apple", color = SafeHavenDarkBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Footer Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = SafeHavenDarkBlue.copy(alpha = 0.7f), fontSize = 14.sp)
                Text(
                    text = "Sign Up",
                    color = SafeHavenGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onSignUpNavigate() }
                )
            }
        }
    }
}


// ============================================
// 1b. FORGOT PASSWORD SCREEN (Password Reset Request)
// ============================================
@Composable
fun ForgotPasswordScreen(
    viewModel: SafeHavenViewModel,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateFlowOf("") }
    val isForgotPasswordLoading by viewModel.isForgotPasswordLoading.collectAsStateWithLifecycle()
    val forgotPasswordSuccess by viewModel.forgotPasswordSuccess.collectAsStateWithLifecycle()
    val forgotPasswordError by viewModel.forgotPasswordError.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Clear forgot password state when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearForgotPasswordState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SafeHavenMint.copy(alpha = 0.15f), SafeHavenBackground),
                    center = Offset(200f, 100f),
                    radius = 800f
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.testTag("back_to_login_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Login",
                        tint = SafeHavenDarkBlue
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Password Recovery",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeHavenDarkBlue
                )
            }

            // Brand Logo Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SafeHavenDarkBlue, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "Lock Reset",
                        tint = SafeHavenMint,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Forgot Password?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafeHavenDarkBlue,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your registered email or username below, and we will simulate sending a password recovery link.",
                    fontSize = 14.sp,
                    color = SafeHavenDarkBlue.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(300.dp)
                )
            }

            // Glass-Card Form
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (forgotPasswordSuccess) {
                        // Success Feedback
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SafeHavenOnInfo.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, SafeHavenOnInfo),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = SafeHavenOnInfo,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Reset Link Sent",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeHavenOnInfo
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "A password recovery link has been simulated and sent to your email. Check your server logs!",
                                        fontSize = 12.sp,
                                        color = SafeHavenDarkBlue.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Input Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "EMAIL OR USERNAME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeHavenDarkBlue.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("emma@safehaven.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = SafeHavenGreen)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SafeHavenGreen,
                                    unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("forgot_password_email_input")
                            )
                        }

                        if (forgotPasswordError != null) {
                            Text(
                                text = forgotPasswordError ?: "",
                                color = SafeHavenError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("forgot_password_error_text")
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (email.isNotBlank() && !isForgotPasswordLoading) {
                                    scope.launch {
                                        viewModel.sendPasswordResetLink(email)
                                    }
                                }
                            },
                            enabled = email.isNotBlank() && !isForgotPasswordLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("forgot_password_submit_button")
                        ) {
                            if (isForgotPasswordLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Back link
            Text(
                text = "Back to Sign In",
                color = SafeHavenGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { onBackToLogin() }
                    .testTag("back_to_login_link")
            )
        }
    }
}


// Helper to provide standard mutable states
fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)


// ============================================
// 2. SIGN UP SCREEN (Secure Sign Up)
// ============================================
@Composable
fun SignUpScreen(
    viewModel: SafeHavenViewModel,
    onLoginNavigate: () -> Unit
) {
    var name by remember { mutableStateFlowOf("") }
    var email by remember { mutableStateFlowOf("") }
    var phone by remember { mutableStateFlowOf("") }
    var password by remember { mutableStateFlowOf("") }
    var agreesToTerms by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(SafeHavenMint.copy(alpha = 0.15f), SafeHavenBackground),
                    center = Offset(200f, 100f),
                    radius = 800f
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = SafeHavenGreen, modifier = Modifier.size(28.dp))
                    Text("SafeHaven", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Create Your Secure Profile",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafeHavenDarkBlue,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Begin your journey to professional-grade home protection and peace of mind.",
                    fontSize = 14.sp,
                    color = SafeHavenDarkBlue.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(300.dp)
                )
            }

            // Cards Form Onboarding
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Full Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Full Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("John Doe") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SafeHavenGreen) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeHavenGreen,
                                unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Email Address
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Email Address", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("john@example.com") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = SafeHavenGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeHavenGreen,
                                unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Phone Number
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Phone Number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = { Text("+1 (555) 000-0000") },
                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = SafeHavenGreen) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeHavenGreen,
                                unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Security Password
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Security Password", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SafeHavenGreen) },
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, contentDescription = null)
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SafeHavenGreen,
                                unfocusedBorderColor = SafeHavenOutline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Terms check
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreesToTerms,
                            onCheckedChange = { agreesToTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = SafeHavenGreen)
                        )
                        Text(
                            text = "I agree to the Terms of Service and Privacy Policy.",
                            fontSize = 12.sp,
                            color = SafeHavenDarkBlue.copy(alpha = 0.7f),
                            modifier = Modifier.clickable { agreesToTerms = !agreesToTerms }
                        )
                    }

                    // Submit Onboarding
                    Button(
                        onClick = {
                            if (agreesToTerms && name.isNotEmpty() && email.isNotEmpty() && password.length >= 6) {
                                isRegistering = true
                                scope.launch {
                                    delay(1000)
                                    viewModel.register(name, email, phone, password)
                                    isRegistering = false
                                }
                            }
                        },
                        enabled = agreesToTerms && name.isNotEmpty() && email.isNotEmpty() && password.length >= 6,
                        colors = ButtonDefaults.buttonColors(containerColor = SafeHavenGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("signup_button")
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Already have an account? ", color = SafeHavenDarkBlue.copy(alpha = 0.7f), fontSize = 14.sp)
                        Text(
                            text = "Login",
                            color = SafeHavenGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onLoginNavigate() }
                        )
                    }
                }
            }

            // Onboarding Security badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(SafeHavenSurfaceLow, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SafeHavenGreen, modifier = Modifier.size(20.dp))
                    Text("AES-256 Bit Encryption", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(SafeHavenSurfaceLow, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SafeHavenGreen, modifier = Modifier.size(20.dp))
                    Text("Privacy Guaranteed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                }
            }
        }
    }
}



// ============================================
// 3. DASHBOARD SCREEN (Screen 4708846472280730674)
// ============================================
@Composable
fun DashboardScreen(
    viewModel: SafeHavenViewModel,
    onViewAllAlerts: () -> Unit,
    onNavigateToLive: () -> Unit,
    onNavigateToDevices: () -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val rawAlerts by viewModel.alerts.collectAsStateWithLifecycle()
    val systemArmed by viewModel.systemArmed.collectAsStateWithLifecycle()
    val armedMode by viewModel.armedMode.collectAsStateWithLifecycle()
    val name by viewModel.userName.collectAsStateWithLifecycle()

    val alerts = rawAlerts.take(2) // Only show the top 2 alerts on the console for a clean overview

    val camerasCount = remember(devices) {
        devices.count { it.iconName == "videocam" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Title Section
        item {
            Column {
                Text(
                    text = "Welcome Home, $name",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafeHavenDarkBlue
                )
                Text(
                    text = "System secure. No issues detected in your perimeter.",
                    fontSize = 14.sp,
                    color = SafeHavenDarkBlue.copy(alpha = 0.70f)
                )
            }
        }

        // Arming Control Panel Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenGreen),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (systemArmed) Color.White.copy(alpha = 0.25f) else SafeHavenErrorContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (systemArmed) Icons.Default.VerifiedUser else Icons.Default.GppBad,
                                    contentDescription = null,
                                    tint = if (systemArmed) Color.White else SafeHavenError,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (systemArmed) "System Armed" else "System Disarmed",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (systemArmed) "Mode: Armed-$armedMode" else "Perimeter unsecured",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // System state switch
                        Switch(
                            checked = systemArmed,
                            onCheckedChange = { viewModel.setSystemArmed(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SafeHavenGreen,
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tactical mode selection switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Home", "Away", "Night").forEach { mode ->
                            val isSelected = armedMode == mode && systemArmed
                            Button(
                                onClick = {
                                    viewModel.setArmedMode(mode)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) SafeHavenGreen else Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shortcut Console Quick Metrics Grid
        item {
            val hasCameras = remember(devices) { devices.any { it.iconName == "videocam" } }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (hasCameras) {
                    // Live View Box Shortcut
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToLive() }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Live View", tint = SafeHavenGreen, modifier = Modifier.size(28.dp))
                            Text("Live View", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                            Text("$camerasCount Active Cameras", fontSize = 12.sp, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                        }
                    }
                }

                // Devices Box Shortcut
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToDevices() }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Router, contentDescription = "Devices", tint = SafeHavenGreen, modifier = Modifier.size(28.dp))
                        Text("Devices", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        Text("${devices.size} Applets", fontSize = 12.sp, color = SafeHavenDarkBlue.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Recent Alert Console Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SafeHavenError, CircleShape)
                            )
                            Text("Recent Alerts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        }
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeHavenGreen,
                            modifier = Modifier
                                .clickable { onViewAllAlerts() }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (alerts.isEmpty()) {
                        Text(
                            text = "No security events occurred today.",
                            color = SafeHavenOutlineDark,
                            fontSize = 14.sp
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            alerts.forEach { alert ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SafeHavenBackground, RoundedCornerShape(12.dp))
                                        .clickable { onViewAllAlerts() }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    if (alert.level == "Critical") SafeHavenErrorContainer else SafeHavenWarningContainer,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (alert.level == "Critical") Icons.Default.NotificationImportant else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (alert.level == "Critical") SafeHavenError else SafeHavenWarning,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(alert.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                                                Text(alert.timestamp, fontSize = 11.sp, color = SafeHavenOutlineDark)
                                            }
                                            Text(alert.message, fontSize = 12.sp, color = SafeHavenOutlineDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Emergency Tactical Panic Action Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenErrorContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("Dispatch Urgent Assistance", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenError)
                        Text("Fires a silent distress signal to private guards.", fontSize = 12.sp, color = SafeHavenDarkBlue.copy(alpha = 0.70f))
                    }
                    Button(
                        onClick = { viewModel.triggerPanicAlert() },
                        colors = ButtonDefaults.buttonColors(containerColor = SafeHavenError),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("panic_button")
                    ) {
                        Text("PANIC", fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}


// ============================================
// 4. LIVE VIEW SCREEN (Live Cameras)
// ============================================
@Composable
fun LiveScreen(
    viewModel: SafeHavenViewModel,
    onNavigateToZoom: () -> Unit = {}
) {
    val activeIdx by viewModel.activeCameraIndex.collectAsStateWithLifecycle()
    val isTalkActive by viewModel.talkActive.collectAsStateWithLifecycle()
    val isListenActive by viewModel.listenActive.collectAsStateWithLifecycle()
    var isGridView by remember { mutableStateOf(false) } // Default to false to showcase the premium monitor view first!

    val dbDevices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val cameras = remember(dbDevices) {
        val dbCameras = dbDevices.filter { it.iconName == "videocam" }
        dbCameras.map { dbCam ->
            val lowerName = dbCam.name.lowercase()
            val imageUrl = when {
                lowerName.contains("front door") -> "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=400"
                lowerName.contains("driveway") -> "https://images.unsplash.com/photo-1542435503-956c469947f6?w=400"
                lowerName.contains("living room") -> "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=400"
                lowerName.contains("backyard") -> "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?w=400"
                else -> {
                    val fallbackImages = listOf(
                        "https://images.unsplash.com/photo-1558002038-1055907df827?w=400",
                        "https://images.unsplash.com/photo-1508962914676-134849a727f0?w=400",
                        "https://images.unsplash.com/photo-1521207418485-99c705420785?w=400"
                    )
                    fallbackImages[Math.abs(dbCam.id) % fallbackImages.size]
                }
            }
            CameraFeedInfo(dbCam.name, imageUrl)
        }
    }

    val activeCamera = remember(cameras, activeIdx) {
        cameras.getOrNull(activeIdx) ?: cameras.firstOrNull() ?: CameraFeedInfo("Live Feed", "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=400")
    }

    // Local session logs list
    val localLogs = remember { mutableStateListOf<MotionIncidentLog>() }

    val zoomScale = 1.0f
    val isPlaybackActive = false
    val selectedDayIndex = 0
    val selectedTimeIndex = 4
    val currentDisplayTime = "LIVE NOW"

    val displayImageUrl = activeCamera.imageUrl

    val currentHistoryLogs = localLogs.toList()

    // Motion Detection states map for each of the camera devices (index 0 to 3)
    val motionActiveMap = remember {
        androidx.compose.runtime.mutableStateMapOf(
            0 to true,
            1 to true,
            2 to true,
            3 to true
        )
    }
    val isMotionActive = motionActiveMap[activeIdx] ?: true
    var sensitivity by remember { mutableStateOf(0.75f) }
    var detectionMode by remember { mutableStateOf("Smart Tracking") } // "Smart Tracking", "Full Frame", "Perimeter Door"
    var detectPeopleOnly by remember { mutableStateOf(false) }

    // Floating alert overlay state
    var activeToastMsg by remember { mutableStateOf<String?>(null) }

    // Animated scanline and blinkers
    val scanlineAnim = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val blinkState = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val isBlinking = blinkState.value > 0.5f

    // Motion Frames configurations
    val simulatedFrames = remember(activeIdx, detectPeopleOnly, cameras) {
        val baseFrames = when (activeIdx) {
            0 -> listOf(
                DetectionFrame("Human near Doorbell", 94, 0.32f, 0.28f, 0.26f, 0.52f),
                DetectionFrame("Mail Package on porch", 88, 0.45f, 0.62f, 0.2f, 0.18f),
                DetectionFrame("Stray Cat on steps", 76, 0.15f, 0.76f, 0.15f, 0.12f)
            )
            1 -> listOf(
                DetectionFrame("SUV in Driveway Frame", 96, 0.2f, 0.32f, 0.5f, 0.45f),
                DetectionFrame("Delivery Agent delivering", 91, 0.65f, 0.25f, 0.18f, 0.52f)
            )
            2 -> listOf(
                DetectionFrame("Living Room motion: Pet", 95, 0.35f, 0.55f, 0.28f, 0.22f),
                DetectionFrame("Robot vacuum alert", 82, 0.1f, 0.82f, 0.14f, 0.08f),
                DetectionFrame("Human inside Living Room", 98, 0.3f, 0.2f, 0.25f, 0.61f)
            )
            else -> {
                val customCamName = cameras.getOrNull(activeIdx)?.name ?: "Custom Camera"
                listOf(
                    DetectionFrame("Human near $customCamName", 95, 0.33f, 0.26f, 0.3f, 0.55f),
                    DetectionFrame("Activity shadows near $customCamName", 84, 0.45f, 0.5f, 0.22f, 0.22f),
                    DetectionFrame("Visitor near $customCamName field", 92, 0.4f, 0.3f, 0.22f, 0.55f)
                )
            }
        }
        if (detectPeopleOnly) {
            baseFrames.filter { frame ->
                frame.label.contains("Human", ignoreCase = true) ||
                frame.label.contains("Agent", ignoreCase = true) ||
                frame.label.contains("Visitor", ignoreCase = true) ||
                frame.label.contains("Courier", ignoreCase = true) ||
                frame.label.contains("Person", ignoreCase = true)
            }
        } else {
            baseFrames
        }
    }

    var currentFrame by remember { mutableStateOf<DetectionFrame?>(null) }
    var simulationTicker by remember { mutableStateOf(0) }

    // Periodic simulation logic
    LaunchedEffect(activeIdx, isMotionActive, simulationTicker, selectedDayIndex, selectedTimeIndex) {
        if (!isMotionActive || selectedDayIndex != 0 || selectedTimeIndex != 4) {
            currentFrame = null
            return@LaunchedEffect
        }
        // Active framework loops
        kotlinx.coroutines.delay(4500)
        if (currentFrame == null) {
            val frame = simulatedFrames.randomOrNull()
            currentFrame = frame
            if (frame != null) {
                val timeString = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                localLogs.add(0, MotionIncidentLog(
                    id = java.util.UUID.randomUUID().toString(),
                    time = timeString,
                    cameraName = activeCamera.name,
                    label = frame.label,
                    confidence = frame.confidence
                ))
                val isPerson = frame.label.contains("Human", ignoreCase = true) ||
                               frame.label.contains("Agent", ignoreCase = true) ||
                               frame.label.contains("Visitor", ignoreCase = true) ||
                               frame.label.contains("Courier", ignoreCase = true) ||
                               frame.label.contains("Person", ignoreCase = true)

                activeToastMsg = if (isPerson) {
                    "PEOPLE ALERT: ${frame.label}!"
                } else {
                    "Motion Activity: ${frame.label} logged"
                }
                
                // Save to general DB
                viewModel.addSecurityAlert(
                    title = if (isPerson) "PEOPLE ALERT: ${activeCamera.name}" else "Motion: ${activeCamera.name}",
                    message = "${frame.label} detected (Certainty: ${frame.confidence}%)",
                    level = if (isPerson) "Warning" else "Info",
                    tag = "Motion"
                )
            }
        } else {
            currentFrame = null
        }
        simulationTicker++
    }

    // Clear alert toast after delay
    LaunchedEffect(activeToastMsg) {
        if (activeToastMsg != null) {
            kotlinx.coroutines.delay(3000)
            activeToastMsg = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
    ) {
        // Scrollable master container so everything fits nicely
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Live View", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                    Text(
                        text = if (isGridView) "Scanning ${cameras.size} feeds" else "Focus: ${activeCamera.name}",
                        fontSize = 13.sp,
                        color = SafeHavenOutlineDark
                    )
                }

                // Grid View toggler
                Row(
                    modifier = Modifier
                        .background(SafeHavenSurfaceLow, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isGridView = false },
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (!isGridView) SafeHavenSurfaceContainer else Color.Transparent, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "Monitor", 
                            modifier = Modifier.size(16.dp),
                            tint = if (!isGridView) SafeHavenDarkBlue else SafeHavenOutlineDark
                        )
                    }
                    IconButton(
                        onClick = { isGridView = true },
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (isGridView) SafeHavenSurfaceContainer else Color.Transparent, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView, 
                            contentDescription = "Grid", 
                            modifier = Modifier.size(16.dp),
                            tint = if (isGridView) SafeHavenDarkBlue else SafeHavenOutlineDark
                        )
                    }
                }
            }

            if (isGridView) {
                // Showing all 4 cameras in grid/list layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Camera Grid Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                    Text("Select a camera to open Smart Monitor", fontSize = 11.sp, color = SafeHavenOutlineDark)
                }

                cameras.forEachIndexed { index, feed ->
                    CameraCardLong(
                        feed = feed,
                        isSelected = activeIdx == index,
                        motionEnabled = motionActiveMap[index] ?: false,
                        onMotionToggle = { enabled ->
                            motionActiveMap[index] = enabled
                        },
                        onClick = { 
                            viewModel.selectCamera(index) 
                            isGridView = false // switch to monitor mode on select!
                            onNavigateToZoom()
                        }
                    )
                }

            } else {
                // Monitor View Mode
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeHavenDarkBlue),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clickable { onNavigateToZoom() }
                        .testTag("live_feed_monitor_box"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenOutline)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).clipToBounds()) {
                        val boxWidth = maxWidth
                        val boxHeight = maxHeight

                        AsyncImage(
                            model = displayImageUrl,
                            contentDescription = activeCamera.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Outer scanner overlay (Show only in real time monitoring)
                        if (isMotionActive && !isPlaybackActive) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val scanlineY = scanlineAnim.value * size.height

                                // Scanning sweeping bar
                                drawLine(
                                    color = Color(0xFF22C55E).copy(alpha = 0.5f),
                                    start = Offset(0f, scanlineY),
                                    end = Offset(size.width, scanlineY),
                                    strokeWidth = 2.dp.toPx()
                                )

                                // Green scanner corners
                                val cornerSz = 12.dp.toPx()
                                val edge = 8.dp.toPx()
                                // TL
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(edge, edge), Offset(edge + cornerSz, edge), 2.dp.toPx())
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(edge, edge), Offset(edge, edge + cornerSz), 2.dp.toPx())
                                // TR
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(size.width - edge, edge), Offset(size.width - edge - cornerSz, edge), 2.dp.toPx())
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(size.width - edge, edge), Offset(size.width - edge, edge + cornerSz), 2.dp.toPx())
                                // BL
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(edge, size.height - edge), Offset(edge + cornerSz, size.height - edge), 2.dp.toPx())
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(edge, size.height - edge), Offset(edge, size.height - edge - cornerSz), 2.dp.toPx())
                                // BR
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(size.width - edge, size.height - edge), Offset(size.width - edge - cornerSz, size.height - edge), 2.dp.toPx())
                                drawLine(Color(0xFF22C55E).copy(alpha = 0.7f), Offset(size.width - edge, size.height - edge), Offset(size.width - edge, size.height - edge - cornerSz), 2.dp.toPx())
                            }
                        }

                        // Blinking Watermark (Adapts based on LIVE vs HISTORIC mode)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (isBlinking) Color.Red else Color.Red.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = "LIVE • ${activeCamera.name.uppercase()}",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Active Motion tracking banner overlay (Show only in Live mode)
                        if (isMotionActive && !isPlaybackActive) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(SafeHavenGreen, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Text(
                                            text = "SCANNER ACTIVE",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                val isPerson = currentFrame?.label?.let { label ->
                                    label.contains("Human", ignoreCase = true) ||
                                    label.contains("Agent", ignoreCase = true) ||
                                    label.contains("Visitor", ignoreCase = true) ||
                                    label.contains("Courier", ignoreCase = true) ||
                                    label.contains("Person", ignoreCase = true)
                                } ?: false

                                Box(
                                    modifier = Modifier
                                        .background(if (isPerson) Color(0xFFEF4444) else Color(0xFFF97316), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = if (isBlinking) Color.White else Color.White.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (isPerson) "PEOPLE ALERT!" else "MOTION DETECTED",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.testTag("motion_detected_badge")
                                        )
                                    }
                                }
                            }
                        }

                        // Playback Timestamp overlay
                        if (isPlaybackActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Playback Speed",
                                        tint = SafeHavenGreen,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${currentDisplayTime} (1.0x)",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Target crosshairs bounding rect (Live mode)
                        if (!isPlaybackActive) {
                            currentFrame?.let { frame ->
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val rectL = frame.x * size.width
                                    val rectT = frame.y * size.height
                                    val rectW = frame.w * size.width
                                    val rectH = frame.h * size.height
                                    
                                    val activeColor = if (frame.label.contains("Intruder", ignoreCase = true)) Color(0xFFEF4444) else Color(0xFFFACC15)
                                    
                                    drawRect(
                                        color = activeColor,
                                        topLeft = Offset(rectL, rectT),
                                        size = Size(rectW, rectH),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    
                                    val bLen = 8.dp.toPx()
                                    drawLine(activeColor, Offset(rectL, rectT), Offset(rectL + bLen, rectT), 3.dp.toPx())
                                    drawLine(activeColor, Offset(rectL, rectT), Offset(rectL, rectT + bLen), 3.dp.toPx())
                                    
                                    drawLine(activeColor, Offset(rectL + rectW, rectT), Offset(rectL + rectW - bLen, rectT), 3.dp.toPx())
                                    drawLine(activeColor, Offset(rectL + rectW, rectT), Offset(rectL + rectW, rectT + bLen), 3.dp.toPx())
                                }

                                val pillX = boxWidth * frame.x
                                val pillY = (boxHeight * frame.y - 18.dp).coerceAtLeast(4.dp)

                                Box(
                                    modifier = Modifier
                                        .offset(x = pillX, y = pillY)
                                        .background(
                                            color = if (frame.label.contains("Intruder", ignoreCase = true)) Color(0xFFEF4444) else Color(0xFFD97706),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${frame.label.uppercase()} [${frame.confidence}%]",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Floating Audio Intercom HUD Overlay directly on the active feed
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .padding(4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TALKBACK",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )

                            // Talk Button (Mic)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = if (isTalkActive) SafeHavenGreen else Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.toggleTalk() }
                                    .testTag("active_feed_talk_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTalkActive) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = "Talk / Speak via Cam Intercom",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Listen Button (VolumeUp)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = if (isListenActive) SafeHavenGreen else Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.toggleListen() }
                                    .testTag("active_feed_listen_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListenActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Listen to Cam Audio Feed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Active Intercom Streaming Overlay Status Pill
                        if (isTalkActive || isListenActive) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(bottom = 22.dp, start = 10.dp)
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isTalkActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (isBlinking) SafeHavenGreen else Color.Transparent, CircleShape)
                                        )
                                        Text(
                                            text = "🎙️ TALK TO ${activeCamera.name.uppercase()}",
                                            color = SafeHavenGreen,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                if (isTalkActive && isListenActive) {
                                    Text("|", color = Color.White.copy(alpha = 0.3f), fontSize = 7.sp)
                                }
                                if (isListenActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (isBlinking) Color.Cyan else Color.Transparent, CircleShape)
                                        )
                                        Text(
                                            text = "🔊 HEAR ${activeCamera.name.uppercase()}",
                                            color = Color.Cyan,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Interactive "🔍 ZOOM SESSION" Button Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 10.dp, bottom = 26.dp)
                                .background(SafeHavenGreen, RoundedCornerShape(12.dp))
                                .clickable { onNavigateToZoom() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "TAP TO ZOOM",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Telemetry Row
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPlaybackActive) "H.265 ARCHIVE PLAYBACK • 30FPS" else "1080P • COIL LIVESTREAM DECODER",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isPlaybackActive) "BUFFER: SECURE STORAGE" else "BANDWIDTH: ${if (isMotionActive) "2.4" else "1.1"} MBPS",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }



                // Camera Selector Gallery Row (Tap to switch source on Analyzer)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Sources", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafeHavenOutlineDark)
                        Text("Tap to focus", fontSize = 11.sp, color = SafeHavenOutlineDark)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cameras.forEachIndexed { index, cam ->
                            val isFocused = activeIdx == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isFocused) SafeHavenGreen else SafeHavenSurfaceLow,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { 
                                        viewModel.selectCamera(index) 
                                        currentFrame = null // clear active frame during switch
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = cam.name.split(" ").firstOrNull() ?: cam.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFocused) Color.White else SafeHavenDarkBlue
                                    )
                                    if (isFocused && (isTalkActive || isListenActive)) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isTalkActive) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Microphone Active",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            if (isListenActive) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Speaker Active",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Action Intercom Control Pill specifically for the Active Focused camera source
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SafeHavenSurfaceLow, RoundedCornerShape(12.dp))
                            .border(1.dp, SafeHavenSurfaceContainer, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(SafeHavenGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = SafeHavenGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Intercom: ${activeCamera.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeHavenDarkBlue
                                )
                                Text(
                                    text = if (isTalkActive || isListenActive) "Connected to ${activeCamera.name}" else "Tap to route audio",
                                    fontSize = 9.sp,
                                    color = SafeHavenOutlineDark
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Talk Switcher
                            Button(
                                onClick = { viewModel.toggleTalk() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTalkActive) SafeHavenGreen else SafeHavenCardBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("quick_active_talk_${activeIdx}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isTalkActive) Color.White else SafeHavenDarkBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "TALK",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTalkActive) Color.White else SafeHavenDarkBlue
                                    )
                                }
                            }

                            // Listen Switcher
                            Button(
                                onClick = { viewModel.toggleListen() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListenActive) SafeHavenGreen else SafeHavenCardBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("quick_active_listen_${activeIdx}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (isListenActive) Color.White else SafeHavenDarkBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "LISTEN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isListenActive) Color.White else SafeHavenDarkBlue
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive Smart Motion Detection Control Hub Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenSurfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (motionActiveMap.values.any { it }) SafeHavenGreen else SafeHavenOutlineDark,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text("Standard Motion Detection", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                                    Text("Select which cameras monitor activity", fontSize = 11.sp, color = SafeHavenOutlineDark)
                                }
                            }
                        }

                        // Compact Camera Motion Selector List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            cameras.forEachIndexed { index, cam ->
                                val isCamMotionEnabled = motionActiveMap[index] ?: false
                                val isCurrentlyActive = (index == activeIdx)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isCurrentlyActive) SafeHavenSurfaceContainer else SafeHavenSurfaceLow,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            motionActiveMap[index] = !isCamMotionEnabled
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = if (isCamMotionEnabled) SafeHavenGreen else SafeHavenOutlineDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cam.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SafeHavenDarkBlue
                                                )
                                                if (isCurrentlyActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(SafeHavenGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "MONITORING",
                                                            color = SafeHavenGreen,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = if (isCamMotionEnabled) "Sensing Active" else "Sensors Off",
                                                fontSize = 11.sp,
                                                color = if (isCamMotionEnabled) SafeHavenGreen else SafeHavenOutlineDark
                                            )
                                        }
                                    }
                                    
                                    Switch(
                                        checked = isCamMotionEnabled,
                                        onCheckedChange = { motionActiveMap[index] = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SafeHavenGreen,
                                            uncheckedThumbColor = SafeHavenOutlineDark,
                                            uncheckedTrackColor = SafeHavenSurfaceLow
                                        ),
                                        modifier = Modifier.scale(0.85f).testTag("motion_selector_switch_${index}")
                                    )
                                }
                            }
                        }

                        // Information prompt when current camera motion is inactive
                        AnimatedVisibility(visible = !isMotionActive) {
                            Column {
                                Divider(color = SafeHavenSurfaceContainer)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Enable motion sensing on ${activeCamera.name} above to access standard radar sensitivity, people alerting priority, and simulator tests.",
                                    fontSize = 11.sp,
                                    color = SafeHavenOutlineDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )
                            }
                        }

                        // Sensitivity controls (Enable only if motion spotting is active)
                        AnimatedVisibility(visible = isMotionActive) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Divider(color = SafeHavenSurfaceContainer)
                                
                                // People Only Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (detectPeopleOnly) SafeHavenGreen else SafeHavenOutlineDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text("People Alert Priority", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                                            Text("Highlight people events with red alert badges", fontSize = 11.sp, color = SafeHavenOutlineDark)
                                        }
                                    }

                                    Switch(
                                        checked = detectPeopleOnly,
                                        onCheckedChange = { detectPeopleOnly = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SafeHavenGreen
                                        ),
                                        modifier = Modifier.testTag("people_detection_only_toggle")
                                    )
                                }

                                Divider(color = SafeHavenSurfaceContainer)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Radar Sensitivity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                                    Text(
                                        text = when {
                                            sensitivity < 0.4f -> "Low (Filters trees)"
                                            sensitivity < 0.8f -> "Medium (Smart Alert)"
                                            else -> "High (Detect butterflies)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeHavenGreen
                                    )
                                }

                                Slider(
                                    value = sensitivity,
                                    onValueChange = { sensitivity = it },
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = SafeHavenGreen,
                                        inactiveTrackColor = SafeHavenSurfaceContainer,
                                        thumbColor = SafeHavenGreen
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Select Mode layout
                                Text("Tracking Focus Zone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Smart Tracking", "Full Frame", "Perimeter Door").forEach { mode ->
                                        val isSel = detectionMode == mode
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    color = if (isSel) SafeHavenSurfaceContainer else SafeHavenSurfaceLow,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { detectionMode = mode }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mode,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) SafeHavenGreen else SafeHavenOutlineDark
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                // Fast Simulated alert trigger button
                                Button(
                                    onClick = {
                                        // Instantly generate a custom manual detection frame
                                        val randomFrames = listOf(
                                            DetectionFrame("Manual Scan: Human detected", 98, 0.44f, 0.35f, 0.22f, 0.5f),
                                            DetectionFrame("Manual Scan: Visitor spotted", 95, 0.5f, 0.22f, 0.24f, 0.48f),
                                            DetectionFrame("Manual Scan: Courier active", 90, 0.32f, 0.58f, 0.20f, 0.32f)
                                        )
                                        val picked = randomFrames.random()
                                        currentFrame = picked
                                        
                                        // Save logs
                                        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                        localLogs.add(0, MotionIncidentLog(
                                            id = java.util.UUID.randomUUID().toString(),
                                            time = timeStr,
                                            cameraName = activeCamera.name,
                                            label = picked.label,
                                            confidence = picked.confidence
                                        ))
                                        
                                        // Write to general alarm database so it stays real!
                                        viewModel.addSecurityAlert(
                                            title = "Manual Trigger Alert",
                                            message = "${picked.label} from command pad",
                                            level = "Warning",
                                            tag = "Motion"
                                        )
                                        
                                        activeToastMsg = "Force Detected Frame Spot logged successfully"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SafeHavenSurfaceLow),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = SafeHavenGreen, modifier = Modifier.size(16.dp))
                                        Text("Force Core Motion Spot Trigger", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                // Intercom Console Card Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeHavenSurfaceLow),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Channel Intercom Audio • ${activeCamera.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeHavenOutlineDark
                        )
 
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.toggleTalk() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTalkActive) SafeHavenGreen else SafeHavenCardBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isTalkActive) Color.White else SafeHavenDarkBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "TALK TO ${activeCamera.name.uppercase()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTalkActive) Color.White else SafeHavenDarkBlue
                                    )
                                }
                            }
 
                            Button(
                                onClick = { viewModel.toggleListen() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListenActive) SafeHavenGreen else SafeHavenCardBg
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (isListenActive) Color.White else SafeHavenDarkBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "LISTEN TO ${activeCamera.name.uppercase()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isListenActive) Color.White else SafeHavenDarkBlue
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Motion Logs Timeline Section (Only shown if logs exist)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenSurfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = SafeHavenGreen, modifier = Modifier.size(20.dp))
                                Text("Motion Event Logger", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                            }

                            if (selectedDayIndex == 0 && selectedTimeIndex == 4 && currentHistoryLogs.isNotEmpty()) {
                                Text(
                                    text = "Clear Session",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafeHavenError,
                                    modifier = Modifier.clickable { localLogs.clear() }
                                )
                            }
                        }

                        if (currentHistoryLogs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlaybackActive) "No historical alerts logged during this archive interval." else "No motion events recorded in this session.",
                                    fontSize = 11.sp,
                                    color = SafeHavenOutlineDark
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                currentHistoryLogs.take(5).forEach { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SafeHavenSurfaceLow, RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        color = if (log.label.contains("Intruder", ignoreCase = true)) SafeHavenError else SafeHavenGreen,
                                                        shape = CircleShape
                                                    )
                                            )
                                            Column {
                                                Text(
                                                    text = log.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SafeHavenDarkBlue
                                                )
                                                Text(
                                                    text = "${log.cameraName} • ${log.time}",
                                                    fontSize = 10.sp,
                                                    color = SafeHavenOutlineDark
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${log.confidence}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SafeHavenGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Overlay Floating Banner Notification representing instant toast
        AnimatedVisibility(
            visible = activeToastMsg != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .background(SafeHavenDarkBlue.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .border(1.dp, SafeHavenGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                    Text(
                        text = activeToastMsg ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class DetectionFrame(
    val label: String,
    val confidence: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

data class MotionIncidentLog(
    val id: String,
    val time: String,
    val cameraName: String,
    val label: String,
    val confidence: Int
)

data class CameraFeedInfo(val name: String, val imageUrl: String)

@Composable
fun CameraCard(feed: CameraFeedInfo, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafeHavenSurfaceLow),
        modifier = Modifier
            .aspectRatio(1.0f)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) SafeHavenGreen else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clipToBounds()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = feed.imageUrl,
                contentDescription = feed.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // live red/white badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }

            // label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(feed.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CameraCardLong(
    feed: CameraFeedInfo, 
    isSelected: Boolean, 
    motionEnabled: Boolean,
    onMotionToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) SafeHavenGreen else SafeHavenSurfaceContainer,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(140.dp).fillMaxHeight()) {
                AsyncImage(
                    model = feed.imageUrl,
                    contentDescription = feed.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .background(Color.Red, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(feed.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                Text("Connected • Streaming 1080p", fontSize = 12.sp, color = SafeHavenOutlineDark)
                
                if (motionEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Motion Detected", 
                            color = Color(0xFFEF4444), 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onMotionToggle(!motionEnabled)
                }
            ) {
                Text("Motion", fontSize = 9.sp, color = SafeHavenOutlineDark, fontWeight = FontWeight.Bold)
                Switch(
                    checked = motionEnabled,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SafeHavenGreen,
                        uncheckedThumbColor = SafeHavenOutlineDark,
                        uncheckedTrackColor = SafeHavenSurfaceLow
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}


// ============================================
// 5. DEVICES SCREEN (My Devices)
// ============================================
@Composable
fun DevicesScreen(viewModel: SafeHavenViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var deviceToDelete by remember { mutableStateOf<Device?>(null) }
    var deviceToRename by remember { mutableStateOf<Device?>(null) }

    // Filtered devices based on the search query
    val filteredDevices = remember(devices, searchQuery) {
        if (searchQuery.isBlank()) {
            devices
        } else {
            devices.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Subdividing into filtered categorized buckets
    val securityDevices = filteredDevices.filter { it.category == "Security" }
    val climateDevices = filteredDevices.filter { it.category == "Climate" }
    val lightingDevices = filteredDevices.filter { it.category == "Lighting" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("Devices", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SafeHavenDarkBlue)
                    Text("Manage your ecosystem of protection.", fontSize = 14.sp, color = SafeHavenOutlineDark)
                }

                // Modern Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search connected devices...", color = SafeHavenOutlineDark) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = SafeHavenOutlineDark
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(48.dp) // Touch target of 48dp
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = SafeHavenOutlineDark
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SafeHavenCardBg,
                        unfocusedContainerColor = SafeHavenSurfaceLow,
                        focusedBorderColor = SafeHavenGreen,
                        unfocusedBorderColor = SafeHavenSurfaceContainer,
                        focusedTextColor = SafeHavenDarkBlue,
                        unfocusedTextColor = SafeHavenDarkBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("devices_search_bar")
                )
            }
        }

        // Empty Search Results State fallback
        if (filteredDevices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenSurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(SafeHavenSurfaceLow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SafeHavenOutlineDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No devices found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeHavenDarkBlue
                        )
                        Text(
                            text = "We couldn't find any connected devices matching \"$searchQuery\". Try checking the spelling or use a different search term.",
                            fontSize = 13.sp,
                            color = SafeHavenOutlineDark,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { searchQuery = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Clear Search", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Category: Security
        if (securityDevices.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Security", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                    Text(
                        text = "${securityDevices.size} Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeHavenGreen,
                        modifier = Modifier
                            .background(SafeHavenSecondaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(securityDevices) { device ->
                SecurityDeviceItem(device = device, viewModel = viewModel, onRenameClick = { deviceToRename = it }, onDeleteClick = { deviceToDelete = it })
            }
        }

        // Category: Climate
        if (climateDevices.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Climate", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                    Text(
                        text = "${climateDevices.size} Devices",
                        fontSize = 11.sp,
                        color = SafeHavenOutlineDark,
                        modifier = Modifier
                            .background(SafeHavenSurfaceLow, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(climateDevices) { device ->
                ClimateDeviceItem(device = device, viewModel = viewModel, onRenameClick = { deviceToRename = it }, onDeleteClick = { deviceToDelete = it })
            }
        }

        // Category: Lighting
        if (lightingDevices.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lighting", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                    Text(
                        text = "${lightingDevices.size} Devices",
                        fontSize = 11.sp,
                        color = SafeHavenOutlineDark,
                        modifier = Modifier
                            .background(SafeHavenSurfaceLow, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            items(lightingDevices) { device ->
                LightingDeviceItem(device = device, viewModel = viewModel, onRenameClick = { deviceToRename = it }, onDeleteClick = { deviceToDelete = it })
            }
        }
    }

    // Confirmation Modal for Deletion/Removal of Device
    if (deviceToDelete != null) {
        val device = deviceToDelete!!
        Dialog(onDismissRequest = { deviceToDelete = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Remove Device?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeHavenDarkBlue
                    )

                    Text(
                        text = "Are you sure you want to remove ${device.name}? This will revoke its permission and disconnect it from your SafeHaven home network.",
                        fontSize = 14.sp,
                        color = SafeHavenOutlineDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { deviceToDelete = null },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel", color = SafeHavenOutlineDark, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.deleteDevice(device)
                                deviceToDelete = null
                             },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Renaming Device
    if (deviceToRename != null) {
        val device = deviceToRename!!
        var renameInputText by remember { mutableStateOf(device.name) }
        Dialog(onDismissRequest = { deviceToRename = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Rename Device",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeHavenDarkBlue
                    )

                    Text(
                        text = "Type a new name for your ${device.category.lowercase()} device.",
                        fontSize = 14.sp,
                        color = SafeHavenOutlineDark
                    )

                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        placeholder = { Text("e.g. Back Patio Sensor") },
                        label = { Text("Device Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SafeHavenDarkBlue,
                            unfocusedTextColor = SafeHavenDarkBlue,
                            focusedBorderColor = SafeHavenGreen,
                            unfocusedBorderColor = SafeHavenOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_rename_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { deviceToRename = null },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel", color = SafeHavenOutlineDark, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (renameInputText.isNotBlank()) {
                                    viewModel.renameDevice(device, renameInputText.trim())
                                    deviceToRename = null
                                }
                            },
                            enabled = renameInputText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityDeviceItem(device: Device, viewModel: SafeHavenViewModel, onRenameClick: (Device) -> Unit, onDeleteClick: (Device) -> Unit) {
    val context = LocalContext.current
    var showControllerOpen by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showControllerOpen = !showControllerOpen }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SafeHavenSurfaceLow, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (device.iconName) {
                                    "lock" -> Icons.Default.Lock
                                    "videocam" -> Icons.Default.Videocam
                                    "sensors" -> Icons.Default.Sensors
                                    else -> Icons.Default.Router
                                },
                                contentDescription = null,
                                tint = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                            )
                        }
                        // Tiny Status Dot Corner Badge
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .background(
                                    color = if (device.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    shape = CircleShape
                                )
                                .border(2.dp, SafeHavenCardBg, CircleShape)
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (device.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (device.isOnline) "Online • ${device.statusText}" else "Offline (Disconnected)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (device.isOnline) {
                                    if (device.statusText == "Locked") SafeHavenGreen else SafeHavenOutlineDark
                                } else {
                                    SafeHavenError
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onRenameClick(device) },
                        modifier = Modifier.size(40.dp).testTag("rename_security_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Device",
                            tint = SafeHavenOutlineDark
                        )
                    }

                    IconButton(
                        onClick = { onDeleteClick(device) },
                        modifier = Modifier.size(40.dp).testTag("delete_security_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Device",
                            tint = SafeHavenOutlineDark
                        )
                    }

                    IconButton(
                        onClick = { android.widget.Toast.makeText(context, "Settings for ${device.name}", android.widget.Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.size(40.dp).testTag("settings_security_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Device Settings",
                            tint = SafeHavenOutlineDark
                        )
                    }
                }
            }

            // Expanded Lock Toggle Controller Drawer
            if (showControllerOpen && device.iconName == "lock") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SafeHavenSurfaceLow)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Secure deadbolt status", fontSize = 12.sp, color = SafeHavenDarkBlue.copy(alpha = 0.70f))
                    Button(
                        onClick = { viewModel.toggleLock(device.id, device.name, device.statusText == "Locked") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (device.statusText == "Locked") SafeHavenError else SafeHavenGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (device.statusText == "Locked") "Unlock" else "Lock",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClimateDeviceItem(device: Device, viewModel: SafeHavenViewModel, onRenameClick: (Device) -> Unit, onDeleteClick: (Device) -> Unit) {
    val context = LocalContext.current
    var mutableTemp by remember { mutableStateOf(device.floatValue) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SafeHavenSurfaceLow, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                            )
                        }
                        // Tiny Status Dot Corner Badge
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .background(
                                    color = if (device.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    shape = CircleShape
                                )
                                .border(2.dp, SafeHavenCardBg, CircleShape)
                        )
                    }

                    Column {
                        Text(
                            text = device.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (device.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = if (device.isOnline) "Online • ${device.statusText}" else "Offline (Disconnected)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (device.isOnline) SafeHavenOutlineDark else SafeHavenError
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onRenameClick(device) },
                        modifier = Modifier.size(40.dp).testTag("rename_climate_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Device",
                            tint = SafeHavenOutlineDark
                        )
                    }

                    IconButton(
                        onClick = { onDeleteClick(device) },
                        modifier = Modifier.size(40.dp).testTag("delete_climate_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Device",
                            tint = SafeHavenOutlineDark
                        )
                    }

                    IconButton(
                        onClick = { android.widget.Toast.makeText(context, "Settings for ${device.name}", android.widget.Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.size(40.dp).testTag("settings_climate_device_${device.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Device Settings",
                            tint = SafeHavenOutlineDark
                        )
                    }

                    Text(
                        text = "${mutableTemp.toInt()}°",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dial Temperate controls slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    enabled = device.isOnline,
                    onClick = {
                        mutableTemp -= 1f
                        viewModel.updateThermostatTemp(device.id, mutableTemp)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (device.isOnline) SafeHavenSurfaceLow else SafeHavenSurfaceLow.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                    )
                }

                Slider(
                    enabled = device.isOnline,
                    value = mutableTemp,
                    onValueChange = {
                        mutableTemp = it
                    },
                    onValueChangeFinished = {
                        viewModel.updateThermostatTemp(device.id, mutableTemp)
                    },
                    valueRange = 60f..85f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = SafeHavenGreen,
                        inactiveTrackColor = SafeHavenOutline.copy(alpha = 0.5f),
                        thumbColor = SafeHavenGreen,
                        disabledActiveTrackColor = SafeHavenOutline.copy(alpha = 0.3f),
                        disabledInactiveTrackColor = SafeHavenOutline.copy(alpha = 0.15f),
                        disabledThumbColor = SafeHavenOutline
                    ),
                    modifier = Modifier.weight(1.0f)
                )

                IconButton(
                    enabled = device.isOnline,
                    onClick = {
                        mutableTemp += 1f
                        viewModel.updateThermostatTemp(device.id, mutableTemp)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (device.isOnline) SafeHavenSurfaceLow else SafeHavenSurfaceLow.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                    )
                }
            }
        }
    }
}

@Composable
fun LightingDeviceItem(device: Device, viewModel: SafeHavenViewModel, onRenameClick: (Device) -> Unit, onDeleteClick: (Device) -> Unit) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (device.isOn && device.isOnline) SafeHavenSecondaryContainer.copy(alpha = 0.2f) else SafeHavenSurfaceLow,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (device.isOnline) {
                                if (device.isOn) SafeHavenOnSecondaryContainer else SafeHavenOutline
                            } else {
                                SafeHavenOutlineDark
                            }
                        )
                    }
                    // Tiny Status Dot Corner Badge
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .background(
                                color = if (device.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                shape = CircleShape
                            )
                            .border(2.dp, SafeHavenCardBg, CircleShape)
                    )
                }

                Column {
                    Text(
                        text = device.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (device.isOnline) SafeHavenDarkBlue else SafeHavenOutlineDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (device.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = if (device.isOnline) "Online • ${device.statusText}" else "Offline (Disconnected)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (device.isOnline) SafeHavenOutlineDark else SafeHavenError
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onRenameClick(device) },
                    modifier = Modifier.size(40.dp).testTag("rename_lighting_device_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename Device",
                        tint = SafeHavenOutlineDark
                    )
                }

                IconButton(
                    onClick = { onDeleteClick(device) },
                    modifier = Modifier.size(40.dp).testTag("delete_lighting_device_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Device",
                        tint = SafeHavenOutlineDark
                    )
                }

                IconButton(
                    onClick = { android.widget.Toast.makeText(context, "Settings for ${device.name}", android.widget.Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.size(40.dp).testTag("settings_lighting_device_${device.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Device Settings",
                        tint = SafeHavenOutlineDark
                    )
                }

                // Power Switch Toggle
                Switch(
                    enabled = device.isOnline,
                    checked = device.isOn,
                    onCheckedChange = { viewModel.toggleDevicePower(device.id, device.isOn, device.name) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SafeHavenGreen,
                        uncheckedThumbColor = SafeHavenOutline,
                        uncheckedTrackColor = SafeHavenSurfaceContainer
                    )
                )
            }
        }
    }
}


// ============================================
// 6. ALERTS SCREEN (Chronological Security Logs)
// ============================================
@Composable
fun AlertsScreen(viewModel: SafeHavenViewModel) {
    val rawAlerts by viewModel.alerts.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredAlerts = remember(rawAlerts, selectedFilter) {
        when (selectedFilter) {
            "Unread" -> rawAlerts.filter { !it.isRead }
            "Critical" -> rawAlerts.filter { it.level == "Critical" }
            "Warning" -> rawAlerts.filter { it.level == "Warning" }
            else -> rawAlerts
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Alerts",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SafeHavenDarkBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Filter chips horizontal row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Unread", "Critical", "Warning").forEach { filter ->
                val isSelected = selectedFilter == filter
                Button(
                    onClick = { selectedFilter = filter },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SafeHavenDarkBlue else SafeHavenSurfaceLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else SafeHavenDarkBlue
                    )
                }
            }
        }

        // List
        if (filteredAlerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsNone,
                        contentDescription = "Empty",
                        tint = SafeHavenOutline,
                        modifier = Modifier.size(64.dp)
                    )
                    Text("No alerts found in active cache.", color = SafeHavenOutlineDark)
                }
            }
        } else {
            // Sorter grouped chronological list
            val grouped = filteredAlerts.groupBy { it.dateGroup }

            LazyColumn(
                modifier = Modifier.weight(1.0f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (dateHeader, list) ->
                    item {
                        Text(
                            text = dateHeader.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeHavenOutline.copy(alpha = 1.0f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(list) { alert ->
                        AlertItemCard(alert = alert, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertItemCard(alert: SecurityAlert, viewModel: SafeHavenViewModel) {
    var isDismissed by remember { mutableStateOf(false) }

    if (!isDismissed) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SafeHavenSurfaceLow),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.markAlertRead(alert.id)
                }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            when (alert.level) {
                                "Critical" -> SafeHavenErrorContainer
                                "Warning" -> SafeHavenWarningContainer
                                else -> SafeHavenSurfaceLow
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (alert.level) {
                            "Critical" -> Icons.Default.NotificationImportant
                            "Warning" -> Icons.Default.Warning
                            else -> Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = when (alert.level) {
                            "Critical" -> SafeHavenError
                            "Warning" -> SafeHavenWarning
                            else -> SafeHavenGreen
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title + Text block
                Column(modifier = Modifier.weight(1.0f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alert.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        Text(alert.timestamp, fontSize = 12.sp, color = SafeHavenOutlineDark)
                    }

                    Text(alert.message, fontSize = 14.sp, color = SafeHavenOutlineDark, modifier = Modifier.padding(bottom = 8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(
                                    when (alert.level) {
                                        "Critical" -> SafeHavenError.copy(alpha = 0.1f)
                                        "Warning" -> SafeHavenWarning.copy(alpha = 0.1f)
                                        else -> SafeHavenGreen.copy(alpha = 0.12f)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = alert.level,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (alert.level) {
                                    "Critical" -> SafeHavenError
                                    "Warning" -> SafeHavenOnSecondaryContainer
                                    else -> SafeHavenGreen
                                }
                            )
                        }

                        if (alert.tag.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(SafeHavenSurfaceLow, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(alert.tag, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SafeHavenDarkBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ============================================
// 7. ADD DEVICE SCREEN (Config / Animated QR Scanner)
// ============================================
@Composable
fun AddDeviceScreen(
    viewModel: SafeHavenViewModel,
    onDeviceAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var scannedCategorySelected by remember { mutableStateOf<String?>(null) }
    var inputDeviceName by remember { mutableStateOf("") }
    var selectedConnectionType by remember { mutableStateOf("Wired") }
    var isSavingDevice by remember { mutableStateOf(false) }

    // Scanner beam loop animation
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QR Scanner View Finder Box
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.Black, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
            ) {
                // Futuristic mock placeholder camera image representation
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1558002038-1055907df827?w=400",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.6f)
                )

                // High tech scan overlay laser brush
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw focus corners
                    val beamY = height * scanLineProgress
                    val lineWeight = 4f

                    // Green Horizontal scan sweep line
                    drawLine(
                        color = Color(0xFF22C55E),
                        start = Offset(width * 0.15f, beamY),
                        end = Offset(width * 0.85f, beamY),
                        strokeWidth = lineWeight
                    )

                    // Draw focus bracket guides
                    val startX = width * 0.20f
                    val endX = width * 0.80f
                    val startY = height * 0.20f
                    val endY = height * 0.80f
                    val length = 32f

                    // Top Left Bracket
                    drawLine(Color(0xFF22C55E), Offset(startX, startY), Offset(startX + length, startY), strokeWidth = lineWeight)
                    drawLine(Color(0xFF22C55E), Offset(startX, startY), Offset(startX, startY + length), strokeWidth = lineWeight)

                    // Top Right Bracket
                    drawLine(Color(0xFF22C55E), Offset(endX, startY), Offset(endX - length, startY), strokeWidth = lineWeight)
                    drawLine(Color(0xFF22C55E), Offset(endX, startY), Offset(endX, startY + length), strokeWidth = lineWeight)

                    // Bottom Left Bracket
                    drawLine(Color(0xFF22C55E), Offset(startX, endY), Offset(startX + length, endY), strokeWidth = lineWeight)
                    drawLine(Color(0xFF22C55E), Offset(startX, endY), Offset(startX, endY - length), strokeWidth = lineWeight)

                    // Bottom Right Bracket
                    drawLine(Color(0xFF22C55E), Offset(endX, endY), Offset(endX - length, endY), strokeWidth = lineWeight)
                    drawLine(Color(0xFF22C55E), Offset(endX, endY), Offset(endX, endY - length), strokeWidth = lineWeight)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Align QR Code",
                        color = SafeHavenGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Point your camera at the QR code located on the back or bottom of your SafeHaven device.",
                fontSize = 13.sp,
                color = SafeHavenOutlineDark,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // manual creation sheet divider
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Divider(modifier = Modifier.weight(1.0f), color = SafeHavenOutline.copy(alpha = 0.3f))
                Text(
                    text = "MANUAL SETUP",
                    fontSize = 11.sp,
                    color = SafeHavenOutlineDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Divider(modifier = Modifier.weight(1.0f), color = SafeHavenOutline.copy(alpha = 0.3f))
            }
        }

        // Manual Categories Selection Sheet
        val items = listOf(
            CategorySelection("Cameras", "Indoor, Outdoor & Doorbell", Icons.Default.Videocam),
            CategorySelection("Sensors", "Motion, Entry & Water leak", Icons.Default.Sensors),
            CategorySelection("Smart Locks", "Deadbolts & Keypads", Icons.Default.Lock),
            CategorySelection("Hubs & Bridges", "Central Base Stations", Icons.Default.Router),
            CategorySelection("Lighting", "Smart Bulbs & Strips", Icons.Default.Lightbulb)
        )

        items(items) { cat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scannedCategorySelected = cat.title
                        selectedConnectionType = "Wired"
                        inputDeviceName = ""
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(SafeHavenSurfaceLow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = cat.icon, contentDescription = null, tint = SafeHavenGreen)
                        }

                        Column {
                            Text(cat.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                            Text(cat.subtitle, fontSize = 12.sp, color = SafeHavenOutlineDark)
                        }
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SafeHavenOutline)
                }
            }
        }
    }

    // Modal Sheet representation containing Device registration inputs Form
    if (scannedCategorySelected != null) {
        Dialog(onDismissRequest = { scannedCategorySelected = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Register ${scannedCategorySelected}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeHavenDarkBlue
                    )

                    OutlinedTextField(
                        value = inputDeviceName,
                        onValueChange = { inputDeviceName = it },
                        placeholder = { 
                            if (scannedCategorySelected == "Cameras") {
                                Text("e.g. Front Door Camera")
                            } else {
                                Text("e.g. Master Bedroom Lock")
                            }
                        },
                        label = { Text("Custom Device Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeHavenGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (scannedCategorySelected == "Cameras") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connection Type",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeHavenDarkBlue
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Wired Camera Option Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedConnectionType = "Wired" }
                                    .border(
                                        width = 2.dp,
                                        color = if (selectedConnectionType == "Wired") SafeHavenGreen else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedConnectionType == "Wired") SafeHavenSecondaryContainer else SafeHavenSurfaceLow
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cable,
                                        contentDescription = null,
                                        tint = if (selectedConnectionType == "Wired") SafeHavenOnSecondaryContainer else SafeHavenOutlineDark,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Wired Camera",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedConnectionType == "Wired") SafeHavenOnSecondaryContainer else SafeHavenDarkBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Ethernet / Cable",
                                        fontSize = 11.sp,
                                        color = if (selectedConnectionType == "Wired") SafeHavenOnSecondaryContainer.copy(alpha = 0.8f) else SafeHavenOutlineDark
                                    )
                                }
                            }

                            // WiFi Camera Option Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedConnectionType = "WiFi" }
                                    .border(
                                        width = 2.dp,
                                        color = if (selectedConnectionType == "WiFi") SafeHavenGreen else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedConnectionType == "WiFi") SafeHavenSecondaryContainer else SafeHavenSurfaceLow
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = if (selectedConnectionType == "WiFi") SafeHavenOnSecondaryContainer else SafeHavenOutlineDark,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "WiFi Camera",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedConnectionType == "WiFi") SafeHavenOnSecondaryContainer else SafeHavenDarkBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Wireless 2.4/5GHz",
                                        fontSize = 11.sp,
                                        color = if (selectedConnectionType == "WiFi") SafeHavenOnSecondaryContainer.copy(alpha = 0.8f) else SafeHavenOutlineDark
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { scannedCategorySelected = null },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenSurfaceLow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Text("Cancel", color = SafeHavenDarkBlue)
                        }

                        Button(
                            onClick = {
                                if (inputDeviceName.isNotEmpty() && !isSavingDevice) {
                                    isSavingDevice = true
                                    scope.launch {
                                        viewModel.saveDevice(
                                            name = inputDeviceName,
                                            category = scannedCategorySelected!!,
                                            connectionType = if (scannedCategorySelected == "Cameras") selectedConnectionType else null
                                        )
                                        scannedCategorySelected = null
                                        isSavingDevice = false
                                        onDeviceAdded()
                                    }
                                }
                            },
                            enabled = inputDeviceName.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.0f)
                                .testTag("add_device_category_button")
                        ) {
                            Text("Add Device", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class CategorySelection(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)


// ============================================
// 8. USER PROFILE SCREEN (Settings Console)
// ============================================
@Composable
fun ProfileScreen(viewModel: SafeHavenViewModel) {
    val bEnabled by viewModel.biometricsEnabled.collectAsStateWithLifecycle()
    val pushVal by viewModel.pushNotifications.collectAsStateWithLifecycle()
    val criticalVal by viewModel.criticalAlerts.collectAsStateWithLifecycle()
    val motionVal by viewModel.motionDetection.collectAsStateWithLifecycle()
    val motionPushVal by viewModel.motionPushNotifications.collectAsStateWithLifecycle()
    val armedVal by viewModel.systemArmed.collectAsStateWithLifecycle()

    val name by viewModel.userName.collectAsStateWithLifecycle()
    val email by viewModel.userEmail.collectAsStateWithLifecycle()
    val phone by viewModel.userPhone.collectAsStateWithLifecycle()
    val address by viewModel.userAddress.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Account visual Profile Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile image with edit badge indicator
                Box {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(Color.White, CircleShape)
                            .border(4.dp, SafeHavenGreen, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCLQi2RVyG4AejShGYBZ1WYzp3iAVCrnjzx9zvbALu2FrPC1aWiv5avzBiAyRo8E3DXXE5zb5hJSG2JxdnoPniY7YWNYhbwX-c6ImIxPCamBWiqySBvhzAA7B5kWXo8myDSdrCR9NkwqHRif1UIDpGSxo9-nDLnQbmCz4dpEWyL0C4Qi_2PsgHKTBNx1er6hie5PF4CPT9AQzPxx0nbJ4PRXHK0LJeuZWCaidLeueq5S9n8PSxxmkTKvxtQ5VJIdPmEpt84vemWNDp3",
                            contentDescription = "Portrait photo representing corporate homeowner style.",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SafeHavenGreen, CircleShape)
                            .align(Alignment.BottomEnd)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable { showEditProfileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile Info", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(SafeHavenSecondaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Home Owner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeHavenOnSecondaryContainer)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(email, fontSize = 14.sp, color = SafeHavenOutlineDark)
                Text(phone, fontSize = 14.sp, color = SafeHavenOutlineDark)
            }
        }

        // Biometrics Card Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(SafeHavenSurfaceLow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = SafeHavenDarkBlue)
                        }

                        Column {
                            Text("Biometric Authentication", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                            Text("FaceID or Fingerprint", fontSize = 12.sp, color = SafeHavenOutlineDark)
                        }
                    }

                    Switch(
                        checked = bEnabled,
                        onCheckedChange = { viewModel.setBiometrics(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = SafeHavenGreen)
                    )
                }
            }
        }

        // Home Coordinates address details card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SafeHavenSurfaceLow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.HomeWork, contentDescription = null, tint = SafeHavenDarkBlue)
                            }
                            Text("Home Address", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        }

                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Edit Coordinates", tint = SafeHavenOutline)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = address,
                        fontSize = 14.sp,
                        color = SafeHavenOutlineDark,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 60.dp)
                    )
                }
            }
        }

        // Emergency Contacts section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(SafeHavenErrorContainer.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = SafeHavenError)
                            }
                            Text("Emergency Contacts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SafeHavenOutline)
                    }

                    // David Johnson Contact row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .padding(start = 60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("David Johnson", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Primary • +1 (555) 123-4567", fontSize = 12.sp, color = SafeHavenOutlineDark)
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SafeHavenSurfaceLow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = SafeHavenGreen, modifier = Modifier.size(16.dp))
                        }
                    }

                    Divider(modifier = Modifier.fillMaxWidth().padding(start = 60.dp, top = 6.dp, bottom = 6.dp), color = SafeHavenSurfaceContainer)

                    // Austin Private Security Contact row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .padding(start = 60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Austin Private Security", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Dispatcher • +1 (555) 911-SAFE", fontSize = 12.sp, color = SafeHavenOutlineDark)
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(SafeHavenSurfaceLow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = SafeHavenGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Account Custom Notifications Config Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = SafeHavenDarkBlue)
                        Text("Notifications", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle: Master push notifications
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Push Notifications", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Master toggle for all alerts", fontSize = 11.sp, color = SafeHavenOutlineDark)
                        }
                        Switch(
                            checked = pushVal,
                            onCheckedChange = { viewModel.setPushNotifications(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SafeHavenGreen)
                        )
                    }

                    Divider(color = SafeHavenSurfaceContainer)

                    // Toggle: Critical alerts override mode
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Critical Alerts", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Sound overrides for emergencies", fontSize = 11.sp, color = SafeHavenOutlineDark)
                        }
                        Switch(
                            checked = criticalVal,
                            onCheckedChange = { viewModel.setCriticalAlerts(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SafeHavenGreen)
                        )
                    }

                    Divider(color = SafeHavenSurfaceContainer)

                    // Toggle: Motion detection reports
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Motion Detection", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Alerts when movement is detected", fontSize = 11.sp, color = SafeHavenOutlineDark)
                        }
                        Switch(
                            checked = motionVal,
                            onCheckedChange = { viewModel.setMotionDetection(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SafeHavenGreen)
                        )
                    }

                    Divider(color = SafeHavenSurfaceContainer)

                    // Toggle: Motion push notifications
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Motion Push Notifications", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Receive push notifications for motion events", fontSize = 11.sp, color = SafeHavenOutlineDark)
                        }
                        Switch(
                            checked = motionPushVal,
                            onCheckedChange = { viewModel.setMotionPushNotifications(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SafeHavenGreen),
                            modifier = Modifier.testTag("motion_push_notifications_toggle")
                        )
                    }

                    Divider(color = SafeHavenSurfaceContainer)

                    // Toggle: Armed trigger signals
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("System Armed/Disarmed", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                            Text("Status change notifications", fontSize = 11.sp, color = SafeHavenOutlineDark)
                        }
                        Switch(
                            checked = armedVal,
                            onCheckedChange = { viewModel.setSystemArmed(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = SafeHavenGreen)
                        )
                    }
                }
            }
        }

        // Action Settings buttons
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockPerson, contentDescription = null, tint = SafeHavenOutline)
                            Text("Privacy & Security", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SafeHavenOutline)
                    }

                    Divider(color = SafeHavenSurfaceContainer)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Help, contentDescription = null, tint = SafeHavenOutline)
                            Text("Help & Support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SafeHavenDarkBlue)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SafeHavenOutline)
                    }
                }
            }
        }

        // Active Sign Out dispatch Button point returning to authentication
        item {
            Button(
                onClick = { viewModel.signOut() },
                colors = ButtonDefaults.buttonColors(containerColor = SafeHavenSurfaceLow),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("sign_out_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = SafeHavenError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, color = SafeHavenError)
                }
            }
        }
    }

    // Modal Edit Profile Details dialog layout
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(name) }
        var tempEmail by remember { mutableStateOf(email) }
        var tempAddress by remember { mutableStateOf(address) }
        var tempPhone by remember { mutableStateOf(phone) }

        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Profile Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeHavenDarkBlue
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Full Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeHavenGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("Email Address") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeHavenGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Phone Number") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeHavenGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("Physical Address Coordinates") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafeHavenGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showEditProfileDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenSurfaceLow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Text("Cancel", color = SafeHavenDarkBlue)
                        }

                        Button(
                            onClick = {
                                viewModel.updateUserProfile(tempName, tempEmail, tempAddress, tempPhone)
                                showEditProfileDialog = false
                            },
                            enabled = tempName.isNotEmpty() && tempEmail.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeHavenGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class MotionEventClip(
    val id: Int,
    val timeLabel: String,
    val duration: String,
    val startMinute: Int,
    val eventTitle: String,
    val desc: String,
    val clipImageUrl: String
)

@Composable
fun ZoomScreen(
    viewModel: SafeHavenViewModel,
    onBack: () -> Unit = {}
) {
    val activeIdx by viewModel.activeCameraIndex.collectAsStateWithLifecycle()
    
    val dbDevices by viewModel.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val cameras = remember(dbDevices) {
        val dbCameras = dbDevices.filter { it.iconName == "videocam" }
        dbCameras.map { dbCam ->
            val lowerName = dbCam.name.lowercase()
            val imageUrl = when {
                lowerName.contains("front door") -> "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=400"
                lowerName.contains("driveway") -> "https://images.unsplash.com/photo-1542435503-956c469947f6?w=400"
                lowerName.contains("living room") -> "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=400"
                lowerName.contains("backyard") -> "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?w=400"
                else -> {
                    val fallbackImages = listOf(
                        "https://images.unsplash.com/photo-1558002038-1055907df827?w=400",
                        "https://images.unsplash.com/photo-1508962914676-134849a727f0?w=400",
                        "https://images.unsplash.com/photo-1521207418485-99c705420785?w=400"
                    )
                    fallbackImages[Math.abs(dbCam.id) % fallbackImages.size]
                }
            }
            CameraFeedInfo(dbCam.name, imageUrl)
        }
    }

    val activeCamera = remember(cameras, activeIdx) {
        cameras.getOrNull(activeIdx) ?: cameras.firstOrNull() ?: CameraFeedInfo("Live Feed", "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=400")
    }

    // Zoom & Pan states
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }

    // Day Selection (7 days history past dates)
    val historyDays = remember {
        val dateFormat = java.text.SimpleDateFormat("EEE dd", java.util.Locale.getDefault())
        val fullDateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        (0..6).map { offset ->
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DATE, -offset)
            val dayName = when (offset) {
                0 -> "Today"
                1 -> "Yest."
                else -> dateFormat.format(cal.time)
            }
            val dateLabel = fullDateFormat.format(cal.time)
            val shortDay = java.text.SimpleDateFormat("E", java.util.Locale.getDefault()).format(cal.time)
            val dayNum = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
            Triple(dayName, dayNum, dateLabel)
        }
    }
    var selectedDayIndex by remember { mutableStateOf(0) }

    // Motion clips and tracking states
    var activeClipUrl by remember { mutableStateOf<String?>(null) }
    var activeClipId by remember { mutableStateOf<Int?>(null) }

    val motionClips = remember(activeIdx) {
        when (activeIdx) {
            0 -> listOf(
                MotionEventClip(
                    id = 1,
                    timeLabel = "10:14 AM",
                    duration = "45s clip",
                    startMinute = 614,
                    eventTitle = "Person Spotted",
                    desc = "Delivery carrier at front door drop-off zone.",
                    clipImageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400"
                ),
                MotionEventClip(
                    id = 2,
                    timeLabel = "02:30 PM",
                    duration = "1m 12s clip",
                    startMinute = 870,
                    eventTitle = "Package Left",
                    desc = "Box left on front porch deck.",
                    clipImageUrl = "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=400"
                ),
                MotionEventClip(
                    id = 3,
                    timeLabel = "06:15 PM",
                    duration = "30s clip",
                    startMinute = 1095,
                    eventTitle = "Unrecognized Motion",
                    desc = "Movement detected near porch light.",
                    clipImageUrl = "https://images.unsplash.com/photo-1507089947368-19c1da9775ae?w=400"
                )
            )
            1 -> listOf(
                MotionEventClip(
                    id = 4,
                    timeLabel = "08:12 AM",
                    duration = "2m 04s clip",
                    startMinute = 492,
                    eventTitle = "Vehicle Incoming",
                    desc = "Sedan entered private driveway coordinates.",
                    clipImageUrl = "https://images.unsplash.com/photo-1508962914676-134849a727f0?w=400"
                ),
                MotionEventClip(
                    id = 5,
                    timeLabel = "01:05 PM",
                    duration = "1m 30s clip",
                    startMinute = 785,
                    eventTitle = "Mail Truck Entry",
                    desc = "Mail vehicle parked adjacent to mailbox area.",
                    clipImageUrl = "https://images.unsplash.com/photo-1542435503-956c469947f6?w=400"
                )
            )
            2 -> listOf(
                MotionEventClip(
                    id = 6,
                    timeLabel = "11:45 AM",
                    duration = "22s clip",
                    startMinute = 705,
                    eventTitle = "Pet Movement",
                    desc = "Dog activity flagged near center sofa area.",
                    clipImageUrl = "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400"
                ),
                MotionEventClip(
                    id = 7,
                    timeLabel = "03:40 PM",
                    duration = "35s clip",
                    startMinute = 940,
                    eventTitle = "Resident Returned",
                    desc = "Individual detected in main corridor space.",
                    clipImageUrl = "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=400"
                )
            )
            else -> listOf(
                MotionEventClip(
                    id = 8,
                    timeLabel = "09:30 AM",
                    duration = "1m 15s clip",
                    startMinute = 570,
                    eventTitle = "Animal Highlight",
                    desc = "Wildlife spotted near perimeter wooden fence.",
                    clipImageUrl = "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?w=400"
                ),
                MotionEventClip(
                    id = 9,
                    timeLabel = "05:15 PM",
                    duration = "50s clip",
                    startMinute = 1035,
                    eventTitle = "Gate Motion",
                    desc = "Slight movement identified near back entrance latch.",
                    clipImageUrl = "https://images.unsplash.com/photo-1590073844006-33379778ae09?w=400"
                )
            )
        }
    }

    // Timeframes Presets (kept simple)
    val timeframes = remember {
        listOf("08:30 AM", "11:00 AM", "02:15 PM", "05:30 PM", "LIVE NOW")
    }
    var selectedTimeIndex by remember { mutableStateOf(4) }

    // Dynamic timeline scrubbing slider (minutes in day: 00:00 to 23:59 -> 0..1439)
    var isScrubbingActive by remember { mutableStateOf(false) }
    var scrubbedMinutes by remember { mutableStateOf(720) } // Default to 12:00 PM (720 min)

    val isPlaybackActive = remember(selectedDayIndex, isScrubbingActive, activeClipUrl) {
        selectedDayIndex != 0 || isScrubbingActive || activeClipUrl != null
    }

    // Helper to format minutes to 12 hour time
    val formatMinutesToTime: (Int) -> String = remember {
        { minutes ->
            val h = minutes / 60
            val m = minutes % 60
            val ampm = if (h >= 12) "PM" else "AM"
            val displayH = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format("%02d:%02d %s", displayH, m, ampm)
        }
    }

    val currentDisplayTime = remember(selectedTimeIndex, isScrubbingActive, scrubbedMinutes) {
        if (isScrubbingActive) {
            formatMinutesToTime(scrubbedMinutes)
        } else {
            timeframes[selectedTimeIndex]
        }
    }

    // Determine target feed image
    val displayImageUrl = remember(activeIdx, selectedDayIndex, selectedTimeIndex, activeClipUrl, scrubbedMinutes, dbDevices) {
        if (activeClipUrl != null) {
            activeClipUrl
        } else if (selectedDayIndex == 0 && !isScrubbingActive) {
            activeCamera.imageUrl
        } else {
            val cycle = (scrubbedMinutes / 120) % 4
            when (activeIdx) {
                0 -> {
                    when (cycle) {
                        0 -> "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400"
                        1 -> "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=400"
                        2 -> "https://images.unsplash.com/photo-1558036117-15d82a90b9b1?w=400"
                        else -> "https://images.unsplash.com/photo-1507089947368-19c1da9775ae?w=400"
                    }
                }
                1 -> {
                    when (cycle) {
                        0 -> "https://images.unsplash.com/photo-1508962914676-134849a727f0?w=400"
                        1 -> "https://images.unsplash.com/photo-1542435503-956c469947f6?w=400"
                        2 -> "https://images.unsplash.com/photo-1521207418485-99c705420785?w=400"
                        else -> "https://images.unsplash.com/photo-1558002038-1055907df827?w=400"
                    }
                }
                2 -> {
                    when (cycle) {
                        0 -> "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?w=400"
                        1 -> "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=400"
                        2 -> "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=400"
                        else -> "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=400"
                    }
                }
                else -> {
                    when (cycle) {
                        0 -> "https://images.unsplash.com/photo-1582268611958-ebfd161ef9cf?w=400"
                        1 -> "https://images.unsplash.com/photo-1590073844006-33379778ae09?w=400"
                        2 -> "https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=400"
                        else -> "https://images.unsplash.com/photo-1576013551627-0cc20b96c2a7?w=400"
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SafeHavenBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Minimalist Top Navigation Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .background(SafeHavenSurfaceLow, RoundedCornerShape(12.dp))
                        .border(1.dp, SafeHavenSurfaceContainer, RoundedCornerShape(12.dp))
                        .testTag("zoom_page_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go Back",
                        tint = SafeHavenDarkBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "${activeCamera.name.uppercase()} ZOOM VIEWER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafeHavenDarkBlue,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Camera Feed horizontal selection row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SELECT VIDEO SOURCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafeHavenOutlineDark,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cameras.forEachIndexed { index, feed ->
                        val isSelected = activeIdx == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSelected) SafeHavenGreen else SafeHavenSurfaceLow,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else SafeHavenSurfaceContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.selectCamera(index)
                                    // Reset zoom on switch
                                    zoomScale = 1.0f
                                    panX = 0f
                                    panY = 0f
                                    activeClipUrl = null
                                    activeClipId = null
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = feed.name,
                                    tint = if (isSelected) Color.White else SafeHavenDarkBlue.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = feed.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SafeHavenDarkBlue,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Zoom View Area (with High fidelity Multi-touch controls)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenDarkBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .testTag("zoom_screen_viewport_card"),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenOutline)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).clipToBounds()) {
                    val zoomAnim by animateFloatAsState(targetValue = zoomScale, label = "ZoomAnim")
                    
                    AsyncImage(
                        model = displayImageUrl,
                        contentDescription = "Zoomable Viewport",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomAnim
                                scaleY = zoomAnim
                                translationX = panX
                                translationY = panY
                            }
                            .pointerInput(zoomScale) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (zoomScale > 1.0f) {
                                            zoomScale = 1.0f
                                            panX = 0f
                                            panY = 0f
                                        } else {
                                            zoomScale = 2.5f
                                        }
                                    }
                                )
                            }
                            .pointerInput(zoomScale) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    zoomScale = (zoomScale * zoom).coerceIn(1.0f, 4.0f)
                                    if (zoomScale > 1.0f) {
                                        panX += pan.x
                                        panY += pan.y
                                    } else {
                                        panX = 0f
                                        panY = 0f
                                    }
                                }
                            }
                    )

                    // Zoom Label overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 26.dp, end = 10.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom Status",
                                tint = if (zoomScale > 1f) SafeHavenGreen else Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "SCALE: ${String.format("%.1fx", zoomScale)}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Video Source + Continuous Live Clock Header Overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isPlaybackActive) SafeHavenWarning else Color.Red, CircleShape)
                            )
                            Text(
                                text = activeCamera.name.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = if (isPlaybackActive) "ARCHIVAL REPLAY" else "LIVESTREAM ACTIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bottom Telemetry overlay bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "H.265 DECODER • HARDWARE ACCELERATED • 1080P",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isPlaybackActive) "SOURCE: SECURE FLASH" else "LIVE FPS: 30",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Calendar 7-Day History Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("zoom_page_history_card"),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "7 Days History",
                                tint = if (selectedDayIndex > 0) SafeHavenGreen else SafeHavenDarkBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("7-Day Video History Archive", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        }
                        Text(
                            text = if (selectedDayIndex == 0) "Today" else historyDays[selectedDayIndex].third,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDayIndex > 0) SafeHavenGreen else SafeHavenOutlineDark
                        )
                    }

                    // Days horizontal row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        historyDays.forEachIndexed { idx, dayInfo ->
                            val isSelected = selectedDayIndex == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) SafeHavenGreen else SafeHavenSurfaceLow,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else SafeHavenSurfaceContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedDayIndex = idx
                                        zoomScale = 1.0f
                                        panX = 0f
                                        panY = 0f
                                        if (idx > 0 && selectedTimeIndex == 4) {
                                            selectedTimeIndex = 1
                                        } else if (idx == 0) {
                                            selectedTimeIndex = 4
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayInfo.first,
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else SafeHavenOutlineDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dayInfo.second,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else SafeHavenDarkBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fine Slider Control Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("zoom_page_timeline_card"),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Timeline",
                                tint = if (isPlaybackActive) SafeHavenGreen else SafeHavenDarkBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Continuous Video Timeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (!isPlaybackActive) Color.Red.copy(alpha = 0.15f) else SafeHavenGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (!isPlaybackActive) "LIVE MONITORING" else "ARCHIVAL PLAYBACK",
                                color = if (!isPlaybackActive) Color.Red else SafeHavenGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Scrubber Continuous Scroll slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fine Slider Control",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeHavenOutlineDark
                            )
                        }

                        Slider(
                            value = scrubbedMinutes.toFloat(),
                            onValueChange = {
                                scrubbedMinutes = it.toInt()
                                isScrubbingActive = true
                                zoomScale = 1.0f
                                panX = 0f
                                panY = 0f
                                activeClipUrl = null
                                activeClipId = null
                            },
                            valueRange = 0f..1439f,
                            colors = SliderDefaults.colors(
                                thumbColor = SafeHavenGreen,
                                activeTrackColor = SafeHavenGreen,
                                inactiveTrackColor = SafeHavenOutline
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("zoom_page_continuous_timeline_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("12:00 AM", fontSize = 8.sp, color = SafeHavenOutlineDark)
                            Text("06:00 AM", fontSize = 8.sp, color = SafeHavenOutlineDark)
                            Text("12:00 PM", fontSize = 8.sp, color = SafeHavenOutlineDark)
                            Text("06:00 PM", fontSize = 8.sp, color = SafeHavenOutlineDark)
                            Text("11:59 PM", fontSize = 8.sp, color = SafeHavenOutlineDark)
                        }
                    }
                }
            }
        }

        // Recorded Motion Detection Clips & Time-Frames List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeHavenCardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("zoom_page_motion_clips_card"),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeHavenSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Motion clips",
                                tint = SafeHavenGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Motion Detection Video Clips", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeHavenDarkBlue)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(SafeHavenGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${motionClips.size} EVENTS",
                                color = SafeHavenGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "SELECT EVENT TO LOAD HIGH-RESOLUTION SCENE FRAME AND MATCH TIMELINE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeHavenOutlineDark,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        motionClips.forEach { clip ->
                            val isSelected = activeClipId == clip.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) SafeHavenGreen.copy(alpha = 0.08f) else SafeHavenSurfaceLow,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) SafeHavenGreen else SafeHavenSurfaceContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        activeClipUrl = clip.clipImageUrl
                                        activeClipId = clip.id
                                        scrubbedMinutes = clip.startMinute
                                        isScrubbingActive = true
                                        zoomScale = 1.0f
                                        panX = 0f
                                        panY = 0f
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left Image Thumbnail representative of Motion detection Video
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                ) {
                                    AsyncImage(
                                        model = clip.clipImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Middle Texts
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = clip.eventTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SafeHavenDarkBlue
                                        )
                                        Text(
                                            text = "• ${clip.duration}",
                                            fontSize = 9.sp,
                                            color = SafeHavenOutlineDark
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Text(
                                        text = clip.desc,
                                        fontSize = 10.sp,
                                        color = SafeHavenOutlineDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (isSelected) SafeHavenGreen else SafeHavenOutlineDark,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "TIMEFRAME: ${clip.timeLabel}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) SafeHavenGreen else SafeHavenOutlineDark,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Right status pill
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) SafeHavenGreen else SafeHavenSurfaceContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "ACTIVE" else "REPLAY",
                                        color = if (isSelected) Color.White else SafeHavenDarkBlue,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Add a padding space for the bottom nav bar
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
