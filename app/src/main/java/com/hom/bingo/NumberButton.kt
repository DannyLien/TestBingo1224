package com.hom.bingo

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class NumberButton @JvmOverloads constructor(
    context: Context, attributeSet: AttributeSet?=null, defStyleInt: Int=0
):AppCompatButton(context, attributeSet,defStyleInt) {
    var number = 0
    var picked = false
    var pos = 0
}