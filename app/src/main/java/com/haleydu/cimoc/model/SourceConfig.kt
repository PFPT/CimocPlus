package com.haleydu.cimoc.model

import org.json.JSONObject

class SourceConfig(@JvmField val key: String, @JvmField val type: Int, @JvmField val title: String, `object`: JSONObject) {

    @JvmField
    val baseUrl: String = trimSlash(opt(`object`, "baseUrl"))

    @JvmField
    val serverUrl: String = opt(`object`, "serverUrl")

    @JvmField
    val search: String = opt(`object`, "search")

    @JvmField
    val searchKey: String = opt(`object`, "searchKey")

    @JvmField
    val searchInfoList: String = opt(`object`, "searchInfoList")

    @JvmField
    val searchInfoCid: String = opt(`object`, "searchInfoCid")

    @JvmField
    val searchInfoTitle: String = opt(`object`, "searchInfoTitle")

    @JvmField
    val searchInfoCover: String = opt(`object`, "searchInfoCover")

    @JvmField
    val searchInfoAuthor: String = opt(`object`, "searchInfoAuthor")

    @JvmField
    val searchInfoUpdate: String = opt(`object`, "searchInfoUpdate")

    @JvmField
    val parseInfoTitle: String = opt(`object`, "parseInfoTitle")

    @JvmField
    val parseInfoCover: String = opt(`object`, "parseInfoCover")

    @JvmField
    val parseInfoIntro: String = opt(`object`, "parseInfoIntro")

    @JvmField
    val parseInfoAuthor: String = opt(`object`, "parseInfoAuthor")

    @JvmField
    val parseInfoUpdate: String = opt(`object`, "parseInfoUpdate")

    @JvmField
    val parseInfoStatus: String = opt(`object`, "parseInfoStatus")

    @JvmField
    val parseChapterList1: String = opt(`object`, "parseChapterList1")

    @JvmField
    val parseChapterList2: String = opt(`object`, "parseChapterList2")

    @JvmField
    val parseChapterList3: String = opt(`object`, "parseChapterList3")

    @JvmField
    val parseChapterPath: String = opt(`object`, "parseChapterPath")

    @JvmField
    val parseChapterTitle: String = opt(`object`, "parseChapterTitle")

    @JvmField
    val parseImageList: String = opt(`object`, "parseImageList")

    @JvmField
    val parseImageUrl: String = firstNonEmpty(opt(`object`, "parseImageUrl"), opt(`object`, "parseImageSrc"))

    @JvmField
    val parseImageFaultImage: String = opt(`object`, "parseImageFaultImage")

    @JvmField
    val imageServerUrl: String = firstNonEmpty(opt(`object`, "imageServerUrl"), serverUrl)

    @JvmField
    val parseCategoryPath: String = opt(`object`, "parseCategoryPath")

    @JvmField
    val parseCategoryInfoList: String = firstNonEmpty(opt(`object`, "parseCategoryInfoList"), searchInfoList)

    @JvmField
    val parseCategoryInfoCid: String = firstNonEmpty(opt(`object`, "parseCategoryInfoCid"), searchInfoCid)

    @JvmField
    val parseCategoryInfoCidPre: String = opt(`object`, "parseCategoryInfoCidPre")

    @JvmField
    val parseCategoryInfoTitle: String = firstNonEmpty(opt(`object`, "parseCategoryInfoTitle"), searchInfoTitle)

    @JvmField
    val parseCategoryInfoCover: String = firstNonEmpty(opt(`object`, "parseCategoryInfoCover"), searchInfoCover)

    @JvmField
    val parseCategoryInfoAuthor: String = firstNonEmpty(opt(`object`, "parseCategoryInfoAuthor"), searchInfoAuthor)

    @JvmField
    val parseCategoryInfoUpdate: String = firstNonEmpty(opt(`object`, "parseCategoryInfoUpdate"), searchInfoUpdate)

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
        private fun opt(`object`: JSONObject, name: String): String {
            val value = `object`.optString(name, "")
            return value?.trim() ?: ""
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
