package com.haleydu.cimoc.model

import org.json.JSONObject

class SourceConfig(
    @JvmField val key: String,
    @JvmField val type: Int,
    @JvmField val title: String,
    values: Map<String, String>
) {
    constructor(key: String, type: Int, title: String, `object`: JSONObject) : this(key, type, title, fromJson(`object`))

    @JvmField
    val baseUrl: String = trimSlash(value(values, "baseUrl"))

    @JvmField
    val serverUrl: String = value(values, "serverUrl")

    @JvmField
    val search: String = value(values, "search")

    @JvmField
    val searchKey: String = value(values, "searchKey")

    @JvmField
    val searchInfoList: String = value(values, "searchInfoList")

    @JvmField
    val searchInfoCid: String = value(values, "searchInfoCid")

    @JvmField
    val searchInfoTitle: String = value(values, "searchInfoTitle")

    @JvmField
    val searchInfoCover: String = value(values, "searchInfoCover")

    @JvmField
    val searchInfoAuthor: String = value(values, "searchInfoAuthor")

    @JvmField
    val searchInfoUpdate: String = value(values, "searchInfoUpdate")

    @JvmField
    val parseInfoTitle: String = value(values, "parseInfoTitle")

    @JvmField
    val parseInfoCover: String = value(values, "parseInfoCover")

    @JvmField
    val parseInfoIntro: String = value(values, "parseInfoIntro")

    @JvmField
    val parseInfoAuthor: String = value(values, "parseInfoAuthor")

    @JvmField
    val parseInfoUpdate: String = value(values, "parseInfoUpdate")

    @JvmField
    val parseInfoStatus: String = value(values, "parseInfoStatus")

    @JvmField
    val parseInfoTags: String = value(values, "parseInfoTags")

    @JvmField
    val parseChapterList1: String = value(values, "parseChapterList1")

    @JvmField
    val parseChapterList2: String = value(values, "parseChapterList2")

    @JvmField
    val parseChapterList3: String = value(values, "parseChapterList3")

    @JvmField
    val parseChapterPath: String = value(values, "parseChapterPath")

    @JvmField
    val parseChapterTitle: String = value(values, "parseChapterTitle")

    @JvmField
    val parseImageList: String = value(values, "parseImageList")

    @JvmField
    val parseImageUrl: String = firstNonEmpty(value(values, "parseImageUrl"), value(values, "parseImageSrc"))

    @JvmField
    val parseImageFaultImage: String = value(values, "parseImageFaultImage")

    @JvmField
    val imageServerUrl: String = firstNonEmpty(value(values, "imageServerUrl"), serverUrl)

    @JvmField
    val parseCategoryPath: String = value(values, "parseCategoryPath")

    @JvmField
    val parseCategoryInfoList: String = firstNonEmpty(value(values, "parseCategoryInfoList"), searchInfoList)

    @JvmField
    val parseCategoryInfoCid: String = firstNonEmpty(value(values, "parseCategoryInfoCid"), searchInfoCid)

    @JvmField
    val parseCategoryInfoCidPre: String = value(values, "parseCategoryInfoCidPre")

    @JvmField
    val parseCategoryInfoTitle: String = firstNonEmpty(value(values, "parseCategoryInfoTitle"), searchInfoTitle)

    @JvmField
    val parseCategoryInfoCover: String = firstNonEmpty(value(values, "parseCategoryInfoCover"), searchInfoCover)

    @JvmField
    val parseCategoryInfoAuthor: String = firstNonEmpty(value(values, "parseCategoryInfoAuthor"), searchInfoAuthor)

    @JvmField
    val parseCategoryInfoUpdate: String = firstNonEmpty(value(values, "parseCategoryInfoUpdate"), searchInfoUpdate)

    fun hasCategory(): Boolean = parseCategoryPath.isNotEmpty()

    fun isComplete(): Boolean {
        return baseUrl.isNotEmpty() &&
            (search.isNotEmpty() || searchKey.isNotEmpty()) &&
            searchInfoList.isNotEmpty()
    }

    fun toSource(enable: Boolean): Source {
        return Source(null, title, type, enable)
    }

    companion object {
        private fun fromJson(`object`: JSONObject): Map<String, String> {
            val names = `object`.keys()
            val values = HashMap<String, String>()
            while (names.hasNext()) {
                val name = names.next()
                values[name] = `object`.optString(name, "").trim()
            }
            return values
        }

        private fun value(values: Map<String, String>, name: String): String {
            return values[name]?.trim().orEmpty()
        }

        private fun firstNonEmpty(a: String?, b: String?): String {
            return if (a.isNullOrEmpty()) (b ?: "") else a
        }

        private fun trimSlash(url: String?): String {
            if (url.isNullOrEmpty()) {
                return ""
            }
            return if (url.endsWith("/")) {
                url.substring(0, url.length - 1)
            } else {
                url
            }
        }
    }
}
