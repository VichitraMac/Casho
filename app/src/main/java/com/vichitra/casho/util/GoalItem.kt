package com.vichitra.casho.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vichitra.casho.ui.theme.PrimaryGreen
import com.vichitra.casho.ui.theme.TextGray
import java.util.Locale

@Composable
fun GoalItem(
    name: String,
    targetAmount: Double,
    currentAmount: Double,
    isCompleted: Boolean = false
) {
    val progress = if (targetAmount > 0) (currentAmount / targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val remaining = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val borderColor = if (isCompleted) PrimaryGreen else Color.Transparent
    val backgroundColor = if (isCompleted) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isCompleted) Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(if (isCompleted) 12.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PAID", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = if (isCompleted) "Payment Completed" 
                          else "Remaining: ₹${String.format(Locale.getDefault(), "%,.0f", remaining)}",
                    color = if (isCompleted) PrimaryGreen else TextGray,
                    fontSize = 14.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCompleted) "100%" else "${(progress * 100).toInt()}%",
                    color = if (isCompleted) PrimaryGreen else Color(0xFF4FC3F7),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "of ₹${String.format(Locale.getDefault(), "%,.0f", targetAmount)}",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E1E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isCompleted) 1f else animatedProgress)
                    .fillMaxHeight()                    .clip(CircleShape)

                    .background(
                        if (isCompleted) SolidColor(PrimaryGreen)
                        else Brush.horizontalGradient(
                            colors = listOf(Color(0xFF4FC3F7), PrimaryGreen)
                        )
                    )
            )
        }
    }
}
