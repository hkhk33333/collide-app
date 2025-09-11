package com.test.testing.discord.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.min

class BorderedCircleCropTransformation(
    private val borderWidth: Float = 8f,
    private val borderColor: Int = android.graphics.Color.WHITE,
    private val shadowEnabled: Boolean = true,
    private val shadowRadius: Float = 4f,
    private val shadowDx: Float = 1f,
    private val shadowDy: Float = 2f,
    private val shadowColor: Int = android.graphics.Color.argb(100, 0, 0, 0),
) : Transformation {
    override val cacheKey: String =
        "${javaClass.name}-$borderWidth-$borderColor-$shadowEnabled-$shadowRadius-$shadowDx-$shadowDy-$shadowColor"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val minSize = min(input.width, input.height)
        val shadowPadding = if (shadowEnabled) shadowRadius * 2 else 0f
        val totalSize = (minSize + shadowPadding).toInt()
        val output = createBitmap(totalSize, totalSize, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        output.applyCanvas {
            val centerX = totalSize / 2f
            val centerY = totalSize / 2f
            val radius = minSize / 2f

            // 1. Draw the drop shadow (if enabled)
            if (shadowEnabled) {
                paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
                paint.color = android.graphics.Color.BLACK
                drawCircle(centerX + shadowDx, centerY + shadowDy, radius, paint)
                paint.clearShadowLayer()
            }

            // 2. Draw the white border as a solid circle.
            paint.color = borderColor
            drawCircle(centerX, centerY, radius, paint)

            // 3. Prepare the avatar image. We'll save the canvas state,
            // create a circular mask, and then draw the image using the mask.
            val saved = saveLayer(null, null)

            // 3a. Carve out the inner circle (the destination for the avatar).
            // This circle will act as the mask.
            val imageRadius = radius - borderWidth
            drawCircle(centerX, centerY, imageRadius, paint)

            // 3b. Set the transfer mode. SRC_IN means "draw the source, but only
            // where the destination already has pixels". This effectively pours
            // the avatar image into the circle we just drew.
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            // 3c. Draw the source bitmap, cropping it from the center to avoid distortion.
            val srcRect =
                Rect(
                    (input.width - minSize) / 2,
                    (input.height - minSize) / 2,
                    (input.width + minSize) / 2,
                    (input.height + minSize) / 2,
                )
            val dstRect =
                RectF(
                    centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius,
                )
            drawBitmap(input, srcRect, dstRect, paint)

            // 4. Restore the canvas to apply the masked image and clean up.
            paint.xfermode = null
            restoreToCount(saved)
        }

        return output
    }
}
