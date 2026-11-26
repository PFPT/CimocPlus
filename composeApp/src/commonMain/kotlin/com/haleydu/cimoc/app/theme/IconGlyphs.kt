package com.haleydu.cimoc.app.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun ink(block: PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }.build()
}

private fun seal(block: PathBuilder.() -> Unit): ImageVector = sealPaths(false, block)

private fun seal(first: PathBuilder.() -> Unit, second: PathBuilder.() -> Unit): ImageVector =
    sealPaths(false, first, second)

private fun sealEvenOdd(block: PathBuilder.() -> Unit): ImageVector = sealPaths(true, block)

private fun sealPaths(evenOdd: Boolean, vararg blocks: PathBuilder.() -> Unit): ImageVector {
    val builder = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    val fillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero
    for (block in blocks) {
        builder.path(fill = SolidColor(Color.Black), pathFillType = fillType, pathBuilder = block)
    }
    return builder.build()
}

private fun PathBuilder.chevronLeft() {
    moveTo(15f, 5f)
    lineTo(8f, 12f)
    lineTo(15f, 19f)
}

private fun PathBuilder.chevronRight() {
    moveTo(9f, 5f)
    lineTo(16f, 12f)
    lineTo(9f, 19f)
}

private fun PathBuilder.star() {
    moveTo(12f, 3.2f)
    lineTo(14.4f, 8.8f)
    lineTo(20.4f, 9.3f)
    lineTo(15.8f, 13.4f)
    lineTo(17.2f, 19.3f)
    lineTo(12f, 16.2f)
    lineTo(6.8f, 19.3f)
    lineTo(8.2f, 13.4f)
    lineTo(3.6f, 9.3f)
    lineTo(9.6f, 8.8f)
    close()
}

private fun PathBuilder.heart() {
    moveTo(12f, 19.5f)
    lineTo(5.2f, 12.2f)
    curveTo(3.4f, 10.2f, 3.6f, 7f, 6f, 5.6f)
    curveTo(7.8f, 4.6f, 10f, 5.1f, 12f, 7.2f)
    curveTo(14f, 5.1f, 16.2f, 4.6f, 18f, 5.6f)
    curveTo(20.4f, 7f, 20.6f, 10.2f, 18.8f, 12.2f)
    close()
}

