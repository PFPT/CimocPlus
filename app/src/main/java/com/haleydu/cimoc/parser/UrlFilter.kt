package com.haleydu.cimoc.parser

class UrlFilter {
    @JvmField
    var Filter: String

    @JvmField
    var Regex: String

    @JvmField
    var Group: Int

    constructor(filter: String) {
        Filter = filter
        Regex = "(\\d+)"
        Group = 1
    }

    constructor(filter: String, regex: String) {
        Filter = filter
        Regex = regex
        Group = 1
    }

    constructor(filter: String, regex: String, group: Int) {
        Filter = filter
        Regex = regex
        Group = group
    }
}
