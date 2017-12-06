package net.squanchy.support.view

import android.graphics.Rect
import android.support.annotation.Px
import android.support.v7.widget.RecyclerView
import android.view.View

class CardSpacingItemDecorator(
        @param:Px @field:Px
        private val horizontalSpacing: Int, @param:Px @field:Px
        private val verticalSpacing: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val count = state.itemCount

        var topSpacing = verticalSpacing / 2
        var bottomSpacing = topSpacing
        if (position == 0) {
            topSpacing = verticalSpacing
        } else if (position == count - 1) {
            bottomSpacing = verticalSpacing
        }

        outRect.set(horizontalSpacing, topSpacing, horizontalSpacing, bottomSpacing)
    }
}
