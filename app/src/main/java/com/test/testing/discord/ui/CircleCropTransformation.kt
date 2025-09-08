package com.test.testing.discord.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation

// A custom Coil transformation that crops a Bitmap into a circle.
class CircleCropTransformation : Transformation {
    override val cacheKey: String = javaClass.name

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val minSize = minOf(input.width, input.height)
        val radius = minSize / 2f
        val output = createBitmap(minSize, minSize)
        output.applyCanvas {
            drawCircle(radius, radius, radius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            drawBitmap(input, radius - input.width / 2f, radius - input.height / 2f, paint)
        }

        return output
    }
}
