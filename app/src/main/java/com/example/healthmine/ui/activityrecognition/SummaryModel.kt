package com.example.healthmine.ui.activityrecognition

class SummaryModel(type: String?, duration: Float) {
    private var activityType: String? = type!!
    private var activityDuration: Float = duration!!

    fun getType(): String? {
        return activityType
    }
    fun setTitle(name: String?) {
        activityType = name!!
    }
    fun getDuration(): Float {
        return activityDuration
    }
    fun setDuration(d: Float) {
        activityDuration = d!!
    }
}
