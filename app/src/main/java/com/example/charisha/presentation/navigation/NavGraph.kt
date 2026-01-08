package com.example.charisha.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.charisha.presentation.channel.ChannelEditScreen
import com.example.charisha.presentation.channel.ChannelListScreen
import com.example.charisha.presentation.chat.ChatScreen
import com.example.charisha.presentation.conversation.ConversationListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT,
        modifier = modifier
    ) {
        composable(route = Routes.CHAT) {
            ChatScreen(
                conversationId = null,
                onNavigateToChannels = { navController.navigate(Routes.CHANNEL_LIST) },
                onNavigateToConversations = { navController.navigate(Routes.CONVERSATION_LIST) }
            )
        }

        composable(
            route = Routes.CHAT_WITH_CONVERSATION_ID,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            ChatScreen(
                conversationId = conversationId,
                onNavigateToChannels = { navController.navigate(Routes.CHANNEL_LIST) },
                onNavigateToConversations = { navController.navigate(Routes.CONVERSATION_LIST) }
            )
        }

        composable(Routes.CHANNEL_LIST) {
            ChannelListScreen(
                onNavigateToEdit = { channelId -> navController.navigate(Routes.channelEdit(channelId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CHANNEL_EDIT,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId")
            val actualChannelId = if (channelId == "new") null else channelId
            ChannelEditScreen(
                channelId = actualChannelId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONVERSATION_LIST) {
            ConversationListScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Routes.chat(conversationId)) {
                        popUpTo(Routes.CHAT) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