internal fun inkGlyphs(): AppIconSet = AppIconSet(
    back = ink { chevronLeft() },
    forward = ink { chevronRight() },
    home = ink {
        moveTo(4f, 11f)
        lineTo(12f, 4.5f)
        lineTo(20f, 11f)
        moveTo(7f, 10f)
        lineTo(7f, 19f)
        lineTo(17f, 19f)
        lineTo(17f, 10f)
    },
    library = ink {
        moveTo(6f, 5f)
        lineTo(6f, 19f)
        lineTo(16f, 19f)
        curveTo(17.5f, 19f, 18.5f, 17.8f, 18.5f, 16.2f)
        lineTo(18.5f, 7f)
        curveTo(18.5f, 5.8f, 17.4f, 5f, 16.2f, 5f)
        close()
        moveTo(6f, 5f)
        curveTo(7.4f, 4.2f, 9.2f, 4.6f, 10.2f, 5.6f)
    },
    search = ink {
        moveTo(15.2f, 15.2f)
        lineTo(20f, 20f)
        moveTo(17f, 11f)
        arcTo(6f, 6f, 0f, true, true, 5f, 11f)
        arcTo(6f, 6f, 0f, true, true, 17f, 11f)
    },
    browser = ink {
        moveTo(20f, 12f)
        arcTo(8f, 8f, 0f, true, true, 4f, 12f)
        arcTo(8f, 8f, 0f, true, true, 20f, 12f)
        moveTo(4.5f, 12f)
        lineTo(19.5f, 12f)
        moveTo(12f, 4f)
        curveTo(9.5f, 7f, 9.5f, 17f, 12f, 20f)
        curveTo(14.5f, 17f, 14.5f, 7f, 12f, 4f)
    },
    sources = ink {
        moveTo(12f, 3f)
        lineTo(19f, 12f)
        lineTo(12f, 21f)
        lineTo(5f, 12f)
        close()
        moveTo(12f, 8f)
        lineTo(12f, 14f)
    },
    profile = ink {
        moveTo(16f, 9f)
        arcTo(4f, 4f, 0f, true, true, 8f, 9f)
        arcTo(4f, 4f, 0f, true, true, 16f, 9f)
        moveTo(5f, 20f)
        curveTo(6.2f, 16.8f, 8.6f, 15.2f, 12f, 15.2f)
        curveTo(15.4f, 15.2f, 17.8f, 16.8f, 19f, 20f)
    },
    star = ink { star() },
    starOutline = ink { star() },
    refresh = ink {
        moveTo(19f, 12f)
        arcTo(7f, 7f, 0f, true, true, 16.5f, 6.2f)
        moveTo(16f, 3.5f)
        lineTo(16.8f, 7.2f)
        lineTo(13.2f, 7.4f)
    },
    list = ink {
        moveTo(7f, 7f)
        lineTo(19f, 7f)
        moveTo(7f, 12f)
        lineTo(19f, 12f)
        moveTo(7f, 17f)
        lineTo(19f, 17f)
        moveTo(4.5f, 7f)
        lineTo(4.5f, 7.1f)
        moveTo(4.5f, 12f)
        lineTo(4.5f, 12.1f)
        moveTo(4.5f, 17f)
        lineTo(4.5f, 17.1f)
    },
    delete = ink {
        moveTo(8f, 8f)
        lineTo(9f, 19f)
        lineTo(15f, 19f)
        lineTo(16f, 8f)
        moveTo(6f, 8f)
        lineTo(18f, 8f)
        moveTo(10f, 8f)
        lineTo(10.6f, 5f)
        lineTo(13.4f, 5f)
        lineTo(14f, 8f)
    },
    favorite = ink { heart() },
    favoriteOutline = ink { heart() },
    swap = ink {
        moveTo(4f, 8f)
        lineTo(18f, 8f)
        lineTo(14.5f, 4.8f)
        moveTo(20f, 16f)
        lineTo(6f, 16f)
        lineTo(9.5f, 19.2f)
    },
    sort = ink {
        moveTo(8f, 5f)
        lineTo(8f, 19f)
        lineTo(4.8f, 15.5f)
        moveTo(16f, 19f)
        lineTo(16f, 5f)
        lineTo(19.2f, 8.5f)
    },
    pause = ink {
        moveTo(8f, 5f)
        lineTo(8f, 19f)
        moveTo(16f, 5f)
        lineTo(16f, 19f)
    },
    replay = ink {
        moveTo(8f, 7f)
        lineTo(4.5f, 7f)
        lineTo(4.5f, 3.5f)
        moveTo(5.2f, 8f)
        arcTo(7f, 7f, 0f, true, true, 7f, 18.5f)
    },
    play = ink {
        moveTo(8f, 5.5f)
        lineTo(18f, 12f)
        lineTo(8f, 18.5f)
        close()
    },
    pages = ink {
        moveTo(6f, 6f)
        lineTo(18f, 6f)
        lineTo(18f, 16f)
        lineTo(6f, 16f)
        close()
        moveTo(8f, 18.5f)
        lineTo(16f, 18.5f)
    },
    rotate = ink {
        moveTo(16f, 6f)
        arcTo(6.5f, 6.5f, 0f, true, true, 8f, 17.5f)
        moveTo(16f, 3f)
        lineTo(16f, 7.5f)
        lineTo(12f, 7.5f)
    },
    night = ink {
        moveTo(15f, 4.5f)
        curveTo(10f, 5f, 7f, 9f, 7.5f, 14f)
        curveTo(8f, 18f, 11.5f, 20.5f, 15.5f, 19.5f)
        curveTo(12f, 18.5f, 9.5f, 15.5f, 10f, 11.5f)
        curveTo(10.4f, 8.2f, 13f, 5.8f, 15f, 4.5f)
        close()
    },
    up = ink {
        moveTo(6f, 14.5f)
        lineTo(12f, 8.5f)
        lineTo(18f, 14.5f)
    },
    left = ink { chevronLeft() },
    right = ink { chevronRight() }
)

