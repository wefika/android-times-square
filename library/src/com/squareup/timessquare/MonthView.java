// Copyright 2012 Square, Inc.
package com.squareup.timessquare;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class MonthView extends LinearLayout implements View.OnClickListener {
    ImageButton mPrev;
    ImageButton mNext;
  TextView title;
  CalendarGridView grid;
  private Listener listener;

    private boolean mOnlyWeek = false;

  public static MonthView create(ViewGroup parent, LayoutInflater inflater,
      DateFormat weekdayNameFormat, Listener listener, Calendar today) {
    final MonthView view = (MonthView) inflater.inflate(R.layout.month, parent, false);

    final int originalDayOfWeek = today.get(Calendar.DAY_OF_WEEK);

    int firstDayOfWeek = today.getFirstDayOfWeek();
    final CalendarRowView headerRow = (CalendarRowView) view.grid.getChildAt(1);
    for (int offset = 0; offset < 7; offset++) {
      today.set(Calendar.DAY_OF_WEEK, firstDayOfWeek + offset);
      final TextView textView = (TextView) headerRow.getChildAt(offset);
      textView.setText(weekdayNameFormat.format(today.getTime()));
    }
    today.set(Calendar.DAY_OF_WEEK, originalDayOfWeek);
    view.listener = listener;
    return view;
  }

  public MonthView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

    public void setOnlyWeek(boolean onlyWeek) {
        if(mOnlyWeek != onlyWeek) {
            mOnlyWeek = onlyWeek;

        }
    }

    @Override protected void onFinishInflate() {
    super.onFinishInflate();
      mPrev = (ImageButton) findViewById(R.id.prev);
      mNext = (ImageButton) findViewById(R.id.next);
    title = (TextView) findViewById(R.id.title);
    grid = (CalendarGridView) findViewById(R.id.calendar_grid);

      mPrev.setOnClickListener(this);
      mNext.setOnClickListener(this);
  }

  public void init(MonthDescriptor month, List<List<MonthCellDescriptor>> cells,
      boolean displayOnly) {
    Logr.d("Initializing MonthView (%d) for %s", System.identityHashCode(this), month);
    long start = System.currentTimeMillis();
    title.setText(month.getLabel());

    final int numRows = 3;
    grid.setNumRows(numRows);
    for (int i = 0; i < 6; i++) {
      CalendarRowView weekRow = (CalendarRowView) grid.getChildAt(i + 2);
      weekRow.setListener(listener);
      if (i < numRows) {
        weekRow.setVisibility(VISIBLE);
        List<MonthCellDescriptor> week = cells.get(i);
        for (int c = 0; c < week.size(); c++) {
          MonthCellDescriptor cell = week.get(c);
          CalendarCellView cellView = (CalendarCellView) weekRow.getChildAt(c);

          cellView.setText(Integer.toString(cell.getValue()));
          cellView.setEnabled(cell.isCurrentMonth());
          cellView.setClickable(!displayOnly);

          cellView.setSelectable(cell.isSelectable());
          cellView.setSelected(cell.isSelected());
          cellView.setCurrentMonth(cell.isCurrentMonth());
          cellView.setToday(cell.isToday());
          cellView.setRangeState(cell.getRangeState());
          cellView.setHighlighted(cell.isHighlighted());
          cellView.setTag(cell);
        }
      } else {
        weekRow.setVisibility(GONE);
      }
    }
    Logr.d("MonthView.init took %d ms", System.currentTimeMillis() - start);
  }

    @Override
    public void onClick(View v) {
        if(listener != null) {
            int i = v.getId();
            if (i == R.id.prev) {
                listener.prevClick();

            } else if (i == R.id.next) {
                listener.nextClick();

            }
        }
    }

    public boolean getOnlyWeek() {
        return mOnlyWeek;
    }

    public interface Listener {
    void handleClick(MonthCellDescriptor cell);
      void prevClick();
      void nextClick();
  }
}
