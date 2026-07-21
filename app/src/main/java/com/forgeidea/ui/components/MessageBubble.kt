package com.forgeidea.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.forgeidea.domain.model.ChatRole
import com.forgeidea.domain.model.Message
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.delay

@Composable
fun MessageBubble(
    message: Message,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    var hasAnimated by rememberSaveable(message.id) { mutableStateOf(false) }
    val slideOffset = remember { Animatable(if (isUser && !hasAnimated) 300f else 0f) }

    LaunchedEffect(message.id) {
        if (isUser && !hasAnimated) {
            slideOffset.animateTo(0f, animationSpec = tween(durationMillis = 400))
            hasAnimated = true
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = slideOffset.value.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .animateContentSize(animationSpec = tween(durationMillis = 300))
                .padding(12.dp)
        ) {
            if (!isUser) {
                Text(
                    text = "Agent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (message.reasoning.isNotBlank()) {
                var expanded by rememberSaveable(message.id) { mutableStateOf(false) }
                var showReasoning by rememberSaveable(message.id) { mutableStateOf(false) }

                LaunchedEffect(expanded) {
                    if (expanded && !showReasoning) {
                        showReasoning = false
                        delay(300)
                        showReasoning = true
                    } else if (!expanded) {
                        showReasoning = false
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .animateContentSize(animationSpec = tween(durationMillis = 300))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "思考过程",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { expanded = !expanded },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (expanded) "收起" else "展开",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = showReasoning,
                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                    ) {
                        Text(
                            text = message.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (message.content.isNotBlank()) {
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = if (message.reasoning.isNotBlank()) 8.dp else 0.dp)
                    )
                } else {
                    Markdown(
                        content = message.content,
                        modifier = Modifier.padding(top = if (message.reasoning.isNotBlank()) 8.dp else 0.dp)
                    )
                }
            } else if (!isUser && isLoading) {
                NpmProgress(
                    modifier = Modifier.padding(top = if (message.reasoning.isNotBlank()) 8.dp else 0.dp)
                )
            }
        }
    }
}

@Composable
private fun NpmProgress(modifier: Modifier = Modifier) {
    var progress by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            progress = (progress + 1) % 11
        }
    }
    val filled = "=".repeat(progress)
    val empty = " ".repeat(10 - progress)
    val arrow = if (progress < 10) ">" else "="
    Text(
        text = "[$filled$arrow$empty] 请求中...",
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
