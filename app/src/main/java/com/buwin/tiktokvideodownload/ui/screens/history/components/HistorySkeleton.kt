package com.buwin.tiktokvideodownload.ui.screens.history.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.buwin.tiktokvideodownload.ui.components.download.shimmerEffect
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * Skeleton shimmer placeholder mimicking the grouped history card layout.
 */
@Composable
fun HistorySkeleton(
    contentBackdrop: LayerBackdrop,
    cardBackground: Color,
    cardBorderColor: Color,
    dividerColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .layerBackdrop(contentBackdrop)
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "skeleton_header_clearance") {
            Spacer(modifier = Modifier.height(116.dp))
        }

        // Section 1: Simulating today's downloads
        item(key = "skeleton_section_1") {
            HistoryDateGroupSkeleton(
                itemCount = 3,
                headerWidth = 140,
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                dividerColor = dividerColor,
                isDark = isDark
            )
        }

        // Section 2: Simulating yesterday's downloads
        item(key = "skeleton_section_2") {
            HistoryDateGroupSkeleton(
                itemCount = 2,
                headerWidth = 110,
                cardBackground = cardBackground,
                cardBorderColor = cardBorderColor,
                dividerColor = dividerColor,
                isDark = isDark
            )
        }

        item(key = "skeleton_footer_clearance") {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun HistoryDateGroupSkeleton(
    itemCount: Int,
    headerWidth: Int,
    cardBackground: Color,
    cardBorderColor: Color,
    dividerColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Shimmer date section header
        Box(
            modifier = Modifier
                .padding(start = 6.dp, bottom = 8.dp)
                .width(headerWidth.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(isDark)
        )

        // Card container with 26dp rounded corner matching HistoryDateGroupCard
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                repeat(itemCount) { index ->
                    HistoryItemRowSkeleton(isDark = isDark)
                    if (index < itemCount - 1) {
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

@Composable
private fun HistoryItemRowSkeleton(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail shimmer box
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .shimmerEffect(isDark)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title and metadata line shimmers
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(isDark)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.50f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(isDark)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmerEffect(isDark)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play button shimmer
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .shimmerEffect(isDark)
        )
    }
}
