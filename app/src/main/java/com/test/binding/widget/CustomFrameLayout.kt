package com.test.binding.widget

import android.content.Context
import android.util.AttributeSet
import com.test.binding.databinding.BaseFrameBinding

class CustomFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseFrameLayout<BaseFrameBinding>(context, attrs, defStyleAttr) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        binding.run {
            textTv.text = "CustomFrameLayout"
        }
    }
}