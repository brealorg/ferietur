package app.ferietur.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * BRAND01: the Ferietur mark for PDF page headers.
 *
 * Drawn as real paths so it stays vector in the PDF; a VectorDrawable would be rasterised at the
 * size of its bounds. The coordinates are the 108-unit launcher foreground
 * (res/drawable/ic_launcher_foreground.xml): the same suitcase, the same luggage tag rotated 14°
 * about (67, 40), the same 0.85 safe-zone scale, shown through the 72-unit launcher window on a
 * rounded Oslo-yellow tile. Uses its own Paint so the writer's shared text paint is never touched.
 */
internal object PdfBrandMark {
    private val TILE = Color.rgb(0xF9, 0xC6, 0x6B)
    private val SUITCASE = Color.rgb(0x2A, 0x28, 0x59)
    private val TAG = Color.rgb(0x6F, 0xE9, 0xFF)
    private val TAG_STRING = Color.rgb(0xF8, 0xF0, 0xDD)

    /** Draws the mark as a [size] x [size] tile with its top-left corner at ([left], [top]). */
    fun draw(canvas: Canvas, left: Float, top: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val checkpoint = canvas.save()
        canvas.translate(left, top)
        canvas.scale(size / 72f, size / 72f)
        canvas.translate(-18f, -18f)

        paint.color = TILE
        canvas.drawRoundRect(RectF(18f, 18f, 90f, 90f), 16f, 16f, paint)

        // Same transform as the "mark" group in the drawable: scale about the pivot, then shift up.
        canvas.translate(0f, -3.5f)
        canvas.scale(0.85f, 0.85f, 54f, 57.5f)

        paint.color = SUITCASE
        canvas.drawPath(handle(), paint)
        canvas.drawPath(leftShell(), paint)
        canvas.drawPath(rightShell(), paint)
        canvas.drawPath(body(), paint)

        canvas.rotate(14f, 67f, 40f)
        paint.color = TAG_STRING
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.7f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(67f, 39.5f, 67f, 50f, paint)
        paint.style = Paint.Style.FILL
        paint.color = TAG
        canvas.drawPath(tagLabel(), paint)
        paint.color = SUITCASE
        canvas.drawCircle(67f, 54.4f, 1.9f, paint)

        canvas.restoreToCount(checkpoint)
    }

    private fun handle(): Path = Path().apply {
        moveTo(43f, 29f)
        lineTo(65f, 29f)
        cubicTo(68f, 29f, 70f, 31f, 70f, 34f)
        lineTo(70f, 40f)
        lineTo(64f, 40f)
        lineTo(64f, 36f)
        lineTo(44f, 36f)
        lineTo(44f, 40f)
        lineTo(38f, 40f)
        lineTo(38f, 34f)
        cubicTo(38f, 31f, 40f, 29f, 43f, 29f)
        close()
    }

    private fun leftShell(): Path = Path().apply {
        moveTo(26f, 42f)
        lineTo(30f, 42f)
        lineTo(30f, 82f)
        lineTo(26f, 82f)
        cubicTo(23.2f, 82f, 21f, 79.8f, 21f, 77f)
        lineTo(21f, 47f)
        cubicTo(21f, 44.2f, 23.2f, 42f, 26f, 42f)
        close()
    }

    private fun rightShell(): Path = Path().apply {
        moveTo(78f, 42f)
        lineTo(82f, 42f)
        cubicTo(84.8f, 42f, 87f, 44.2f, 87f, 47f)
        lineTo(87f, 77f)
        cubicTo(87f, 79.8f, 84.8f, 82f, 82f, 82f)
        lineTo(78f, 82f)
        lineTo(78f, 42f)
        close()
    }

    private fun body(): Path = Path().apply {
        moveTo(35f, 40f)
        lineTo(73f, 40f)
        cubicTo(74.7f, 40f, 76f, 41.3f, 76f, 43f)
        lineTo(76f, 79f)
        cubicTo(76f, 80.7f, 74.7f, 82f, 73f, 82f)
        lineTo(71f, 82f)
        lineTo(71f, 86f)
        lineTo(66f, 86f)
        lineTo(66f, 82f)
        lineTo(42f, 82f)
        lineTo(42f, 86f)
        lineTo(37f, 86f)
        lineTo(37f, 82f)
        lineTo(35f, 82f)
        cubicTo(33.3f, 82f, 32f, 80.7f, 32f, 79f)
        lineTo(32f, 43f)
        cubicTo(32f, 41.3f, 33.3f, 40f, 35f, 40f)
        close()
    }

    private fun tagLabel(): Path = Path().apply {
        moveTo(60.25f, 54f)
        lineTo(67f, 47.2f)
        lineTo(73.75f, 54f)
        lineTo(73.75f, 72.2f)
        cubicTo(73.75f, 73.6f, 72.6f, 74.7f, 71.25f, 74.7f)
        lineTo(62.75f, 74.7f)
        cubicTo(61.4f, 74.7f, 60.25f, 73.6f, 60.25f, 72.2f)
        close()
    }
}
