package com.haleydu.cimoc.model

class MiniComic(comic: Comic) {
    var id: Long? = comic.id
    var source: Int = comic.source
    var cid: String = comic.cid
    var title: String = comic.title
    var cover: String = comic.cover
    private var finish: Boolean? = comic.finish
    var isHighlight: Boolean = comic.highlight
    var isLocal: Boolean = comic.local
        private set

    override fun equals(other: Any?): Boolean {
        if (other !is MiniComic) {
            return false
        }
        val otherId = other.id
        if (id == null) {
            return otherId == null
        }
        return id == otherId
    }

    override fun hashCode(): Int = id?.hashCode() ?: super.hashCode()

    var isFinish: Boolean?
        get() = finish
        set(value) { finish = value }

    companion object {
        @JvmStatic
        fun listOf(comics: List<Comic>): List<MiniComic> {
            val result = ArrayList<MiniComic>(comics.size)
            for (comic in comics) {
                result.add(MiniComic(comic))
            }
            return result
        }
    }
}
