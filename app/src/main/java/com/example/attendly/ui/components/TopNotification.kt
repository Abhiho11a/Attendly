package com.example.attendly.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun TopNotificationHost(
    message: String?,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(100f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            if (message != null) {
                Surface(
                    modifier = Modifier
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
