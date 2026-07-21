@file:OptIn(ExperimentalAnimationApi::class)

package com.forgeidea.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
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
    onAnimated: (String) -> Unit = {},
    onRecall: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val shouldAnimate = isUser && !message.animated
    val slideOffset = remember { Animatable(if (shouldAnimate) 300f else 0f) }

    LaunchedEffect(message.id) {
        if (shouldAnimate) {
            slideOffset.animateTo(0f, animationSpec = tween(durationMillis = 400))
            onAnimated(message.id)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = slideOffset.value.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        var showMenu by remember { mutableStateOf(false) }
        val isError = !isUser && message.content.startsWith("❌")

        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { if (isUser) showMenu = true }
                    )
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
                    val reasoningAlpha by animateFloatAsState(
                        targetValue = if (showReasoning) 1f else 0f,
                        animationSpec = tween(durationMillis = 300),
                        label = "reasoningAlpha"
                    )

                    LaunchedEffect(expanded) {
                        if (expanded && !showReasoning) {
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
                            visible = expanded,
                            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(300))
                        ) {
                            Text(
                                text = message.reasoning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .alpha(reasoningAlpha)
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
                    LoadingState(
                        onRetry = onRetry,
                        modifier = Modifier.padding(top = if (message.reasoning.isNotBlank()) 8.dp else 0.dp)
                    )
                }

                if (!isUser && isError && !isLoading) {
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("重试", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (!isUser && (message.modelName.isNotBlank() || message.providerName.isNotBlank())) {
                    val meta = buildString {
                        if (message.modelName.isNotBlank()) append(message.modelName)
                        if (message.providerName.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(message.providerName)
                        }
                        if (message.durationMs > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("${message.durationMs}ms")
                        }
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("撤回") },
                    onClick = {
                        onRecall(message.id)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Thinking...", "Building...", "Working...", "Coding...", "Crafting...")
    var index by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var progress by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            progress = (progress + 1) % 11
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    LaunchedEffect(elapsedSeconds) {
        if (elapsedSeconds > 0 && elapsedSeconds % 15 == 0) {
            index = (index + 1) % labels.size
        }
    }

    val filled = "=".repeat(progress)
    val empty = " ".repeat(10 - progress)
    val arrow = if (progress < 10) ">" else "="

    Column(modifier = modifier) {
        if (elapsedSeconds >= 60) {
            TextButton(
                onClick = onRetry,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "思考可能太久了，重试吗？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            AnimatedContent(
                targetState = index,
                transitionSpec = { fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500)) },
                label = "loadingLabel"
            ) { targetIndex ->
                Text(
                    text = labels[targetIndex],
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "[$filled$arrow$empty]",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
