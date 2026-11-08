package com.cricket.scoreboard.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cricket.scoreboard.export.MatchReportGenerator
import com.cricket.scoreboard.export.ReportImageFormat
import com.cricket.scoreboard.model.Match
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShareReportDialog(
    match: Match,
    onShareImage: (ReportImageFormat) -> Unit,
    onSaveToGallery: (ReportImageFormat) -> Unit,
    onShareText: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ReportImageFormat.PNG) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(match) {
        withContext(Dispatchers.Default) {
            try {
                val fullBmp = MatchReportGenerator.generateReportBitmap(context, match)
                val targetW = 540
                val scale = targetW.toFloat() / fullBmp.width
                val targetH = (fullBmp.height * scale).toInt()
                val thumb = Bitmap.createScaledBitmap(fullBmp, targetW, targetH, true)
                previewBitmap = thumb
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGenerating = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PitchGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = PitchGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Share Match Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Export broadcast scorecard graphic",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Format Selector (PNG vs JPG)
                Text(
                    text = "Select Image Format:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormatSelectCard(
                        title = "PNG Image",
                        subtitle = "Lossless HD • Crystal Clear",
                        badge = "Recommended",
                        isSelected = selectedFormat == ReportImageFormat.PNG,
                        onClick = { selectedFormat = ReportImageFormat.PNG },
                        modifier = Modifier.weight(1f)
                    )

                    FormatSelectCard(
                        title = "JPG Image",
                        subtitle = "Compressed • Smaller Size",
                        badge = "Compact",
                        isSelected = selectedFormat == ReportImageFormat.JPG,
                        onClick = { selectedFormat = ReportImageFormat.JPG },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Image Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 250.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A))
                        .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = PitchGold,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Generating card preview...",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    } else if (previewBitmap != null) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Match Report Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp),
                                contentScale = ContentScale.FillWidth
                            )
                            // Tag overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "1080px • ${selectedFormat.name}",
                                    fontSize = 11.sp,
                                    color = PitchGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Scorecard Graphic Ready",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action 1: Share Image
                Button(
                    onClick = { onShareImage(selectedFormat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurfGreenMedium),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share as ${selectedFormat.name} Image",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action 2: Save Image to Gallery
                OutlinedButton(
                    onClick = { onSaveToGallery(selectedFormat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save ${selectedFormat.name} to Device Gallery",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action 3: Share as Text
                TextButton(
                    onClick = onShareText,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share as Plain Text Instead",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatSelectCard(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) TurfGreenMedium else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) TurfGreenDark.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) TurfGreenMedium else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TurfGreenMedium,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) TurfGreenMedium.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) TurfGreenMedium else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
