package com.example.healthmine.ui.self_report

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthmine.R
import com.example.healthmine.database.HealthmineDatabase
import com.example.healthmine.databinding.FragmentSelfReportHistoryBinding
import com.example.healthmine.ui.self_report.self_report_db.SelfReportDbDao
import com.example.healthmine.ui.self_report.self_report_db.SelfReportEntry
import com.example.healthmine.ui.self_report.self_report_db.SelfReportRepo

class SelfReportHistoryFragment: Fragment() {
    private var _binding: FragmentSelfReportHistoryBinding? = null

    private val binding get() = _binding!!
    //database
    private lateinit var database: HealthmineDatabase
    private lateinit var databaseDao: SelfReportDbDao
    private lateinit var repository: SelfReportRepo
    private lateinit var selfReportViewModel: SelfReportViewModel
    private lateinit var factoryModel:SelfReportViewFactory

    // listview
    private lateinit var historyListView:ListView
    private lateinit var selfReportHistoryAdapter: SelfReportHistoryAdapter
    private lateinit var arrayList:ArrayList<SelfReportEntry>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelfReportHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //access database
        database = HealthmineDatabase.getInstance(requireContext())
        databaseDao = database.selfReportDatabaseDao
        repository = SelfReportRepo(databaseDao)
        factoryModel= SelfReportViewFactory(repository)
        selfReportViewModel = ViewModelProvider(this, factoryModel)
            .get(SelfReportViewModel::class.java)

        historyListView = binding.selfReportHistoryList
        arrayList = ArrayList()
        selfReportHistoryAdapter = SelfReportHistoryAdapter(requireContext(), arrayList)
        historyListView.adapter = selfReportHistoryAdapter

        selfReportViewModel.allExerciseEntriesLiveData.observe(requireActivity()){
            historyListView.invalidateViews()
            selfReportHistoryAdapter.selfReportEntryList = it
            selfReportHistoryAdapter.notifyDataSetChanged()
        }

        historyListView.setOnItemClickListener { parent, view, position, id ->
            val textViewId = view.findViewById<TextView>(R.id.history_id)
            var bundle = Bundle()
            bundle.putString("id", textViewId.text.toString())
            var outIntent:Intent = Intent(requireContext(), DisplaySelfReportActivity::class.java)
            outIntent.putExtras(bundle)
            startActivity(outIntent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}