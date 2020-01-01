package com.surrus.galwaybus.support

import android.view.View

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

import java.util.ArrayList

import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion

import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat

object RecyclerViewMatchers {

    fun withRecyclerView(recyclerViewId: Int): RecyclerViewMatcher {
        return RecyclerViewMatcher(recyclerViewId)
    }

    fun havingItemCount(itemCount: Int): RecyclerViewItemCountAssertion {
        return RecyclerViewItemCountAssertion(itemCount)
    }

    class RecyclerViewMatcher(private val recyclerViewId: Int) {

        /**
         * This method gets the position of the view in a recycle grid.
         * If the position is greater then the grid size being displayed it will
         * calculate the appropriate view in the last row of the grid. If a gird size of zero is
         * passed this calculation will not be run.
         * @param position position in the grid
         * @param targetViewId id of the view being verified
         * @param gridSize the size of the grid layout
         * @return Matcher<>View>
         */

        fun atPositionOnViewWithGridCorrection(position: Int, @IdRes targetViewId: Int, gridSize: Int): Matcher<View> {

            return object : TypeSafeMatcher<View>() {

                internal var childView: View? = null

                override fun describeTo(description: Description) {
                    description.appendText("at position $position on child view with id: $targetViewId")
                }

                public override fun matchesSafely(view: View): Boolean {
                    if (childView == null) {
                        val recyclerView = view.rootView.findViewById<View>(recyclerViewId) as RecyclerView
                        if (recyclerView != null && recyclerView.id == recyclerViewId) {
                            // Calculation to determine the approximate location of child view
                            // in the last row of the grid.
                            if (position >= recyclerView.childCount) {
                                val correction = gridSize - position % gridSize
                                childView = recyclerView.getChildAt(recyclerView.childCount - correction)
                            } else {
                                childView = recyclerView.getChildAt(position)
                            }
                        } else {
                            return false
                        }
                    }
                    val targetView = childView!!.findViewById<View>(targetViewId)
                    return view === targetView
                }
            }
        }

        /**
         * This method returns the position of a view in a recycle view grid.
         * @param position position id
         * @param targetViewId view id
         * @return Matcher<View>
        </View> */
        fun atPositionOnView(position: Int, @IdRes targetViewId: Int): Matcher<View> {
            return atPositionOnViewWithGridCorrection(position, targetViewId, 0)
        }
    }

    class RecyclerViewItemCountAssertion(private val expectedCount: Int) : ViewAssertion {

        override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
            if (noViewFoundException != null) {
                throw noViewFoundException
            }

            val recyclerView = view as RecyclerView
            val adapter = recyclerView.adapter
            assertThat(adapter!!.itemCount, `is`(expectedCount))
        }
    }

    fun viewHolderWithDescendentWithText(text: CharSequence): Matcher<RecyclerView.ViewHolder> {
        return object : TypeSafeMatcher<RecyclerView.ViewHolder>() {
            override fun matchesSafely(viewHolder: RecyclerView.ViewHolder): Boolean {
                val ret = ArrayList<View>()
                viewHolder.itemView.findViewsWithText(ret, text, View.FIND_VIEWS_WITH_TEXT)
                return !ret.isEmpty()
            }

            override fun describeTo(description: Description) {
                description.appendText("view holder with descendent with text '$text'")
            }
        }
    }
}
