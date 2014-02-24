package com.squareup.timessquare;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static com.squareup.timessquare.CalendarPickerView.SelectionMode;
import static com.squareup.timessquare.CalendarPickerView.betweenDates;
import static com.squareup.timessquare.CalendarPickerView.containsDate;
import static com.squareup.timessquare.CalendarPickerView.maxDate;
import static com.squareup.timessquare.CalendarPickerView.minDate;
import static com.squareup.timessquare.CalendarPickerView.sameDate;
import static com.squareup.timessquare.CalendarPickerView.sameMonth;
import static com.squareup.timessquare.CalendarPickerView.setMidnight;
import static com.squareup.timessquare.MonthCellDescriptor.RangeState;
import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

/**
 * Created by Blaž Šolar on 17/02/14.
 */
public class CollapseCalendarView extends FrameLayout {

    final MonthView.Listener listener = new CellClickedListener();

    final List<Calendar> selectedCals = new ArrayList<>();
    final List<Calendar> highlightedCals = new ArrayList<>();
    final List<MonthCellDescriptor> selectedCells = new ArrayList<>();
    private List<List<MonthCellDescriptor>> mCells = new ArrayList<>();

    private MonthDescriptor mMonth;
    private MonthView mMonthView;

    SelectionMode selectionMode;

    private Locale locale;
    private DateFormat monthNameFormat;
    private DateFormat weekdayNameFormat;
    private DateFormat fullDateFormat;
    private Calendar minCal;
    private Calendar maxCal;
    Calendar today;

    private CalendarPickerView.OnDateSelectedListener dateListener;
    private CalendarPickerView.DateSelectableFilter dateConfiguredListener;

    public CollapseCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        locale = Locale.getDefault();
        monthNameFormat = new SimpleDateFormat(context.getString(R.string.month_name_format), locale);
        weekdayNameFormat = new SimpleDateFormat(context.getString(R.string.day_name_format), locale);
        fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        minCal = Calendar.getInstance(locale);
        today = Calendar.getInstance(locale);
        selectionMode = SelectionMode.SINGLE;

        mMonthView = MonthView.create(this, LayoutInflater.from(getContext()), weekdayNameFormat,
                listener, today);
        addView(mMonthView);

