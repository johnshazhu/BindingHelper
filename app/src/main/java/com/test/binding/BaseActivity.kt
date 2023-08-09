package com.test.binding

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import io.github.johnshazhu.lib.binding.helper.getBinding

open class BaseActivity<T : ViewBinding> : FragmentActivity() {
    protected lateinit var binding: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = getBinding(this, layoutInflater)
        setContentView(binding.root)
    }
}