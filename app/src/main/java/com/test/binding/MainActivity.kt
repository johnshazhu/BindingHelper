package com.test.binding

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.test.binding.widget.CustomFrameLayout

class MainActivity : TestActivity() {
    private lateinit var testFragment: TestFragment
    private lateinit var customFrameLayout: CustomFrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.run {
            testTv.text = "Hello World"
        }
    }

    fun onTestFragmentBinding(v: View) {
        if (!::testFragment.isInitialized) {
            supportFragmentManager.beginTransaction().add(
                R.id.container, TestFragment().also { testFragment = it }
            ).commitAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction()
                .show(testFragment)
                .commitAllowingStateLoss()
        }
    }

    fun onTestViewBinding(v: View) {
        hideTestFragment()
        if (binding.root.getChildAt(0) !is CustomFrameLayout) {
            binding.root.addView(CustomFrameLayout(v.context).also { customFrameLayout = it }, 0, ViewGroup.LayoutParams(-1, -1))
        } else {
            customFrameLayout.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        if (hideTestFragment()) {
            return
        }
        if (::customFrameLayout.isInitialized && customFrameLayout.visibility != View.GONE) {
            customFrameLayout.visibility = View.GONE
            return
        }
        super.onBackPressed()
    }

    private fun hideTestFragment(): Boolean {
        if (::testFragment.isInitialized && !testFragment.isHidden) {
            supportFragmentManager.beginTransaction()
                .hide(testFragment)
                .commitAllowingStateLoss()
            return true
        }
        return false
    }
}