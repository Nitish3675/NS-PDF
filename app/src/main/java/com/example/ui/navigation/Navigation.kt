package com.example.ui.navigation

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DocumentScannerScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PdfEditorScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolOperationsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.viewmodel.PdfViewModel
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun NsPdfApp(
    viewModel: PdfViewModel = viewModel()
) {
    val navController = rememberNavController()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var ocrTargetBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(operationMessage) {
        val msg = operationMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    if (isOnboardingCompleted) {
                        navController.navigate("main") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("onboarding") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    viewModel.completeOnboarding()
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainShellScreen(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onOpenPdf = { file ->
                    val encoded = URLEncoder.encode(file.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("reader/$encoded")
                },
                onEditPdf = { file ->
                    val encoded = URLEncoder.encode(file.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("editor/$encoded")
                },
                onOpenScanner = { navController.navigate("scanner") },
                onOpenOcr = {
                    ocrTargetBitmap = null
                    navController.navigate("ocr")
                },
                onOpenTool = { toolId ->
                    navController.navigate("tool_op/$toolId")
                },
                onRestartOnboarding = {
                    viewModel.resetOnboarding()
                    navController.navigate("onboarding")
                }
            )
        }

        composable(
            route = "reader/{filePath}",
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val file = File(decodedPath)

            PdfReaderScreen(
                file = file,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenEditor = { targetFile ->
                    val encoded = URLEncoder.encode(targetFile.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("editor/$encoded")
                }
            )
        }

        composable(
            route = "editor/{filePath}",
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8.toString())
            val file = File(decodedPath)

            PdfEditorScreen(
                file = file,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { savedFile ->
                    val encoded = URLEncoder.encode(savedFile.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("reader/$encoded") {
                        popUpTo("main")
                    }
                }
            )
        }

        composable("scanner") {
            DocumentScannerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPdfGenerated = { savedFile ->
                    val encoded = URLEncoder.encode(savedFile.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("reader/$encoded") {
                        popUpTo("main")
                    }
                },
                onRunOcr = { bitmap ->
                    ocrTargetBitmap = bitmap
                    navController.navigate("ocr")
                }
            )
        }

        composable("ocr") {
            OcrScreen(
                initialBitmap = ocrTargetBitmap,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenCreatedPdf = { newPdf ->
                    val encoded = URLEncoder.encode(newPdf.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("reader/$encoded") {
                        popUpTo("main")
                    }
                }
            )
        }

        composable(
            route = "tool_op/{toolType}",
            arguments = listOf(navArgument("toolType") { type = NavType.StringType })
        ) { backStackEntry ->
            val toolType = backStackEntry.arguments?.getString("toolType") ?: ""
            ToolOperationsScreen(
                toolType = toolType,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPdfGenerated = { generatedFile ->
                    val encoded = URLEncoder.encode(generatedFile.absolutePath, StandardCharsets.UTF_8.toString())
                    navController.navigate("reader/$encoded") {
                        popUpTo("main")
                    }
                }
            )
        }
    }
}

@Composable
fun MainShellScreen(
    viewModel: PdfViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenPdf: (File) -> Unit,
    onEditPdf: (File) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenOcr: () -> Unit,
    onOpenTool: (String) -> Unit,
    onRestartOnboarding: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        NavTabItem("Home", Icons.Default.Home, "nav_tab_home"),
        NavTabItem("Files", Icons.Default.Folder, "nav_tab_files"),
        NavTabItem("Tools", Icons.Default.Build, "nav_tab_tools"),
        NavTabItem("Settings", Icons.Default.Settings, "nav_tab_settings")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                tabs.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(imageVector = item.icon, contentDescription = item.title)
                        },
                        label = { Text(item.title) },
                        modifier = Modifier.testTag(item.testTag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRoute = { route ->
                        when (route) {
                            "tools" -> selectedTab = 2
                            else -> onOpenTool(route)
                        }
                    },
                    onOpenPdf = onOpenPdf,
                    onOpenScanner = onOpenScanner,
                    onOpenOcr = onOpenOcr
                )
                1 -> FilesScreen(
                    viewModel = viewModel,
                    onOpenPdf = onOpenPdf,
                    onEditPdf = onEditPdf
                )
                2 -> ToolsScreen(
                    onToolSelected = { toolId ->
                        when (toolId) {
                            "scanner" -> onOpenScanner()
                            "ocr" -> onOpenOcr()
                            else -> onOpenTool(toolId)
                        }
                    }
                )
                3 -> SettingsScreen(
                    viewModel = viewModel,
                    onRestartOnboarding = onRestartOnboarding
                )
            }
        }
    }
}
