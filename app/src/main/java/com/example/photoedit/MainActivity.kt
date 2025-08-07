package com.example.photoedit

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.photoedit.core.navigation.FiltersScreenNavKey
import com.example.photoedit.core.navigation.GalleryScreenNavKey
import com.example.photoedit.core.navigation.NavigationRoot
import com.example.photoedit.presentation.Gallery.GalleryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val galleryViewModel: GalleryViewModel = hiltViewModel()

            val backStack = rememberNavBackStack(GalleryScreenNavKey)

            val selectedIndex by galleryViewModel.selectedIndex.collectAsStateWithLifecycle()
            val selectedImage by galleryViewModel.selectedImage.collectAsStateWithLifecycle()

            Scaffold(
                modifier = Modifier

                    .fillMaxSize(),
                topBar = {
                    TopBar(backStack, galleryViewModel)
                },
                containerColor = Color.White,
                bottomBar = {
                    if (selectedIndex != 0 && selectedIndex != 1 && selectedIndex != 2 && selectedIndex != 3 && selectedIndex != 4 ) {
                        BottomBar(backStack, galleryViewModel)


                    } else {

                    }
                }
            ) { innerPadding ->
                NavigationRoot(backStack, innerPadding, galleryViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(backStack: NavBackStack, galleryViewModel: GalleryViewModel) {
    val fullSizeImage by galleryViewModel.fullSizeImage.collectAsStateWithLifecycle()
    val selectedImage by galleryViewModel.selectedImage.collectAsStateWithLifecycle()

    CenterAlignedTopAppBar(
        modifier = Modifier
            .blur(
                if (fullSizeImage.toString() == "null") 0.dp else 10.dp
            ),
        title = {
            Text(
                when (backStack.lastOrNull()) {
                    is GalleryScreenNavKey -> "Галерея"
                    is FiltersScreenNavKey -> "Фильтры"
                    else -> {
                        ""
                    }
                }
            )
        },
        actions = {
            if (selectedImage != null) {
                IconButton(
                    onClick = {}
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_done_24),
                        null
                    )
                }
            }
        },
        navigationIcon = {
            if (selectedImage != null) {
                IconButton(
                    onClick = { backStack.removeLastOrNull() }
                ) {
                    Icon(
                        painterResource(R.drawable.baseline_arrow_back_24),
                        null
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(Color.White)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomBar(backStack: NavBackStack, galleryViewModel: GalleryViewModel) {
    val selectedKey by remember(backStack) {
        derivedStateOf { backStack.lastOrNull() }
    }
    val selectedImage by galleryViewModel.selectedImage.collectAsStateWithLifecycle()
    val fullSizeImage by galleryViewModel.fullSizeImage.collectAsStateWithLifecycle()
    val selectedIndex by galleryViewModel.selectedIndex.collectAsStateWithLifecycle()
    BackHandler(enabled = true) {
        backStack.removeLastOrNull()
        galleryViewModel.clearSelectedImage()
    }

    Box {

        FlexibleBottomAppBar(
            containerColor = Color.White,
            contentColor = Color(0xFF0C7EF0),
            expandedHeight = 80.dp,
            modifier = Modifier
                .blur(
                    if (fullSizeImage.toString() == "null") 0.dp else 10.dp
                )
        ) {

            AnimatedVisibility(
                visible = selectedImage != null ,
                enter = scaleIn(animationSpec = tween(500)),
                exit = scaleOut(animationSpec = tween(300))
            ) {




                    val items = listOf(
                        R.drawable.baseline_crop_24 to "Обрезка",
                        R.drawable.baseline_filter_24 to "Фильтры",
                        R.drawable.baseline_invert_colors_24 to "Цвет",
                        R.drawable.baseline_draw_24 to "Кисть",
                        R.drawable.baseline_layers_clear_24 to "Удалить фон"

                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                    ) {
                        items.forEachIndexed { index, (icon, text) ->
                            CustomIconButton(
                                icon = icon,
                                text = text,
                                isSelected = selectedIndex == index,
                                onClick = { galleryViewModel.addSelectedIndex(index) }
                            )
                        }
                    }
            }
            AnimatedVisibility(
                visible = selectedImage == null,
                enter = scaleIn(animationSpec = tween(500)),
                exit = scaleOut(animationSpec = tween(300))
            ) {
                Row {

                    NavigationBarItem(

                        onClick = {
                            backStack.add(GalleryScreenNavKey)
                        },
                        selected = selectedKey == GalleryScreenNavKey,
                        icon = {
                            Icon(
                                painterResource(R.drawable.baseline_image_24),
                                contentDescription = null
                            )
                        },
                        label = { Text("Галерея") },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(Color(0xFF0C7EF0))
                    )

                    NavigationBarItem(
                        onClick = {
                            if (selectedImage.toString().isEmpty()) {
                                backStack.add(FiltersScreenNavKey(""))
                            } else {
                                backStack.add(FiltersScreenNavKey(selectedImage.toString()))
                            }
                        },
                        selected = selectedKey is FiltersScreenNavKey,
                        icon = {
                            Icon(
                                painterResource(R.drawable.baseline_auto_fix_high_24),
                                contentDescription = null
                            )
                        },
                        label = { Text("Фильтры") },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(Color(0xFF0C7EF0))

                    )
                }
            }

            if (selectedImage == null) {
                HorizontalDivider()

            }
        }
    }
}


@Composable
fun CustomIconButton(
    icon: Int,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor = if (isSelected) Color.Blue else Color.Black

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tintColor
            )
        }
        Text(text, color = tintColor)
    }
}