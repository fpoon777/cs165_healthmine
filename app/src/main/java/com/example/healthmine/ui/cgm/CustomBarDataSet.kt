package com.example.healthmine.ui.cgm

import com.github.mikephil.charting.data.BarEntry

import com.github.mikephil.charting.data.BarDataSet
import android.graphics.LinearGradient

import com.github.mikephil.charting.model.GradientColor

import com.github.mikephil.charting.buffer.BarBuffer

import com.github.mikephil.charting.data.BarData

import com.github.mikephil.charting.interfaces.datasets.IBarDataSet

import com.github.mikephil.charting.utils.ViewPortHandler

import com.github.mikephil.charting.animation.ChartAnimator

import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider

import android.graphics.RectF

import com.github.mikephil.charting.renderer.BarChartRenderer

import android.content.Context

import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Shader
import android.util.AttributeSet

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.highlight.Range
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils


class CustomBarDataSet(private val yVals: List<BarEntry?>?, label: String?) :
    BarDataSet(yVals, label) {
    override fun getEntryIndex(e: BarEntry?): Int {
        return if(yVals.isNullOrEmpty()){
            -1
        }else{
            yVals.indexOf(e)
        }
    }

    override fun getColor(index: Int): Int {
        return if (yVals?.get(index)?.y!! <= 110) {
            println("debug: "+ mColors[2].toString())
            mColors[0]
        }
        else if (yVals?.get(index)?.y!! <= 140){
            mColors[1]
        }
        else{
            mColors[2]
        }
    }
}

