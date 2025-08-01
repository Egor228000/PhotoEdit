package com.example.photoedit.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.navEntryDecorator
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.example.photoedit.presentation.Collage.CollageScreen
import com.example.photoedit.presentation.Filters.FiltersScreen
import com.example.photoedit.presentation.Gallery.GalleryScreen
import com.example.photoedit.presentation.Gallery.GalleryViewModel
import kotlinx.serialization.Serializable

@Serializable
data object GalleryScreenNavKey: NavKey

@Serializable
data class FiltersScreenNavKey(val uri: String?): NavKey

@Serializable
data object CollageScreenNavKey: NavKey

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavigationRoot(
    backStack: NavBackStack,
    paddingValues: PaddingValues,
    galleryViewModel: GalleryViewModel
) {
    val localNavSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope> =
        compositionLocalOf {
            throw IllegalStateException(
                "Unexpected access to LocalNavSharedTransitionScope. You must provide a " +
                        "SharedTransitionScope from a call to SharedTransitionLayout() or " +
                        "SharedTransitionScope()"
            )
        }

    val sharedEntryInSceneNavEntryDecorator = navEntryDecorator { entry ->
        with(localNavSharedTransitionScope.current) {
            Box(
                Modifier
                    .sharedElement(
                        rememberSharedContentState(entry.key),
                        animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                    ),
            ) {
                entry.content(entry.key)
            }
        }
    }
    val twoPaneStrategy = remember { TwoPaneSceneStrategy<Any>() }
    SharedTransitionLayout {
        CompositionLocalProvider(localNavSharedTransitionScope provides this) {
            NavDisplay(
                modifier = Modifier.padding(paddingValues),
                backStack = backStack,
                onBack = { keysToRemove -> repeat(keysToRemove) { backStack.removeLastOrNull() } },
                entryDecorators = listOf(
                    sharedEntryInSceneNavEntryDecorator,
                    rememberSceneSetupNavEntryDecorator(),
                    rememberSavedStateNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                sceneStrategy = twoPaneStrategy,
                entryProvider = entryProvider {

                    entry<GalleryScreenNavKey>(
                        metadata = TwoPaneScene.twoPane()

                    ) {
                        GalleryScreen(
                            galleryViewModel,
                            onNavigateFilters = { uri ->
                                backStack.add(FiltersScreenNavKey(uri))
                            }
                        )
                    }
                    entry<FiltersScreenNavKey>(

                    ) { uri ->
                        FiltersScreen(
                            galleryViewModel,
                            uri.uri,
                            onNavigateGallery = {backStack.add(GalleryScreenNavKey)}
                        )
                    }
                    entry<CollageScreenNavKey> {
                        CollageScreen(galleryViewModel)
                    }
                }
            )
        }
    }
}