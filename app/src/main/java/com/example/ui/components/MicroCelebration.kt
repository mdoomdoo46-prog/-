package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald800
import com.example.ui.theme.WarmGold

@Composable
fun MicroCelebrationBanner(
    visible: Boolean,
    title: String = "ما شاء الله 🤍",
    subtitle: String = "أحسنت، خطوة مباركة في طاعة الله",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Emerald800
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✨",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        color = WarmGold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "✨",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
