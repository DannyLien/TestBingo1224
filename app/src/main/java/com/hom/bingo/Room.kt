package com.hom.bingo

class Room(
    var id: String,
    var title: String,
    var status: Int = 0,
    var init: Member?
) {
    constructor() : this("", "", 0, null)
    constructor(title: String, init: Member?) : this("", title, 0, init)
}