package com.example.healthmine.ui.self_report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.healthmine.databinding.InterfaceSelfReportBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class SelfReportHolderFragment: Fragment() {
    private val tabTitles = arrayOf("Self Report", "History")
    private lateinit var selfReportViewModel: SelfReportViewModel
    private var _binding: InterfaceSelfReportBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //this is a holder fragment for two fragments: self report and self report history
    private lateinit var tabLayout: TabLayout
    private lateinit var fragments:ArrayList<Fragment>
    private lateinit var viewPager2: ViewPager2
    private lateinit var fragmentSelfReport: SelfReportFragment
    private lateinit var fragmentSelfReportHistory:SelfReportHistoryFragment
    private lateinit var tabConfigurationStrategy: TabLayoutMediator.TabConfigurationStrategy
    private lateinit var selfReportFragmentStateAdapter: SelfReportStateAdapter
    private lateinit var tabLayoutMediator: TabLayoutMediator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        selfReportViewModel =
//            ViewModelProvider(this).get(SelfReportViewModel::class.java)

        _binding = InterfaceSelfReportBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //creating tablayout for child fragment
        tabLayout = binding.tablayout
        viewPager2 = binding.viewpager
        fragments = ArrayList()
        fragmentSelfReportHistory = SelfReportHistoryFragment()
        fragmentSelfReport = SelfReportFragment()
        fragments.add(fragmentSelfReport)
        fragments.add(fragmentSelfReportHistory)

        selfReportFragmentStateAdapter = SelfReportStateAdapter(requireActivity(), fragments)
        viewPager2.adapter = selfReportFragmentStateAdapter

        tabConfigurationStrategy = TabLayoutMediator.TabConfigurationStrategy(){
                tab: TabLayout.Tab, position: Int ->
            tab.text = tabTitles[position]
        }
        tabLayoutMediator= TabLayoutMediator(tabLayout, viewPager2, tabConfigurationStrategy)
        tabLayoutMediator.attach()
//        val textView: TextView = binding.textSlideshow
//        slideshowViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}