internal fun sealGlyphs(): AppIconSet = AppIconSet(
    back = seal {
        moveTo(15.5f, 4f)
        lineTo(7f, 12f)
        lineTo(15.5f, 20f)
        lineTo(15.5f, 15f)
        lineTo(11.5f, 12f)
        lineTo(15.5f, 9f)
        close()
    },
    forward = seal {
        moveTo(8.5f, 4f)
        lineTo(17f, 12f)
        lineTo(8.5f, 20f)
        lineTo(8.5f, 15f)
        lineTo(12.5f, 12f)
        lineTo(8.5f, 9f)
        close()
    },
    home = seal({
        moveTo(3f, 11.5f)
        lineTo(12f, 4f)
        lineTo(21f, 11.5f)
        lineTo(17.5f, 11.5f)
        lineTo(12f, 7.2f)
        lineTo(6.5f, 11.5f)
        close()
    }, {
        moveTo(7f, 12.5f)
        lineTo(17f, 12.5f)
        lineTo(17f, 20f)
        lineTo(7f, 20f)
        close()
    }),
    library = seal {
        moveTo(5f, 4f)
        lineTo(9f, 4f)
        lineTo(9f, 20f)
        lineTo(5f, 20f)
        close()
        moveTo(11f, 5.5f)
        lineTo(19f, 4f)
        lineTo(19f, 19f)
        lineTo(11f, 20f)
        close()
    },
    search = seal({
        moveTo(16.5f, 10.5f)
        arcTo(6f, 6f, 0f, true, true, 4.5f, 10.5f)
        arcTo(6f, 6f, 0f, true, true, 16.5f, 10.5f)
        close()
    }, {
        moveTo(14.6f, 14.2f)
        lineTo(20.2f, 20.4f)
        lineTo(17.8f, 20.4f)
        lineTo(13.2f, 15.4f)
        close()
    }),
    browser = seal({
        moveTo(21f, 12f)
        arcTo(9f, 9f, 0f, true, true, 3f, 12f)
        arcTo(9f, 9f, 0f, true, true, 21f, 12f)
        close()
    }, {
        moveTo(11f, 3.4f)
        lineTo(13f, 3.4f)
        lineTo(13f, 20.6f)
        lineTo(11f, 20.6f)
        close()
    }),
    sources = seal {
        moveTo(12f, 2.5f)
        lineTo(20.5f, 12f)
        lineTo(12f, 21.5f)
        lineTo(3.5f, 12f)
        close()
    },
    profile = seal({
        moveTo(16.2f, 7.7f)
        arcTo(4.2f, 4.2f, 0f, true, true, 7.8f, 7.7f)
        arcTo(4.2f, 4.2f, 0f, true, true, 16.2f, 7.7f)
        close()
    }, {
        moveTo(4.5f, 20.5f)
        curveTo(5.5f, 15.5f, 8f, 13.5f, 12f, 13.5f)
        curveTo(16f, 13.5f, 18.5f, 15.5f, 19.5f, 20.5f)
        close()
    }),
    star = seal { star() },
    starOutline = sealEvenOdd {
        moveTo(12f, 3.2f)
        lineTo(14.2f, 8.4f)
        lineTo(19.8f, 8.8f)
        lineTo(15.6f, 12.6f)
        lineTo(16.8f, 18.2f)
        lineTo(12f, 15.4f)
        lineTo(7.2f, 18.2f)
        lineTo(8.4f, 12.6f)
        lineTo(4.2f, 8.8f)
        lineTo(9.8f, 8.4f)
        close()
        moveTo(12f, 7.2f)
        lineTo(10.6f, 10.4f)
        lineTo(7.2f, 10.6f)
        lineTo(9.8f, 12.8f)
        lineTo(9f, 16.2f)
        lineTo(12f, 14.4f)
        lineTo(15f, 16.2f)
        lineTo(14.2f, 12.8f)
        lineTo(16.8f, 10.6f)
        lineTo(13.4f, 10.4f)
        close()
    },
    refresh = seal({
        moveTo(12f, 4f)
        arcTo(8f, 8f, 0f, true, true, 6f, 7f)
        lineTo(8.2f, 9.2f)
        arcTo(5.4f, 5.4f, 0f, true, false, 12f, 6.6f)
        close()
    }, {
        moveTo(5f, 3f)
        lineTo(9.5f, 3f)
        lineTo(9.5f, 8f)
        lineTo(7.2f, 6.2f)
        lineTo(5f, 6.2f)
        close()
    }),
    list = seal({
        moveTo(4f, 5.5f)
        lineTo(7f, 5.5f)
        lineTo(7f, 8.5f)
        lineTo(4f, 8.5f)
        close()
        moveTo(9f, 6f)
        lineTo(20f, 6f)
        lineTo(20f, 8f)
        lineTo(9f, 8f)
        close()
        moveTo(4f, 10.5f)
        lineTo(7f, 10.5f)
        lineTo(7f, 13.5f)
        lineTo(4f, 13.5f)
        close()
        moveTo(9f, 11f)
        lineTo(20f, 11f)
        lineTo(20f, 13f)
        lineTo(9f, 13f)
        close()
        moveTo(4f, 15.5f)
        lineTo(7f, 15.5f)
        lineTo(7f, 18.5f)
        lineTo(4f, 18.5f)
        close()
        moveTo(9f, 16f)
        lineTo(20f, 16f)
        lineTo(20f, 18f)
        lineTo(9f, 18f)
        close()
    }),
    delete = seal({
        moveTo(8f, 4f)
        lineTo(16f, 4f)
        lineTo(16f, 6.5f)
        lineTo(8f, 6.5f)
        close()
        moveTo(5f, 7.5f)
        lineTo(19f, 7.5f)
        lineTo(19f, 9.5f)
        lineTo(5f, 9.5f)
        close()
        moveTo(7f, 10.5f)
        lineTo(17f, 10.5f)
        lineTo(16f, 20f)
        lineTo(8f, 20f)
        close()
    }),
    favorite = seal { heart() },
    favoriteOutline = seal {
        moveTo(12f, 19.2f)
        lineTo(6f, 12.6f)
        curveTo(4.4f, 10.8f, 4.6f, 8f, 6.8f, 6.8f)
        curveTo(8.4f, 5.8f, 10.2f, 6.4f, 12f, 8.4f)
        curveTo(13.8f, 6.4f, 15.6f, 5.8f, 17.2f, 6.8f)
        curveTo(19.4f, 8f, 19.6f, 10.8f, 18f, 12.6f)
        close()
        moveTo(12f, 16.2f)
        lineTo(15.6f, 12.4f)
        curveTo(16.4f, 11.4f, 16.2f, 10f, 15.2f, 9.4f)
        curveTo(14.4f, 9f, 13.4f, 9.4f, 12f, 11.2f)
        curveTo(10.6f, 9.4f, 9.6f, 9f, 8.8f, 9.4f)
        curveTo(7.8f, 10f, 7.6f, 11.4f, 8.4f, 12.4f)
        close()
    },
    swap = seal({
        moveTo(3f, 6.5f)
        lineTo(16f, 6.5f)
        lineTo(16f, 9.5f)
        lineTo(3f, 9.5f)
        close()
        moveTo(13f, 4f)
        lineTo(20f, 8f)
        lineTo(13f, 12f)
        close()
        moveTo(8f, 14.5f)
        lineTo(21f, 14.5f)
        lineTo(21f, 17.5f)
        lineTo(8f, 17.5f)
        close()
        moveTo(11f, 12f)
        lineTo(4f, 16f)
        lineTo(11f, 20f)
        close()
    }),
    sort = seal({
        moveTo(6.5f, 3f)
        lineTo(9.5f, 3f)
        lineTo(9.5f, 16f)
        lineTo(6.5f, 16f)
        close()
        moveTo(4f, 13f)
        lineTo(8f, 20f)
        lineTo(12f, 13f)
        close()
        moveTo(14.5f, 8f)
        lineTo(17.5f, 8f)
        lineTo(17.5f, 21f)
        lineTo(14.5f, 21f)
        close()
        moveTo(12f, 11f)
        lineTo(16f, 4f)
        lineTo(20f, 11f)
        close()
    }),
    pause = seal({
        moveTo(6f, 4f)
        lineTo(10f, 4f)
        lineTo(10f, 20f)
        lineTo(6f, 20f)
        close()
    }, {
        moveTo(14f, 4f)
        lineTo(18f, 4f)
        lineTo(18f, 20f)
        lineTo(14f, 20f)
        close()
    }),
    replay = seal({
        moveTo(12f, 5f)
        arcTo(7f, 7f, 0f, true, true, 6.2f, 8f)
        lineTo(8.8f, 10.2f)
        arcTo(4.4f, 4.4f, 0f, true, false, 12f, 8f)
        close()
    }, {
        moveTo(4f, 3.5f)
        lineTo(9f, 3.5f)
        lineTo(9f, 9f)
        lineTo(6.5f, 6.8f)
        lineTo(4f, 6.8f)
        close()
    }),
    play = seal {
        moveTo(7f, 4f)
        lineTo(19f, 12f)
        lineTo(7f, 20f)
        close()
    },
    pages = seal({
        moveTo(5f, 4f)
        lineTo(18f, 4f)
        lineTo(18f, 16f)
        lineTo(5f, 16f)
        close()
    }, {
        moveTo(7f, 17.5f)
        lineTo(19f, 17.5f)
        lineTo(19f, 20f)
        lineTo(7f, 20f)
        close()
    }),
    rotate = seal({
        moveTo(12f, 5f)
        arcTo(7f, 7f, 0f, true, true, 6.5f, 8.2f)
        lineTo(9f, 10f)
        arcTo(4.2f, 4.2f, 0f, true, false, 12f, 8f)
        close()
    }, {
        moveTo(14f, 2.5f)
        lineTo(20f, 2.5f)
        lineTo(20f, 8.5f)
        lineTo(16.5f, 6f)
        lineTo(14f, 6f)
        close()
    }),
    night = seal {
        moveTo(14.5f, 3f)
        curveTo(8f, 4.2f, 5.5f, 9.5f, 6.5f, 14.5f)
        curveTo(7.4f, 19f, 11.5f, 21.5f, 16f, 20.2f)
        curveTo(11f, 19.2f, 8.2f, 15f, 9f, 10.5f)
        curveTo(9.6f, 7f, 12.2f, 4.6f, 14.5f, 3f)
        close()
    },
    up = seal {
        moveTo(12f, 5f)
        lineTo(19f, 15f)
        lineTo(14f, 15f)
        lineTo(14f, 20f)
        lineTo(10f, 20f)
        lineTo(10f, 15f)
        lineTo(5f, 15f)
        close()
    },
    left = seal {
        moveTo(15f, 4.5f)
        lineTo(7f, 12f)
        lineTo(15f, 19.5f)
        lineTo(15f, 15f)
        lineTo(11f, 12f)
        lineTo(15f, 9f)
        close()
    },
    right = seal {
        moveTo(9f, 4.5f)
        lineTo(17f, 12f)
        lineTo(9f, 19.5f)
        lineTo(9f, 15f)
        lineTo(13f, 12f)
        lineTo(9f, 9f)
        close()
    }
)
