package com.test.binding.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import io.github.johnshazhu.lib.binding.helper.getBinding

open class BaseFrameLayout<T: ViewBinding> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    protected lateinit var binding: T

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!::binding.isInitialized) {
            binding = getBinding(this, LayoutInflater.from(context), this, true)
        }
    }
}