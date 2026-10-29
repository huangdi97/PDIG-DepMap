package com.pdig.uivnext.ui.r9

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.VTestIds
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** A deliberately small consumer-facing gallery: choose one picture, save, finished. */
internal const val R10_DEFAULT_ART = "original"
internal val R10_ART_CHOICES = listOf(
    R10_DEFAULT_ART to "原卡面",
    "ocean" to "海洋",
    "sky" to "云蓝",
    "coral" to "霞光",
    "night" to "星夜",
)

internal fun selectedCardArt(profile: PresentationProfile?): String =
    if (profile?.backgroundKind == "local-image") "local-image"
    else if (profile?.backgroundKind == "r10-art") profile.backgroundValue
    else R10_DEFAULT_ART

internal fun r10ArtProfile(profile: PresentationProfile, art: String) = profile.copy(
    backgroundKind = if(art == R10_DEFAULT_ART) "preset" else "r10-art",
    backgroundValue = if(art == R10_DEFAULT_ART) profile.themeId else art,
 )

private const val MAX_IMPORT_BYTES = 12 * 1024 * 1024
private const val ART_FOLDER = "pdig_r10_card_art"
private val safeArtName = Regex("^card-[a-f0-9-]{36}\\.jpg$")

/**
 * Pick image from Android's content picker, sample down to <=2048 px, store an
 * app-private JPEG. Never persist an arbitrary content:// URI, remote URL, or
 * unsanitized filesystem path in PresentationProfile.
 */
internal fun importCardArt(context: Context, uri: Uri): String? = runCatching {
    require(uri.scheme == "content") { "Only Android-picked content URI accepted" }
    val stream = context.contentResolver.openInputStream(uri) ?: error("Unreadable selected image")
    val buffer = ByteArrayOutputStream()
    stream.use { input ->
        val chunk = ByteArray(8192)
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            if (n == 0) continue
            require(buffer.size() + n <= MAX_IMPORT_BYTES) { "Selected image exceeds 12MB" }
            buffer.write(chunk, 0, n)
        }
    }
    val bytes = buffer.toByteArray()
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth in 1..20000 && bounds.outHeight in 1..20000) {
        "Selected content is not a supported image"
    }
    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: error("Cannot decode selected image")
    val folder = File(context.filesDir, ART_FOLDER)
    require(folder.isDirectory || folder.mkdirs()) { "Cannot create private image folder" }
    val name = "card-${UUID.randomUUID()}.jpg"
    val output = File(folder, name)
    output.outputStream().use { streamOut ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, streamOut)) error("Image conversion failed")
    }
    bitmap.recycle()
    name
}.getOrNull()

internal fun loadCardArt(context: Context, name: String): Bitmap? {
    if (!safeArtName.matches(name)) return null
    val file = File(File(context.filesDir, ART_FOLDER), name)
    if (!file.isFile || file.length() > MAX_IMPORT_BYTES) return null
    return BitmapFactory.decodeFile(file.absolutePath)
}

/** The selected card picture is a local presentation layer; no card data is rewritten. */
@Composable
internal fun R10CardFace(
    card: UiVNextCard,
    privacyMask: Boolean,
    profile: PresentationProfile?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val art = selectedCardArt(profile)
    if (art == R10_DEFAULT_ART) {
        R9BankCardFace(card, privacyMask, modifier = modifier, compact = compact)
        return
    }
    val ctx = LocalContext.current
    val bitmap = remember(profile?.backgroundValue, art) {
        if (art == "local-image") loadCardArt(ctx, profile?.backgroundValue.orEmpty()) else null
    }
    val theme = when(art) {
        "ocean" -> listOf(Color(0xFF0580C5), Color(0xFF062C6A), Color(0xFF1EB8D3))
        "sky" -> listOf(Color(0xFF8FC9F7), Color(0xFF4478D9), Color(0xFFE2F4FF))
        "coral" -> listOf(Color(0xFFF2A69A), Color(0xFFD85489), Color(0xFF5F3979))
        "night" -> listOf(Color(0xFF192C67), Color(0xFF090E29), Color(0xFF414DB4))
        else -> listOf(Color(0xFF2D75CE), Color(0xFF0F2452), Color(0xFF79B8EC))
    }
    Surface(
        modifier = modifier.clip(RoundedCornerShape(if(compact) 11.dp else 19.dp))
            .testTag(VTestIds.CARD_FACE),
        color = theme[1],
        shape = RoundedCornerShape(if(compact) 11.dp else 19.dp),
        shadowElevation = if(compact) 1.dp else 3.dp,
    ) {
        Box(Modifier.aspectRatio(1.586f).background(Brush.linearGradient(theme))) {
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "所选本地卡面图片",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    val focus = Offset(size.width * 0.9f, size.height * 0.15f)
                    drawCircle(Color.White.copy(alpha = .14f), radius = size.width * .6f,
                        center = focus, style = Stroke(width = 5.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = .1f), radius = size.width * .39f,
                        center = focus)
                }
            }
            // The image is the primary visual. Keep identifying information readable.
            Box(Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f),
                    Color.Transparent, Color.Black.copy(alpha = .5f))))) {
                Column(Modifier.fillMaxSize().padding(if(compact) 8.dp else 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(card.issuer, color = Color.White,
                            fontSize = if(compact) 9.sp else 15.sp,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("◉", color = Color.White,
                            fontSize = if(compact) 11.sp else 16.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if(privacyMask) "••••" else "••••  ${card.last4}",
                            color = Color.White, fontWeight = FontWeight.SemiBold,
                            fontSize = if(compact) 10.sp else 17.sp)
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(regionFlag(card.region) + " " + card.currency,
                                color = Color.White, fontSize = if(compact) 8.sp else 11.sp)
                            Text(card.network.uppercase(), color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = if(compact) 9.sp else 15.sp)
                        }
                    }
                }
            }
        }
    }
}
