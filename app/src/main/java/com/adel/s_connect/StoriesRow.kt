package com.adel.s_connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StoriesRow(
    stories: List<UserStories>,
    myUserId: String?,
    onStoryClick: (UserStories) -> Unit,
    onAddStoryClick: () -> Unit
) {
    if (stories.isEmpty() && myUserId == null) return

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val myStories = stories.firstOrNull { it.user.id == myUserId }
            if (myStories != null) {
                StoryCircle(
                    userStories = myStories,
                    isMine = true,
                    onClick = { onStoryClick(myStories) }
                )
            } else {
                AddStoryCircle(onClick = onAddStoryClick)
            }
        }

        items(stories.filter { it.user.id != myUserId }) { userStories ->
            StoryCircle(
                userStories = userStories,
                isMine = false,
                onClick = { onStoryClick(userStories) }
            )
        }
    }
}

@Composable
private fun StoryCircle(
    userStories: UserStories,
    isMine: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        val borderBrush = if (userStories.hasUnseen) {
            Brush.sweepGradient(
                listOf(
                    Color(0xFF7C3AED),
                    Color(0xFFEC4899),
                    Color(0xFFF59E0B),
                    Color(0xFF7C3AED)
                )
            )
        } else {
            Brush.sweepGradient(listOf(Color.Gray, Color.Gray))
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(width = 3.dp, brush = borderBrush, shape = CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!userStories.user.avatar_url.isNullOrEmpty()) {
                AsyncImage(
                    model = userStories.user.avatar_url,
                    contentDescription = userStories.user.username,
                    modifier = Modifier.size(58.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(58.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userStories.user.username.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = if (isMine) "Вы" else userStories.user.username,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isMine) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun AddStoryCircle(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "＋",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Добавить",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}