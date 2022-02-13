package com.example.healthmine.ui.activityrecognition

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.healthmine.R
import kotlin.math.round

internal class ActivityAdapter(private var summaryList: List<SummaryModel>) :
    RecyclerView.Adapter<ActivityAdapter.MyViewHolder>() {
    internal inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var duration: TextView = view.findViewById(R.id.first_time_text)
        var activityType: TextView = view.findViewById(R.id.first_act_text)
        var icon: ImageView = view.findViewById(R.id.first_img)
    }
    @NonNull
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_recognition_summary_adapter, parent, false)
        return MyViewHolder(itemView)
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val summary = summaryList[position]
        val durationInSec:Float = summary.getDuration()
        println("durationInSec is $durationInSec")
        val hourdurationInSec:Int = Math.floor((durationInSec/3600).toDouble()).toInt()
        val minutedurationInSec:Int = round(((durationInSec%3600)/60).toDouble()).toInt()
        println("minutedurationInSec is ${(durationInSec%3600)/60}")
        holder.duration.text = "$hourdurationInSec h $minutedurationInSec min"
        holder.activityType.text = summary.getType()
        when {
            summary.getType() == "Still" -> {
                println("Enter still adapter")
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_accessibility_24)
            }
            summary.getType() == "Walk" -> {
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_directions_walk_24)
            }
            summary.getType() == "Run" -> {
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_directions_run_24)
            }
            summary.getType() == "Vehicle" -> {
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_drive_eta_24)
            }
            summary.getType() == "Bicycle" -> {
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_directions_bike_24)
            }
            summary.getType() == "Tilt" -> {
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_eject_24)
            }
            summary.getType() == "OnFoot" -> {
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_emoji_nature_24)
            }
            summary.getType() == "Unknown" -> {
                println("Enter unknown adapter")
                holder.icon.setBackgroundResource(R.drawable.ic_baseline_question_mark_24)
            }
        }
    }
    override fun getItemCount(): Int {
        return summaryList.size
    }
}


