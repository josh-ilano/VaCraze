import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.HelperViews.AuthState
import com.example.myapplication.HelperViews.AuthViewModel
import com.example.myapplication.HelperViews.SettingsViewModel
import com.example.myapplication.Pages.CalendarPage
import com.example.myapplication.Pages.LoginPage
import com.example.myapplication.Pages.NearbyPlacesPage
import com.example.myapplication.Pages.SettingsPage
import com.example.myapplication.Pages.SignupPage
import com.example.myapplication.Tools.MapWithSearchScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val authState by authViewModel.authState.observeAsState(AuthState.Loading)


    // Redirect to login on logout (Unauthenticated)
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true } // Clearing the entire backstack
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isLandscape && authState is AuthState.Authenticated) {
                BottomNavigationBar(
                    items = listOf(
                        NavItem("calendar", Icons.Filled.DateRange, "Calendar"),
                        NavItem("map", Icons.Filled.Place, "Map"),
                        NavItem("nearby_places", Icons.Filled.Star, "Places"),
                        NavItem("settings", Icons.Default.Settings, "Settings")
                    ),
                    currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route,
                    onItemClick = { route -> navigateToDestination(navController, route) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(0.dp)) {
            if (isLandscape && authState is AuthState.Authenticated) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SideNavigationBar(
                        items = listOf(
                            NavItem("calendar", Icons.Filled.DateRange, "Calendar"),
                            NavItem("map", Icons.Filled.Place, "Map"),
                            NavItem("nearby_places", Icons.Filled.Star, "Places"),
                            NavItem("settings", Icons.Default.Settings, "Settings")
                        ),
                        currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route,
                        onItemClick = { route -> navigateToDestination(navController, route) }
                    )

                    Box(modifier = Modifier.weight(0.1f)) {
                        NavigationHost(
                            navController = navController,
                            authViewModel = authViewModel,
                            settingsViewModel = settingsViewModel,
                        )
                    }
                }
            } else {
                NavigationHost(
                    navController = navController,
                    authViewModel = authViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}


@Composable
fun NavigationHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    settingsViewModel: SettingsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier.fillMaxSize()
    ) {
        composable("splash") {
            SplashRedirector(navController = navController, authViewModel = authViewModel)
        }
        composable("login") {
            LoginPage(
                modifier = Modifier,
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("signup") {
            SignupPage(
                modifier = Modifier,
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("calendar") { CalendarPage(settingsViewModel = settingsViewModel) }
        composable("map") { MapWithSearchScreen() }
        composable("nearby_places") { NearbyPlacesPage(navController) }
        composable("settings") { SettingsPage(settingsViewModel = settingsViewModel, authViewModel = authViewModel, navController = navController ) }
    }
}

@Composable
fun SplashRedirector(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.observeAsState(AuthState.Loading)

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("calendar") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            else -> {
            }
        }
    }

    // loading UI while checking auth
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


@Composable
fun BottomNavigationBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    BottomNavigation {
        items.forEach { item ->
            BottomNavigationItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onItemClick(item.route) }
            )
        }
    }
}

@Composable
fun SideNavigationBar(
    items: List<NavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primaryContainer),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val itemColor = if (isSelected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            }

            Column(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .clickable { onItemClick(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = itemColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = itemColor,
                    maxLines = 1,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// Helper function to handle navigation with single-top behavior
private fun navigateToDestination(navController: NavHostController, route: String) {
    navController.navigate(route) {
        // Pop up to the start destination of the graph to avoid back stack build-up
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Avoid multiple copies of the same destination when reselecting the same item
        launchSingleTop = true
        // Restore state when reselecting a previously selected item
        restoreState = true
    }
}

// Extended NavItem to include label
data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

// Dummy ViewModels
class AuthViewModel : androidx.lifecycle.ViewModel()
class LocationViewModel : androidx.lifecycle.ViewModel()

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PortraitPreview() {
    MyAppNavigation()
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun LandscapePreview() {
    MyAppNavigation()
}
