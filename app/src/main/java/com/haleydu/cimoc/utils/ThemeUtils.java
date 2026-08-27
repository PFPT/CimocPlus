package com.haleydu.cimoc.utils;

import android.content.Context;
import android.util.TypedValue;

import com.haleydu.cimoc.R;

public class ThemeUtils {

    public static final int THEME_GUOFENG = 6;

    public static int getResourceId(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    public static int getThemeById(int id) {
        return R.style.AppThemeGuofeng;
    }

}
