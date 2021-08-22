/*
* Дата создания: 22.08.2021
* Автор: Лукьянов Андрей. Студент 3 курса Физического факультета МГУ.
*/

package com.daiwerystudio.chronos.ui
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.daiwerystudio.chronos.R
import com.google.android.material.appbar.AppBarLayout

/*
 * В данном файле написаны все кастомные Behavior, использующиеся в приложении, а также
 * необходимые классы, расширяющие обычные виджеты.
 */


/**
 * Поведение для View, зависящее от AppBarLayout. Получает их xml атрибут behavior_startHeight.
 */
class FadeViewAppBarBehavior @JvmOverloads constructor(context: Context? = null,
                                                        attrs: AttributeSet? = null) :
    CoordinatorLayout.Behavior<View>(context, attrs) {

    private var startHeight: Float = 0f
    private var minHeightAppBar: Float = 0f

    init {
        context?.theme?.obtainStyledAttributes(attrs, R.styleable.FadeViewAppBarBehavior,
            0, 0)?.apply {
            try {
                startHeight = getDimensionPixelSize(R.styleable.FadeViewAppBarBehavior_behavior_startHeight, 0).toFloat()
                minHeightAppBar = getDimensionPixelSize(R.styleable.FadeViewAppBarBehavior_behavior_minHeightAppBar, 0).toFloat()
            } finally {
                recycle()
            }
        }
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: View,
                                 dependency: View): Boolean = dependency is AppBarLayout

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: View,
                                        dependency: View): Boolean {
        val appBar = dependency as AppBarLayout

        val currentHeightAppBar = (appBar.height+appBar.y)  // Значение 'y' меньше нуля.
        val ratio = (currentHeightAppBar-minHeightAppBar)/(appBar.height-minHeightAppBar)

        child.y = currentHeightAppBar-child.height
        child.alpha = ratio
        child.layoutParams?.height = (startHeight*ratio).toInt()
        child.requestLayout()
        return false
    }
}




