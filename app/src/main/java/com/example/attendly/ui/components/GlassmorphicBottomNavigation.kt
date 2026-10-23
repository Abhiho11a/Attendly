package com.example.attendly.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.attendly.ui.navigation.Screen

@Composable
fun GlassmorphicBottomNavigation(
    items: List<Screen>,
    currentDestination: NavDestination?,
    onItemClick: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val navbarShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val borderBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f)
            )
        )

        // Opaque Dark Charcoal/Navy Surface matching Attendly theme
        val darkSurfaceColor = Color(0xFF181C2B)
        val topSheenColor = Color.White.copy(alpha = 0.06f)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = navbarShape,
                    ambientColor = Color.Black.copy(alpha = 0.20f),
                    spotColor = Color.Black.copy(alpha = 0.30f)
                ),
            shape = navbarShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, borderBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(navbarShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                darkSurfaceColor.copy(alpha = 0.98f),
                                darkSurfaceColor
                            )
                        )
                    )
            ) {
                // Subtle top highlight sheen for depth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(topSheenColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        GlassNavigationItem(
                            screen = screen,
                            isSelected = isSelected,
                            onClick = { onItemClick(screen) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassNavigationItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val activeContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
    val inactiveContainerColor = Color.Transparent

    val activeContentColor = MaterialTheme.colorScheme.primary
    val inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    val animatedContainerColor by animateColorAsState(
        targetValue = if (isSelected) activeContainerColor else inactiveContainerColor,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "navItemContainerColor"
    )

    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) activeContentColor else inactiveContentColor,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "navItemContentColor"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "navItemScale"
    )

    val itemShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 5.dp)
            .clip(itemShape)
            .background(animatedContainerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .clearAndSetSemantics {
                contentDescription = screen.title
                role = Role.Tab
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(animatedScale)
        ) {
            screen.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedContentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = screen.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = animatedContentColor
            )
        }
    }
}
