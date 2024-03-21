package com.hsc.compiler.span

data class Span(val lo: Int, val hi: Int, val fid: Int) {
    companion object {
        fun single(pos: Int, fid: Int): Span = Span(pos, pos, fid)

        val none: Span get() = Span(0, 0, -1)
    }

    val loSpan: Span get() = single(lo, fid)
    val hiSpan: Span get() = single(hi, fid)

}