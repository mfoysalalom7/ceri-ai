package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ActivityHistoryScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.PersonasScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateTo = intent?.getStringExtra("navigate_to")

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()

                if (navigateTo == "notifications") {
                    viewModel.setSelectedTab(ScreenTab.NOTIFICATIONS)
                }

                CherryApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CherryApp(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()

    // BackHandler: Return to CHAT screen if on secondary tab
    if (selectedTab != ScreenTab.CHAT) {
        BackHandler {
            viewModel.setSelectedTab(ScreenTab.CHAT)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            ScreenTab.CHAT -> "Cherry AI 🍒"
                            ScreenTab.PERSONAS -> "Personas 🎭"
                            ScreenTab.PROFILE -> "User Profile 👤"
                            ScreenTab.ACTIVITY -> "Activity History 📜"
                            ScreenTab.NOTIFICATIONS -> "Notifications 🔔"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                actions = {
                    if (selectedTab != ScreenTab.NOTIFICATIONS) {
                        IconButton(
                            onClick = { viewModel.setSelectedTab(ScreenTab.NOTIFICATIONS) },
                            modifier = Modifier.testTag("top_bar_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "$unreadNotificationsCount",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                val navItems = listOf(
                    NavItemData(ScreenTab.CHAT, "Chat", Icons.AutoMirrored.Filled.Chat, "tab_chat"),
                    NavItemData(ScreenTab.PERSONAS, "Personas", Icons.Default.Psychology, "tab_personas"),
                    NavItemData(ScreenTab.ACTIVITY, "History", Icons.Default.History, "tab_activity"),
                    NavItemData(ScreenTab.NOTIFICATIONS, "Alerts", Icons.Default.Notifications, "tab_notifications", hasBadge = unreadNotificationsCount > 0),
                    NavItemData(ScreenTab.PROFILE, "Profile", Icons.Default.Person, "tab_profile")
                )

                navItems.forEach { item ->
                    val isSelected = selectedTab == item.tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTab(item.tab) },
                        icon = {
                            if (item.hasBadge) {
                                BadgedBox(badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("$unreadNotificationsCount", fontSize = 9.sp)
                                    }
                                }) {
                                    Icon(imageVector = item.icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(imageVector = item.icon, contentDescription = item.label)
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    ScreenTab.CHAT -> ChatScreen(viewModel = viewModel)
                    ScreenTab.PERSONAS -> PersonasScreen(viewModel = viewModel)
                    ScreenTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    ScreenTab.ACTIVITY -> ActivityHistoryScreen(viewModel = viewModel)
                    ScreenTab.NOTIFICATIONS -> NotificationCenterScreen(viewModel = viewModel)
                }
            }
        }
    }
}

private data class NavItemData(
    val tab: ScreenTab,
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val hasBadge: Boolean = false
)
