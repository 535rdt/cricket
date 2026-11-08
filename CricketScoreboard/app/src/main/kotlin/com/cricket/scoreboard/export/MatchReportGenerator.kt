package com.cricket.scoreboard.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.cricket.scoreboard.model.BatsmanScore
import com.cricket.scoreboard.model.BowlerScore
import com.cricket.scoreboard.model.Innings
import com.cricket.scoreboard.model.Match
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportImageFormat(
    val extension: String,
    val mimeType: String,
    val compressFormat: Bitmap.CompressFormat,
    val displayName: String
) {
    PNG("png", "image/png", Bitmap.CompressFormat.PNG, "PNG (Lossless HD)"),
    JPG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG, "JPG (Compressed)")
}

object MatchReportGenerator {

    private const val BITMAP_WIDTH = 1080

    /**
     * Generates a high-resolution, broadcast-quality graphic card for the match report.
     */
    fun generateReportBitmap(context: Context, match: Match): Bitmap {
        val inn1 = match.innings1
        val inn2 = match.innings2

        val inn1Batters = inn1.batsmen.filter { it.runs > 0 || it.balls > 0 || it.isBatting || it.isOut }.ifEmpty { inn1.batsmen.take(2) }
        val inn1Bowlers = inn1.bowlers.filter { it.legalBalls > 0 }.ifEmpty { inn1.bowlers.take(1) }

        val inn2Batters = inn2?.batsmen?.filter { it.runs > 0 || it.balls > 0 || it.isBatting || it.isOut }?.ifEmpty { inn2.batsmen.take(2) } ?: emptyList()
        val inn2Bowlers = inn2?.bowlers?.filter { it.legalBalls > 0 }?.ifEmpty { inn2.bowlers.take(1) } ?: emptyList()

        // Calculate dynamic height
        var totalHeight = 50f // top margin
        totalHeight += 210f   // Header banner
        totalHeight += 120f   // Result / MOTM banner
        totalHeight += 30f    // Spacing

        // Innings 1 card height
        val inn1CardHeight = 100f + 45f + (inn1Batters.size * 56f) + 30f + 45f + (inn1Bowlers.size * 48f) +
                (if (inn1.fallOfWickets.isNotEmpty()) 55f else 0f) + 40f
        totalHeight += inn1CardHeight + 30f

        // Innings 2 card height (if present)
        if (inn2 != null) {
            val inn2CardHeight = 110f + 45f + (inn2Batters.size * 56f) + 30f + 45f + (inn2Bowlers.size * 48f) +
                    (if (inn2.fallOfWickets.isNotEmpty()) 55f else 0f) + 40f
            totalHeight += inn2CardHeight + 30f
        }

        totalHeight += 100f // Footer
        totalHeight += 40f  // Bottom margin

        val bitmapHeight = totalHeight.toInt().coerceAtLeast(1200)
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Gradient (Dark Navy/Slate into Pitch Night)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, bitmapHeight.toFloat(),
                intArrayOf(
                    Color.rgb(11, 19, 36),   // Deep sports navy #0B1324
                    Color.rgb(15, 23, 42),   // Slate dark #0F172A
                    Color.rgb(6, 30, 20)     // Turf dark green tint #061E14
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, BITMAP_WIDTH.toFloat(), bitmapHeight.toFloat(), bgPaint)

        // Common Paints
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 34, 52) // #182234
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 213, 79) // Pitch Gold #FFD54F
        }
        val emeraldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 211, 153) // Emerald Green #34D399
        }
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(156, 163, 175) // Gray #9CA3AF
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 255, 255, 255)
            strokeWidth = 1.5f
        }

        var currentY = 50f

        // 2. Header Section
        val headerRect = RectF(40f, currentY, (BITMAP_WIDTH - 40).toFloat(), currentY + 190f)
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                headerRect.left, headerRect.top, headerRect.right, headerRect.bottom,
                Color.rgb(30, 41, 59), Color.rgb(15, 23, 42), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(headerRect, 20f, 20f, headerBg)
        canvas.drawRoundRect(headerRect, 20f, 20f, cardBorderPaint)

        // App badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 213, 79)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🏏 CRICKET SCOREBOARD  •  OFFICIAL MATCH REPORT", BITMAP_WIDTH / 2f, currentY + 40f, badgePaint)

        // Teams Match Title
        val teamTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val vsText = "${match.teamA.name.uppercase()}  vs  ${match.teamB.name.uppercase()}"
        canvas.drawText(ellipsize(vsText, teamTitlePaint, 960f), BITMAP_WIDTH / 2f, currentY + 95f, teamTitlePaint)

        // Match Info Subtitle
        val subInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(156, 163, 175)
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val tossWinnerName = if (match.tossResult.winnerTeamId == match.teamA.id) match.teamA.name else match.teamB.name
        val metaText = "${match.totalOvers} Overs  •  ${match.date}  •  Toss: $tossWinnerName elected to ${match.tossResult.choice.label}"
        canvas.drawText(ellipsize(metaText, subInfoPaint, 960f), BITMAP_WIDTH / 2f, currentY + 140f, subInfoPaint)

        currentY += 210f

        // 3. Result Banner / MOTM
        val resultRect = RectF(40f, currentY, (BITMAP_WIDTH - 40).toFloat(), currentY + 110f)
        val resultBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                resultRect.left, resultRect.top, resultRect.right, resultRect.bottom,
                if (match.isFinished) Color.rgb(20, 83, 45) else Color.rgb(30, 41, 59),
                if (match.isFinished) Color.rgb(6, 44, 25) else Color.rgb(24, 34, 52),
                Shader.TileMode.CLAMP
            )
        }
        val resultBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (match.isFinished) Color.rgb(52, 211, 153) else Color.rgb(255, 213, 79)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(resultRect, 20f, 20f, resultBg)
        canvas.drawRoundRect(resultRect, 20f, 20f, resultBorder)

        val resultTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val statusText = if (match.isFinished) "🏆 ${match.resultMessage ?: "Match Completed"}" else "⚡ Match In Progress (Innings ${match.currentInningsIndex})"
        canvas.drawText(ellipsize(statusText, resultTextPaint, 960f), BITMAP_WIDTH / 2f, currentY + 45f, resultTextPaint)

        if (match.manOfTheMatch != null) {
            val motmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 213, 79)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("⭐ Player of the Match: ${match.manOfTheMatch}", BITMAP_WIDTH / 2f, currentY + 85f, motmPaint)
        }

        currentY += 130f

        // 4. Innings 1 Card
        currentY = drawInningsCard(
            context = context,
            canvas = canvas,
            startY = currentY,
            inningsTitle = "1st Innings: ${inn1.battingTeamName}",
            innings = inn1,
            batters = inn1Batters,
            bowlers = inn1Bowlers,
            targetInfo = null,
            cardPaint = cardPaint,
            cardBorderPaint = cardBorderPaint,
            dividerPaint = dividerPaint
        )

        // 5. Innings 2 Card (if present)
        if (inn2 != null) {
            currentY += 25f
            currentY = drawInningsCard(
                context = context,
                canvas = canvas,
                startY = currentY,
                inningsTitle = "2nd Innings: ${inn2.battingTeamName}",
                innings = inn2,
                batters = inn2Batters,
                bowlers = inn2Bowlers,
                targetInfo = "Target: ${inn2.targetScore ?: (inn1.totalRuns + 1)}",
                cardPaint = cardPaint,
                cardBorderPaint = cardBorderPaint,
                dividerPaint = dividerPaint
            )
        }

        // 6. Footer Section
        currentY += 25f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 19f
            textAlign = Paint.Align.CENTER
        }
        val timeStamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Generated by Cricket Scoreboard App  •  $timeStamp", BITMAP_WIDTH / 2f, currentY + 25f, footerPaint)

        val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 17f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("https://github.com/535rdt/cricket", BITMAP_WIDTH / 2f, currentY + 52f, creditPaint)

        return bitmap
    }

    private fun drawInningsCard(
        context: Context,
        canvas: Canvas,
        startY: Float,
        inningsTitle: String,
        innings: Innings,
        batters: List<BatsmanScore>,
        bowlers: List<BowlerScore>,
        targetInfo: String?,
        cardPaint: Paint,
        cardBorderPaint: Paint,
        dividerPaint: Paint
    ): Float {
        var y = startY
        val cardHeight = 100f + 45f + (batters.size * 56f) + 30f + 45f + (bowlers.size * 48f) +
                (if (innings.fallOfWickets.isNotEmpty()) 55f else 0f) + 40f

        val cardRect = RectF(40f, y, (BITMAP_WIDTH - 40).toFloat(), y + cardHeight)
        canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)
        canvas.drawRoundRect(cardRect, 20f, 20f, cardBorderPaint)

        y += 45f

        // Innings Header Left (Team Name)
        val teamNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(ellipsize(inningsTitle, teamNamePaint, 520f), 70f, y, teamNamePaint)

        // Innings Header Right (Score & Overs)
        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 213, 79)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val scoreText = "${innings.totalRuns}/${innings.wickets}"
        canvas.drawText(scoreText, 1010f, y, scorePaint)

        val oversPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val oversScoreWidth = scorePaint.measureText(scoreText)
        val oversText = "(${innings.oversText}/${innings.maxOvers} Ov)  "
        canvas.drawText(oversText, 1010f - oversScoreWidth, y, oversPaint)

        y += 35f

        // Sub-header (Extras & CRR & Target)
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 19f
            textAlign = Paint.Align.LEFT
        }
        val extrasText = "Extras: ${innings.totalExtras} (w ${innings.wides}, nb ${innings.noBalls}, lb ${innings.legByes}, b ${innings.byes})   •   CRR: ${innings.formattedCrr}" +
                (if (targetInfo != null) "   •   $targetInfo" else "")
        canvas.drawText(ellipsize(extrasText, subPaint, 940f), 70f, y, subPaint)

        y += 25f
        canvas.drawLine(60f, y, 1020f, y, dividerPaint)
        y += 30f

        // Batting Table Header
        val colHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        colHeaderPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("BATSMAN", 70f, y, colHeaderPaint)

        colHeaderPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("R", 600f, y, colHeaderPaint)
        canvas.drawText("B", 690f, y, colHeaderPaint)
        canvas.drawText("4s", 780f, y, colHeaderPaint)
        canvas.drawText("6s", 870f, y, colHeaderPaint)
        canvas.drawText("SR", 1010f, y, colHeaderPaint)

        y += 15f
        canvas.drawLine(60f, y, 1020f, y, dividerPaint)
        y += 10f

        // Batting Rows
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val dismissPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val runValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 213, 79)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val statValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 21f
            textAlign = Paint.Align.RIGHT
        }
        val fourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(250, 204, 21) // Gold 4s
            textSize = 21f
            textAlign = Paint.Align.RIGHT
        }
        val sixPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 211, 153) // Green 6s
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        for (b in batters) {
            y += 28f
            // Player photo / avatar if available
            var textStartX = 70f
            val avatarBitmap = loadCircularBitmap(context, b.photoUri, 36)
            if (avatarBitmap != null) {
                canvas.drawBitmap(avatarBitmap, 70f, y - 22f, null)
                textStartX = 116f
            }

            // Name
            val displayName = b.name + (if (b.isBatting) " 🏏*" else "")
            canvas.drawText(ellipsize(displayName, namePaint, 480f - textStartX), textStartX, y, namePaint)

            // Runs, Balls, 4s, 6s, SR
            canvas.drawText(b.runs.toString(), 600f, y, runValPaint)
            canvas.drawText(b.balls.toString(), 690f, y, statValPaint)
            canvas.drawText(b.fours.toString(), 780f, y, fourPaint)
            canvas.drawText(b.sixes.toString(), 870f, y, sixPaint)
            canvas.drawText(b.formattedStrikeRate, 1010f, y, statValPaint)

            // Dismissal subtitle
            y += 22f
            canvas.drawText(ellipsize(b.dismissalText, dismissPaint, 480f - textStartX), textStartX, y, dismissPaint)
            y += 6f
        }

        y += 20f
        canvas.drawLine(60f, y, 1020f, y, dividerPaint)
        y += 30f

        // Bowling Table Header
        colHeaderPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("BOWLER", 70f, y, colHeaderPaint)

        colHeaderPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("O", 600f, y, colHeaderPaint)
        canvas.drawText("M", 690f, y, colHeaderPaint)
        canvas.drawText("R", 780f, y, colHeaderPaint)
        canvas.drawText("W", 870f, y, colHeaderPaint)
        canvas.drawText("ECON", 1010f, y, colHeaderPaint)

        y += 15f
        canvas.drawLine(60f, y, 1020f, y, dividerPaint)
        y += 10f

        // Bowling Rows
        val wktValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(52, 211, 153)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        for (bw in bowlers) {
            y += 34f
            var textStartX = 70f
            val avatarBitmap = loadCircularBitmap(context, bw.photoUri, 36)
            if (avatarBitmap != null) {
                canvas.drawBitmap(avatarBitmap, 70f, y - 22f, null)
                textStartX = 116f
            }

            canvas.drawText(ellipsize(bw.name, namePaint, 480f - textStartX), textStartX, y, namePaint)
            canvas.drawText(bw.oversText, 600f, y, statValPaint)
            canvas.drawText(bw.maidens.toString(), 690f, y, statValPaint)
            canvas.drawText(bw.runsConceded.toString(), 780f, y, statValPaint)
            canvas.drawText(bw.wickets.toString(), 870f, y, wktValPaint)
            canvas.drawText(bw.formattedEconomy, 1010f, y, statValPaint)
            y += 14f
        }

        // Fall of Wickets summary line
        if (innings.fallOfWickets.isNotEmpty()) {
            y += 20f
            canvas.drawLine(60f, y, 1020f, y, dividerPaint)
            y += 30f

            val fowTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 213, 79)
                textSize = 19f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Fall of Wickets: ", 70f, y, fowTitlePaint)

            val fowContentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                textSize = 18f
                textAlign = Paint.Align.LEFT
            }
            val titleW = fowTitlePaint.measureText("Fall of Wickets: ")
            val fowString = innings.fallOfWickets.joinToString(", ") { "${it.wicketNum}-${it.teamRuns} (${it.batsmanName}, ${it.overText} ov)" }
            canvas.drawText(ellipsize(fowString, fowContentPaint, 920f - titleW), 70f + titleW, y, fowContentPaint)
            y += 10f
        }

        return startY + cardHeight
    }

    /**
     * Loads and crops an image to a clean circular bitmap for player avatar display.
     */
    private fun loadCircularBitmap(context: Context, photoUriStr: String?, size: Int): Bitmap? {
        if (photoUriStr.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(photoUriStr)
            val inputStream = if (photoUriStr.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)
            } else {
                val file = File(photoUriStr)
                if (file.exists()) file.inputStream() else null
            } ?: return null

            val original = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            val scaled = Bitmap.createScaledBitmap(original, size, size, true)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
            canvas.drawOval(rect, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            output
        } catch (_: Exception) {
            null
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated…"
    }

    /**
     * Exports the match report bitmap as a PNG or JPG file into the app's cache directory.
     */
    fun exportReportFile(context: Context, match: Match, format: ReportImageFormat): File {
        val bitmap = generateReportBitmap(context, match)
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val cleanTeamA = match.teamA.name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val cleanTeamB = match.teamB.name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val fileName = "Match_Report_${cleanTeamA}_vs_${cleanTeamB}_${System.currentTimeMillis()}.${format.extension}"
        val destFile = File(imagesDir, fileName)

        FileOutputStream(destFile).use { out ->
            val quality = if (format == ReportImageFormat.PNG) 100 else 92
            bitmap.compress(format.compressFormat, quality, out)
        }
        return destFile
    }

    /**
     * Shares the match report image (PNG or JPG) using Android's system share intent.
     */
    fun shareReportImage(
        context: Context,
        match: Match,
        format: ReportImageFormat,
        textCaption: String? = null
    ) {
        try {
            val file = exportReportFile(context, match, format)
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                if (!textCaption.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, textCaption)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Share Match Report (${format.displayName})")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves the match report image (PNG or JPG) to the device's Pictures gallery folder.
     */
    fun saveReportToGallery(
        context: Context,
        match: Match,
        format: ReportImageFormat
    ): Boolean {
        return try {
            val bitmap = generateReportBitmap(context, match)
            val cleanTeamA = match.teamA.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val cleanTeamB = match.teamB.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "Cricket_Report_${cleanTeamA}_vs_${cleanTeamB}_${System.currentTimeMillis()}.${format.extension}"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CricketScoreboard")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        val quality = if (format == ReportImageFormat.PNG) 100 else 92
                        bitmap.compress(format.compressFormat, quality, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    Toast.makeText(context, "✅ Saved ${format.extension.uppercase()} report to Pictures/CricketScoreboard!", Toast.LENGTH_LONG).show()
                    true
                } else {
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val targetDir = File(picturesDir, "CricketScoreboard").apply { mkdirs() }
                val destFile = File(targetDir, fileName)
                FileOutputStream(destFile).use { out ->
                    val quality = if (format == ReportImageFormat.PNG) 100 else 92
                    bitmap.compress(format.compressFormat, quality, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(format.mimeType), null)
                Toast.makeText(context, "✅ Saved ${format.extension.uppercase()} report to Gallery!", Toast.LENGTH_LONG).show()
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
