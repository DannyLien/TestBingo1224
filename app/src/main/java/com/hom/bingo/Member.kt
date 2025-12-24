package com.hom.bingo

class Member(
    var uid: String,
    var displayName: String,
    var nickname: String? = null,
    var avatarId: Int = 0
) {
    constructor() : this("", "", null, 0)

}