package com.simplemobiletools.calendar.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.DBHelper;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.models.Event;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EventActivity extends AppCompatActivity implements DBHelper.DBOperationsListener {
    @BindView(R.id.event_start_date) TextView mStartDate;
    @BindView(R.id.event_start_time) TextView mStartTime;
    @BindView(R.id.event_end_date) TextView mEndDate;
    @BindView(R.id.event_end_time) TextView mEndTime;
    @BindView(R.id.event_title) EditText mTitleET;
    @BindView(R.id.event_description) EditText mDescriptionET;

    private DateTime mEventStartDateTime;
    private DateTime mEventEndDateTime;
    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        if (intent == null)
            return;

        final Event event = (Event) intent.getSerializableExtra(Constants.EVENT);
        if (event != null) {
            mEvent = event;
            setupEditEvent();
        } else {
            mEvent = new Event();
            final String dayCode = intent.getStringExtra(Constants.DAY_CODE);
            if (dayCode == null || dayCode.isEmpty())
                return;

            setupNewEvent(dayCode);
        }

        updateStartDate();
        updateStartTime();
        updateEndDate();
        updateEndTime();
    }

    private void setupEditEvent() {
        mEventStartDateTime = new DateTime(mEvent.getStartTS() * 1000L, DateTimeZone.getDefault());
        mEventEndDateTime = new DateTime(mEvent.getEndTS() * 1000L, DateTimeZone.getDefault());
        mTitleET.setText(mEvent.getTitle());
        mDescriptionET.setText(mEvent.getDescription());
    }

    private void setupNewEvent(String dayCode) {
        mEventStartDateTime = Formatter.getDateTimeFromCode(dayCode).withZone(DateTimeZone.getDefault()).withHourOfDay(13);
        mEventEndDateTime = Formatter.getDateTimeFromCode(dayCode).withZone(DateTimeZone.getDefault()).withHourOfDay(14);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event, menu);
        final MenuItem item = menu.findItem(R.id.delete);
        if (mEvent.getId() == 0) {
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                deleteEvent();
                return true;
            case R.id.save:
                saveEvent();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteEvent() {
        DBHelper.newInstance(getApplicationContext(), this).deleteEvent(mEvent.getId());
    }

    private void saveEvent() {
        final String title = mTitleET.getText().toString().trim();
        if (title.isEmpty()) {
            Utils.showToast(getApplicationContext(), R.string.title_empty);
            mTitleET.requestFocus();
            return;
        }

        final int startTS = (int) (mEventStartDateTime.getMillis() / 1000);
        final int endTS = (int) (mEventEndDateTime.getMillis() / 1000);

        if (startTS > endTS) {
            Utils.showToast(getApplicationContext(), R.string.end_before_start);
            return;
        }

        final DBHelper dbHelper = DBHelper.newInstance(getApplicationContext(), this);
        final String description = mDescriptionET.getText().toString().trim();
        mEvent.setStartTS(startTS);
        mEvent.setEndTS(endTS);
        mEvent.setTitle(title);
        mEvent.setDescription(description);
        if (mEvent.getId() == 0) {
            dbHelper.insert(mEvent);
        } else {
            dbHelper.update(mEvent);
        }
    }

    private void updateStartDate() {
        mStartDate.setText(Formatter.getEventDate(mEventStartDateTime));
    }

    private void updateStartTime() {
        mStartTime.setText(Formatter.getEventTime(mEventStartDateTime));
    }

    private void updateEndDate() {
        mEndDate.setText(Formatter.getEventDate(mEventEndDateTime));
    }

    private void updateEndTime() {
        mEndTime.setText(Formatter.getEventTime(mEventEndDateTime));
    }

    @OnClick(R.id.event_start_date)
    public void startDateClicked(View view) {
        new DatePickerDialog(this, startDateSetListener, mEventStartDateTime.getYear(), mEventStartDateTime.getMonthOfYear() - 1,
                mEventStartDateTime.getDayOfMonth()).show();
    }

    @OnClick(R.id.event_start_time)
    public void startTimeClicked(View view) {
        new TimePickerDialog(this, startTimeSetListener, mEventStartDateTime.getHourOfDay(), mEventStartDateTime.getMinuteOfHour(), true)
                .show();
    }

    @OnClick(R.id.event_end_date)
    public void endDateClicked(View view) {
        new DatePickerDialog(this, endDateSetListener, mEventEndDateTime.getYear(), mEventEndDateTime.getMonthOfYear() - 1,
                mEventEndDateTime.getDayOfMonth()).show();
    }

    @OnClick(R.id.event_end_time)
    public void endTimeClicked(View view) {
        new TimePickerDialog(this, endTimeSetListener, mEventEndDateTime.getHourOfDay(), mEventEndDateTime.getMinuteOfHour(), true).show();
    }

    private final DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateSet(year, monthOfYear, dayOfMonth, true);
        }
    };

    private TimePickerDialog.OnTimeSetListener startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            timeSet(hourOfDay, minute, true);
        }
    };

    private DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateSet(year, monthOfYear, dayOfMonth, false);
        }
    };

    private TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            timeSet(hourOfDay, minute, false);
        }
    };

    private void dateSet(int year, int month, int day, boolean isStart) {
        if (isStart) {
            mEventStartDateTime = mEventStartDateTime.withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day);
            updateStartDate();
        } else {
            mEventEndDateTime = mEventEndDateTime.withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day);
            updateEndDate();
        }
    }

    private void timeSet(int hours, int minutes, boolean isStart) {
        if (isStart) {
            mEventStartDateTime = mEventStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes);
            updateStartTime();
        } else {
            mEventEndDateTime = mEventEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes);
            updateEndTime();
        }
    }

    @Override
    public void eventInserted() {
        Utils.showToast(getApplicationContext(), R.string.event_added);
        finish();
    }

    @Override
    public void eventUpdated() {
        Utils.showToast(getApplicationContext(), R.string.event_updated);
        finish();
    }

    @Override
    public void eventsDeleted() {
        finish();
    }

    @Override
    public void gotEvents(List<Event> events) {

    }
}