// Copyright 2012 Square, Inc.
package com.squareup.timessquare;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

/** TableRow that draws a divider between each cell. To be used with {@link CalendarGridView}. */
public class CalendarRowView extends ViewGroup implements View.OnClickListener {
  private boolean isHeaderRow;
  private MonthView.Listener listener;
  private int cellSize;

  public CalendarRowView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override public void addView(View child, int index, ViewGroup.LayoutParams params) {
    child.setOnClickListener(this);
    super.addView(child, index, params);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    long start = System.currentTimeMillis();
    final int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
    cellSize = totalWidth / 7;
    int cellWidthSpec = makeMeasureSpec(cellSize, EXACTLY);
    int cellHeightSpec = isHeaderRow ? makeMeasureSpec(cellSize, AT_MOST) : cellWidthSpec;
    int rowHeight = 0;
    for (int c = 0, numChildren = getChildCount(); c < numChildren; c++) {

      final View child = getChildAt(c);

        LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
        int childWidthSpec = makeMeasureSpec(cellSize * layoutParams.cols, EXACTLY);

      child.measure(childWidthSpec, cellHeightSpec);
      // The row height is the height of the tallest cell.
      if (child.getMeasuredHeight() > rowHeight) {
        rowHeight = child.getMeasuredHeight();
      }
    }
    final int widthWithPadding = totalWidth + getPaddingLeft() + getPaddingRight();
    final int heightWithPadding = rowHeight + getPaddingTop() + getPaddingBottom();
    setMeasuredDimension(widthWithPadding, heightWithPadding);
    Logr.d("Row.onMeasure %d ms", System.currentTimeMillis() - start);
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    long start = System.currentTimeMillis();
    int cellHeight = bottom - top;
    for (int c = 0, offset = 0, numChildren = getChildCount(); c < numChildren; c++) {
      final View child = getChildAt(c);

        LayoutParams params = (LayoutParams) child.getLayoutParams();

      child.layout(offset * cellSize, 0, (offset + params.cols) * cellSize, cellHeight);

        offset += params.cols;
    }
    Logr.d("Row.onLayout %d ms", System.currentTimeMillis() - start);
  }

    public boolean isHeaderRow() {
        return isHeaderRow;
    }

  public void setIsHeaderRow(boolean isHeaderRow) {
    this.isHeaderRow = isHeaderRow;
  }

  @Override public void onClick(View v) {
    // Header rows don't have a click listener
    if (listener != null) {
      listener.handleClick((MonthCellDescriptor) v.getTag());
    }
  }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && p instanceof LayoutParams;
    }

    public void setListener(MonthView.Listener listener) {
    this.listener = listener;
  }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        public int cols = 1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CalendarRowView_Layout);

            try {
                cols = a.getInt(R.styleable.CalendarRowView_Layout_cols, 1);
            } finally {
                a.recycle();
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
