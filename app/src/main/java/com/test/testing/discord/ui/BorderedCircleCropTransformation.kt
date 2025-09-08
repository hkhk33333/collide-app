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

// A custom Coil transformation that crops a Bitmap into a circle and adds a border.
class BorderedCircleCropTransformation(
    private val borderWidth: Float = 8f, // Increased border for better visuals
    private val borderColor: Int = android.graphics.Color.WHITE,
) : Transformation {
    override val cacheKey: String = "${javaClass.name}-$borderWidth-$borderColor"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val minSize = min(input.width, input.height)
        val output = createBitmap(minSize, minSize, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        output.applyCanvas {
            // 1. Draw the white border as a solid circle.
            paint.color = borderColor
            val radius = minSize / 2f
            drawCircle(radius, radius, radius, paint)

            // 2. Prepare the avatar image. We'll save the canvas state,
            // create a circular mask, and then draw the image using the mask.
            val saved = saveLayer(null, null)

            // 2a. Carve out the inner circle (the destination for the avatar).
            // This circle will act as the mask.
            val imageRadius = radius - borderWidth
            drawCircle(radius, radius, imageRadius, paint)

            // 2b. Set the transfer mode. SRC_IN means "draw the source, but only
            // where the destination already has pixels". This effectively pours
            // the avatar image into the circle we just drew.
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            // 2c. Draw the source bitmap, cropping it from the center to avoid distortion.
            val srcRect =
                Rect(
                    (input.width - minSize) / 2,
                    (input.height - minSize) / 2,
                    (input.width + minSize) / 2,
                    (input.height + minSize) / 2,
                )
            val dstRect = RectF(0f, 0f, minSize.toFloat(), minSize.toFloat())
            drawBitmap(input, srcRect, dstRect, paint)

            // 3. Restore the canvas to apply the masked image and clean up.
            paint.xfermode = null
            restoreToCount(saved)
        }

        return output
    }
}
