package cn.yxffcode.freetookit.lang;

import java.util.Calendar;

/**
 * 表示日期的域(年,月,日)的枚举,在{@link Calendar}类中,年/月/日三个域都是使用整型常量表示,意思不明确,且容易出错
 *
 * @author gaohang on 15/11/29.
 */
public enum DateField {
  YEAR {
    @Override public int getCalendarField() {
      return Calendar.YEAR;
    }
  },
  MOUTH {
    @Override public int getCalendarField() {
      return Calendar.MONTH;
    }
  },
  DAY {
    @Override public int getCalendarField() {
      return Calendar.DAY_OF_MONTH;
    }
  };

  public abstract int getCalendarField();
}
