package com.haleydu.cimoc.shared.model

data class CheckFailure(
    val title: String,
    val kind: String
)

data class FavoriteCheck(
    val updated: Int = 0,
    val failures: List<CheckFailure> = emptyList()
)

data class ImportPreview(
    val title: String,
    val host: String
)

fun checkKindLabel(kind: String): String = when (kind) {
    "NETWORK" -> "网络"
    "EMPTY" -> "空结果"
    else -> "解析"
}

fun favoriteCheckText(check: FavoriteCheck): String {
    if (check.failures.isEmpty()) {
        return if (check.updated == 0) "已是最新" else "发现 ${check.updated} 部更新"
    }
    val detail = check.failures.joinToString("，") { "${it.title}（${checkKindLabel(it.kind)}）" }
    return if (check.updated == 0) "更新失败：$detail" else "发现 ${check.updated} 部更新，失败：$detail"
}