        changeMonth(today);
    }

    public void setDateListener(CalendarPickerView.OnDateSelectedListener dateListener) {
        this.dateListener = dateListener;
    }

    public Date getSelectedDate() {
        return (selectedCals.size() > 0 ? selectedCals.get(0).getTime() : null);
    }

    public void setSelectedDate(Calendar calendar) {
        if(!sameMonth(calendar, mMonth)) {
            changeMonth(calendar);
        }

        for(List<MonthCellDescriptor> week : mCells) {
            for(MonthCellDescriptor day : week) {
                Calendar dayCalendar = Calendar.getInstance(locale);
                dayCalendar.setTime(day.getDate());
                if(sameDate(calendar, dayCalendar)) {
                    doSelectDate(calendar.getTime(), day);
                    return;
                }
            }
        }
    }

    private void changeMonth(Calendar calendar) {

        Date date = calendar.getTime();

        // get current month
        mMonth = new MonthDescriptor(calendar.get(MONTH), calendar.get(YEAR), date,
                monthNameFormat.format(date));
        mCells = getMonthCells(mMonth, calendar);

        initMonth();
    }

    private void initMonth() {
        mMonthView.init(mMonth, mCells, false);
    }

    private List<List<MonthCellDescriptor>> getMonthCells(MonthDescriptor month, Calendar startCal) {
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(startCal.getTime());
        List<List<MonthCellDescriptor>> cells = new ArrayList<List<MonthCellDescriptor>>();
        cal.set(DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(DAY_OF_WEEK);
        int offset = cal.getFirstDayOfWeek() - firstDayOfWeek;
        if (offset > 0) {
            offset -= 7;
        }
        cal.add(Calendar.DATE, offset);

        Calendar minSelectedCal = minDate(selectedCals);
        Calendar maxSelectedCal = maxDate(selectedCals);

        while ((cal.get(MONTH) < month.getMonth() + 1 || cal.get(YEAR) < month.getYear()) //
                && cal.get(YEAR) <= month.getYear()) {
            Logr.d("Building week row starting at %s", cal.getTime());
            List<MonthCellDescriptor> weekCells = new ArrayList<MonthCellDescriptor>();
            cells.add(weekCells);
            for (int c = 0; c < 7; c++) {
                Date date = cal.getTime();
                boolean isCurrentMonth = cal.get(MONTH) == month.getMonth();
                boolean isSelected = isCurrentMonth && containsDate(selectedCals, cal);
                boolean isSelectable =
                        isCurrentMonth && betweenDates(cal, minCal, maxCal) && isDateSelectable(date);
                boolean isToday = sameDate(cal, today);
                boolean isHighlighted = containsDate(highlightedCals, cal);
                int value = cal.get(DAY_OF_MONTH);

                RangeState rangeState = RangeState.NONE;
                if (selectedCals.size() > 1) {
                    if (sameDate(minSelectedCal, cal)) {
                        rangeState = RangeState.FIRST;
                    } else if (sameDate(maxDate(selectedCals), cal)) {
                        rangeState = RangeState.LAST;
                    } else if (betweenDates(cal, minSelectedCal, maxSelectedCal)) {
                        rangeState = RangeState.MIDDLE;
                    }
                }

                weekCells.add(
                        new MonthCellDescriptor(date, isCurrentMonth, isSelectable, isSelected, isToday,
                                isHighlighted, value, rangeState));
                cal.add(DATE, 1);
            }
        }
        return cells;
    }

    private boolean isDateSelectable(Date date) {
        return dateConfiguredListener == null || dateConfiguredListener.isDateSelectable(date);
    }

    private boolean doSelectDate(Date date, MonthCellDescriptor cell) {
        Calendar newlySelectedCal = Calendar.getInstance(locale);
        newlySelectedCal.setTime(date);
        // Sanitize input: clear out the hours/minutes/seconds/millis.
        setMidnight(newlySelectedCal);

        // Clear any remaining range state.
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        switch (selectionMode) {
            case RANGE:
                if (selectedCals.size() > 1) {
                    // We've already got a range selected: clear the old one.
                    clearOldSelections();
                } else if (selectedCals.size() == 1 && newlySelectedCal.before(selectedCals.get(0))) {
                    // We're moving the start of the range back in time: clear the old start date.
                    clearOldSelections();
                }
                break;

            case MULTIPLE:
                date = applyMultiSelect(date, newlySelectedCal);
                break;

            case SINGLE:
                clearOldSelections();
                break;
            default:
                throw new IllegalStateException("Unknown selectionMode " + selectionMode);
        }

        if (date != null) {
            // Select a new cell.
            if (selectedCells.size() == 0 || !selectedCells.get(0).equals(cell)) {
                selectedCells.add(cell);
                cell.setSelected(true);
            }
            selectedCals.add(newlySelectedCal);

            if (selectionMode == SelectionMode.RANGE && selectedCells.size() > 1) {
                // Select all days in between start and end.
                Date start = selectedCells.get(0).getDate();
                Date end = selectedCells.get(1).getDate();
                selectedCells.get(0).setRangeState(RangeState.FIRST);
                selectedCells.get(1).setRangeState(RangeState.LAST);

                for (List<MonthCellDescriptor> week : mCells) {
                    for (MonthCellDescriptor singleCell : week) {
                        if (singleCell.getDate().after(start)
                                && singleCell.getDate().before(end)
                                && singleCell.isSelectable()) {
                            singleCell.setSelected(true);
                            singleCell.setRangeState(RangeState.MIDDLE);
                            selectedCells.add(singleCell);
                        }
                    }
                }
            }
        }

        // Update the adapter.
        mMonthView.init(mMonth, mCells, false);
        return date != null;
    }

    private void clearOldSelections() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            // De-select the currently-selected cell.
            selectedCell.setSelected(false);
        }
        selectedCells.clear();
        selectedCals.clear();
    }

    private static String dbg(Date minDate, Date maxDate) {
        return "minDate: " + minDate + "\nmaxDate: " + maxDate;
    }

    private Date applyMultiSelect(Date date, Calendar selectedCal) {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            if (selectedCell.getDate().equals(date)) {
                // De-select the currently-selected cell.
                selectedCell.setSelected(false);
                selectedCells.remove(selectedCell);
                date = null;
                break;
            }
        }
        for (Calendar cal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                selectedCals.remove(cal);
                break;
            }
        }
        return date;
    }

    private class CellClickedListener implements MonthView.Listener {
        @Override public void handleClick(MonthCellDescriptor cell) {
            Date clickedDate = cell.getDate();

            if (betweenDates(clickedDate, minCal, maxCal) && isDateSelectable(clickedDate)) {
                boolean wasSelected = doSelectDate(clickedDate, cell);

                if (dateListener != null) {
                    if (wasSelected) {
                        dateListener.onDateSelected(clickedDate);
                    } else {
                        dateListener.onDateUnselected(clickedDate);
                    }
                }
            }
        }

        @Override
        public void prevClick() {
            Calendar calendar = new GregorianCalendar(locale);
            calendar.setTime(mMonth.getDate());
            calendar.add(Calendar.MONTH, -1);
            if(betweenDates(calendar, minCal, maxCal) ||
                    minCal.get(MONTH) == calendar.get(MONTH) && minCal.get(YEAR) == calendar.get(YEAR)) {
                changeMonth(calendar);
            }
        }

        @Override
        public void nextClick() {
            Calendar calendar = new GregorianCalendar(locale);
            calendar.setTime(mMonth.getDate());
            calendar.add(Calendar.MONTH, 1);
            changeMonth(calendar);
        }
    }

}
