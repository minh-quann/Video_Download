package com.buwin.tiktokvideodownload.ui.screens.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buwin.tiktokvideodownload.data.model.DownloadRecord
import com.buwin.tiktokvideodownload.ui.screens.history.model.HistoryDateGroup
import com.kyant.backdrop.Backdrop

/**
 * Renders a calendar date section with the title placed outside the card,
 * and all day items housed inside a 26dp rounded corner card.
 */
@Composable
fun HistoryDateGroupCard(
    group: HistoryDateGroup,
    backdrop: Backdrop,
    onOpenRecord: (DownloadRecord) -> Unit,
    isDark: Boolean,
    cardBackground: Color,
    cardBorderColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Date title header outside the card
        Text(
            text = group.dateTitle,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )

        // Large 26dp rounded card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.items.forEachIndexed { index, item ->
                    HistoryItemRow(
                        record = item,
                        backdrop = backdrop,
                        onOpen = { onOpenRecord(item) },
                        isDark = isDark
                    )
                    if (index < group.items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp, end = 16.dp),
                            thickness = 0.6.dp,
                            color = dividerColor
                        )
                    }
                }
            }
        }
    }
}
