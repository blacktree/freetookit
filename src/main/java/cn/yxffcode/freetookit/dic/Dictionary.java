package cn.yxffcode.freetookit.dic;

import java.io.Serializable;

/**
 * 字典的实现
 *
 * @author gaohang on 15/12/9.
 */
public interface Dictionary extends Serializable {

  boolean match(String word);

  /**
   * 起始状态
   */
  int startState();

  /**
   * 状态的转换
   */
  int nextState(int state, int input);

  /**
   * 当前状态是否表示一个词的结尾
   */
  boolean isWordEnded(int state);

}
