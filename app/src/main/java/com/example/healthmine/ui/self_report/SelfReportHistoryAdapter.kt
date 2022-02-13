package com.example.healthmine.ui.self_report

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.healthmine.R
import com.example.healthmine.ui.self_report.self_report_db.SelfReportEntry
import java.lang.Exception

class SelfReportHistoryAdapter(
    val context: Context,
    var selfReportEntryList: List<SelfReportEntry>
): BaseAdapter() {
    override fun getCount(): Int {
        return selfReportEntryList.size
    }

    override fun getItem(position: Int): Any {
        return selfReportEntryList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = View.inflate(context, R.layout.history_list_item, null)
        var textViewID = view.findViewById<TextView>(R.id.history_id)
        var textViewDate = view.findViewById<TextView>(R.id.history_date)
        var textViewFeel = view.findViewById<TextView>(R.id.history_feel)

        try{
            textViewID.text = selfReportEntryList[position].id.toString()
            textViewDate.text = selfReportEntryList[position].dateTime
            var healthCondition = selfReportEntryList[position].rateHealth
            when (healthCondition){
                0 -> {
                    textViewFeel.text = context.resources.getStringArray(R.array.self_report_spinner_items)[0]
//                    view.background = ContextCompat.getDrawable(context, R.color.health_good)
                }
                1 -> {
                    textViewFeel.text = context.resources.getStringArray(R.array.self_report_spinner_items)[1]
//                    view.background = ContextCompat.getDrawable(context, R.color.health_ok)
                }
                2 -> {
                    textViewFeel.text = context.resources.getStringArray(R.array.self_report_spinner_items)[2]
//                    view.background = ContextCompat.getDrawable(context, R.color.health_sick)
                }
                3 -> {
                    textViewFeel.text = context.resources.getStringArray(R.array.self_report_spinner_items)[3]
//                    view.background = ContextCompat.getDrawable(context, R.color.health_very_sick)
                }
                else ->{}
            }
        }
        catch (e:Exception){
            println("Error at position: $position")
        }

        return view
    }
}