package com.simplemobiletools.calendar.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.adapters.WeekEventsAdapter
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.WEEK_START_TIMESTAMP
import com.simplemobiletools.calendar.helpers.WeeklyCalendarImpl
import com.simplemobiletools.calendar.interfaces.WeeklyCalendar
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.views.MyScrollView
import kotlinx.android.synthetic.main.fragment_week.view.*
import kotlin.comparisons.compareBy

class WeekFragment : Fragment(), WeeklyCalendar {
    private var mListener: WeekScrollListener? = null
    private var mWeekTimestamp = 0
    lateinit var mView: View
    lateinit var mCalendar: WeeklyCalendarImpl
    lateinit var mRes: Resources

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_week, container, false).apply {

            week_events_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
                override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                    mListener?.scrollTo(y)
                }
            })

            week_events_scrollview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    updateScrollY(MainActivity.mWeekScrollY)
                    mView.week_events_scrollview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

            week_events_grid.adapter = WeekEventsAdapter(context)
        }

        mRes = resources
        mWeekTimestamp = arguments.getInt(WEEK_START_TIMESTAMP)
        mCalendar = WeeklyCalendarImpl(this, context)
        setupDayLabels()
        return mView
    }

    private fun setupDayLabels() {
        var curDay = Formatter.getDateTimeFromTS(mWeekTimestamp)
        for (i in 0..6) {
            val view = mView.findViewById(mRes.getIdentifier("week_day_label_$i", "id", context.packageName)) as TextView
            val dayLetter = getDayLetter(curDay.dayOfWeek)
            view.text = "$dayLetter\n${curDay.dayOfMonth}"
            curDay = curDay.plusDays(1)
        }
    }

    private fun getDayLetter(pos: Int): String {
        return mRes.getString(when (pos) {
            1 -> R.string.monday_letter
            2 -> R.string.tuesday_letter
            3 -> R.string.wednesday_letter
            4 -> R.string.thursday_letter
            5 -> R.string.friday_letter
            6 -> R.string.saturday_letter
            else -> R.string.sunday_letter
        })
    }

    override fun onResume() {
        super.onResume()
        mCalendar.updateWeeklyCalendar(mWeekTimestamp)
    }

    override fun updateWeeklyCalendar(events: List<Event>) {
        val fullHeight = mRes.getDimension(R.dimen.weekly_view_events_height)
        val minuteHeight = fullHeight / (24 * 60)
        val minimalHeight = mRes.getDimension(R.dimen.weekly_view_minimal_event_height).toInt()
        val eventColor = context.config.primaryColor
        val sideMargin = mRes.displayMetrics.density.toInt()
        (0..6).map { getColumnWithId(it) }
                .forEach { activity.runOnUiThread { it.removeAllViews() } }

        val sorted = events.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description }))
        for (event in sorted) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val dayOfWeek = startDateTime.dayOfWeek - if (context.config.isSundayFirst) 0 else 1
            val layout = getColumnWithId(dayOfWeek)

            val startMinutes = startDateTime.minuteOfDay
            val duration = endDateTime.minuteOfDay - startMinutes

            (LayoutInflater.from(context).inflate(R.layout.week_event_marker, null, false) as TextView).apply {
                background = ColorDrawable(eventColor)
                text = event.title
                activity.runOnUiThread {
                    layout.addView(this)
                    (layoutParams as RelativeLayout.LayoutParams).apply {
                        rightMargin = sideMargin
                        topMargin = (startMinutes * minuteHeight).toInt()
                        width = layout.width
                        height = if (event.startTS == event.endTS) minimalHeight else (duration * minuteHeight).toInt() - sideMargin
                    }
                }
            }
        }
    }

    private fun getColumnWithId(id: Int) = mView.findViewById(mRes.getIdentifier("week_column_$id", "id", context.packageName)) as RelativeLayout

    fun setListener(listener: WeekScrollListener) {
        mListener = listener
    }

    fun updateScrollY(y: Int) {
        mView.week_events_scrollview.scrollY = y
    }

    interface WeekScrollListener {
        fun scrollTo(y: Int)
    }
}
