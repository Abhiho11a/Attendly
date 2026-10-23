package com.example.attendly.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(targetValue: Float = 1000f): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation, y = translateAnimation)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    width: Dp = Dp.Unspecified,
    cornerRadius: Dp = 8.dp
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .run { if (width != Dp.Unspecified) width(width) else fillMaxWidth() }
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 96.dp
) {
    val brush = shimmerBrush()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
    }
}
