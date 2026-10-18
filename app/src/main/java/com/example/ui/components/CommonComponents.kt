package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeColor: Color? = null
) {
    Card(
        modifier = modifier.testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (subtitle != null) {
                    Surface(
                        color = (badgeColor ?: iconTint).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = subtitle,
                            color = badgeColor ?: iconTint,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, text) = when (status.lowercase()) {
        "active", "ongoing", "p" -> Pair(WagesGreenLight, WagesGreenDark)
        "hd", "on hold", "half day" -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        "a", "inactive", "absent" -> Pair(AdvanceRedLight, AdvanceRed)
        "completed" -> Pair(OtBlueLight, OtBlue)
        else -> Pair(Slate200, Slate700)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = status,
            color = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
fun TradeIcon(
    trade: String,
    modifier: Modifier = Modifier
) {
    val lower = trade.lowercase()
    val (icon, tint) = when {
        lower.contains("carpenter") || lower.contains("လက်သမား") -> Pair(Icons.Default.Build, ConstructionGoldDark)
        lower.contains("mason") || lower.contains("ပန်းရံ") -> Pair(Icons.Default.Square, SafetyOrangeDark)
        lower.contains("electrician") || lower.contains("လျှပ်စစ်") -> Pair(Icons.Default.Bolt, Color(0xFFEAB308))
        lower.contains("plumber") || lower.contains("ရေပိုက်") -> Pair(Icons.Default.WaterDrop, Color(0xFF0284C7))
        lower.contains("painter") || lower.contains("ဆေးသုတ်") -> Pair(Icons.Default.FormatPaint, Color(0xFF8B5CF6))
        lower.contains("welder") || lower.contains("ဝရိန်") || lower.contains("ဝဟ်") -> Pair(Icons.Default.Handyman, Color(0xFF64748B))
        lower.contains("bar bender") || lower.contains("သံချည်") -> Pair(Icons.Default.Construction, Color(0xFF475569))
        lower.contains("tile") || lower.contains("ကြွေပြား") -> Pair(Icons.Default.GridOn, Color(0xFF0D9488))
        lower.contains("aluminium") || lower.contains("အလူမီနီယမ်") -> Pair(Icons.Default.Window, Color(0xFF2563EB))
        lower.contains("foreman") || lower.contains("ဖိုမင်") || lower.contains("ခေါင်းဆောင်") || lower.contains("အင်ဂျင်နီယာ") -> Pair(Icons.Default.Engineering, Color(0xFFDC2626))
        lower.contains("operator") || lower.contains("စက်မောင်း") -> Pair(Icons.Default.DirectionsCar, Color(0xFFD97706))
        lower.contains("concreter") || lower.contains("ကွန်ကရစ်") -> Pair(Icons.Default.Layers, Color(0xFF78716C))
        lower.contains("ceiling") || lower.contains("မျက်နှာကြက်") -> Pair(Icons.Default.Roofing, Color(0xFF0891B2))
        else -> Pair(Icons.Default.Person, Color(0xFF059669)) // Helper / အကူ / အလုပ်ကြမ်း
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = trade,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
