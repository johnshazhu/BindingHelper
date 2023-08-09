package com.test.binding

import android.os.Bundle
import android.util.Log
import android.view.View
import com.test.binding.databinding.FragmentMainBinding
import com.test.binding.model.TestData

class TestFragment : BaseFragment<FragmentMainBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("xdebug", "onViewCreated")
        binding.user = TestData("Someone", 20)
    }
}