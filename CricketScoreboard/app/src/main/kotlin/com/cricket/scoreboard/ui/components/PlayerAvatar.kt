package com.cricket.scoreboard.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.scoreboard.ui.theme.PitchGold
import com.cricket.scoreboard.ui.theme.TurfGreenDark
import com.cricket.scoreboard.ui.theme.TurfGreenMedium
import java.io.File
import java.util.UUID
import kotlin.math.abs

fun savePlayerPhotoToInternalStorage(context: Context, uri: Uri, playerId: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val photosDir = File(context.filesDir, "player_photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val destFile = File(photosDir, "photo_${playerId}_${System.currentTimeMillis()}.jpg")
        destFile.outputStream().use { out ->
            inputStream.copyTo(out)
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun PlayerAvatar(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    borderColor: Color = TurfGreenMedium.copy(alpha = 0.4f),
    borderWidth: Dp = 1.dp,
    isEditable: Boolean = false,
    playerId: String = "",
    onPhotoChanged: ((String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = savePlayerPhotoToInternalStorage(context, uri, playerId.ifEmpty { UUID.randomUUID().toString() })
            if (savedPath != null) {
                onPhotoChanged?.invoke(savedPath)
            }
        }
    }

    val bitmap = remember(photoUri) {
        if (!photoUri.isNullOrEmpty()) {
            try {
                val file = File(photoUri)
                if (file.exists() && file.length() > 0) {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    val target = 256
                    var sample = 1
                    while (boundsOptions.outWidth / (sample * 2) >= target && boundsOptions.outHeight / (sample * 2) >= target) {
                        sample *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    // Avatar background color based on name hash
    val avatarBgColor = remember(name) {
        val colors = listOf(
            Color(0xFF2E7D32), // Green
            Color(0xFF1565C0), // Blue
            Color(0xFF6A1B9A), // Purple
            Color(0xFFC62828), // Red
            Color(0xFFE65100), // Orange
            Color(0xFF00838F), // Cyan
            Color(0xFF4527A0)  // Deep Purple
        )
        val idx = abs(name.hashCode()) % colors.size
        colors[idx]
    }

    val initial = remember(name) {
        name.trim().firstOrNull()?.uppercase() ?: "P"
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isEditable) {
                    Modifier.clickable {
                        if (photoUri != null) {
                            showMenu = true
                        } else {
                            launcher.launch("image/*")
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(borderWidth, borderColor, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(avatarBgColor)
                    .border(borderWidth, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.44f).sp
                )
            }
        }

        // Camera badge if editable
        if (isEditable) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.38f)
                    .clip(CircleShape)
                    .background(TurfGreenDark)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.22f)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("📷 Choose from Gallery") },
                    onClick = {
                        showMenu = false
                        launcher.launch("image/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text("🗑️ Remove Photo") },
                    onClick = {
                        showMenu = false
                        onPhotoChanged?.invoke(null)
                    }
                )
            }
        }
    }
}
