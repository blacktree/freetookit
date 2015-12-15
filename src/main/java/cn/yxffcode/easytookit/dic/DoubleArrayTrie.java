package cn.yxffcode.easytookit.dic;

import cn.yxffcode.easytookit.collection.IntIterator;
import cn.yxffcode.easytookit.collection.IntStack;
import cn.yxffcode.easytookit.lang.IntsRef;
import cn.yxffcode.easytookit.lang.StringIntsRef;
import cn.yxffcode.easytookit.participle.AlphabetTransformer;

import static cn.yxffcode.easytookit.utils.ArrayUtils.grow;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 双数组字典树,使用int数组实现
 * <p/>
 * 实现原理:
 * base[s] + c = t;
 * check[t] = s;
 * <p/>
 * 1.base与check是两个平行的数组,数组的下标表示状态
 * 2.base数组与check数组表示节点之间的父子关系
 * 3.base[s]的值表示状态s的子节点在check数组中的起始下标(但起始下标不一定存的是s的子节点)
 * 4.check[t]表示状态t的父节点状态
 * 4.输入c后,状态由s转成t,新的状态,那么check[t]=s,状态s增加一个子节点base[s] + c且满足关系t = base[s] + c
 * <p/>
 * 创建过程:
 * base数组的起始值都为0,初始状态为1,check数组的初始值为0,表示没有父节点
 * 输入字符c,则t = base[s] + c,检查check[t]的值,如果check[t]==s,则表示此字符在公共前缀中,状态转换成t,如果check[t]==0
 * 表示t这个状态还没有被加入到字典中,则check[t]=s,状态转换成t,如果check[t]!=0且check[t]!=s,则表示check[t]已经被其它状态
 * 占用,那么修改base[s]的值为b,使修改前,所有的check[b + m]=0(check数组元素没有被其它节点占用)
 * 假设s状态后有输入为n,那么修改后check[b+n] = check[base[s] + n],base[b + n] = base[base[s] + n],然后修改base[s]+n
 * 的所有子节点,使用其check值为b+n.
 * <p/>
 * base       check
 * -------    -------
 * |     |    |     |
 * -------    -------
 * |  0  |s   |     | s
 * -------    -------
 * |  0  |t   |  s  | t
 * -------    -------
 * |     |    |     |
 * -------    -------
 * |     |    |  t  | m
 * -------    -------
 * |     |    |     |
 * -------    -------
 * |     |    |     |
 * -------    -------
 * 如果b=1,则base[s]由0变为1,那么t的值则变成t+1,那么check[t+1]=check[t],check[t]=0
 * base[t]变成base[t+1],base[t+1]=base[t]
 * t的子节点m也发生变动check[m]=t+1
 * 最后base[s]=1
 * <p/>
 * 内存存储方面,需要对字母表做处理,{@link #alphabetTransformer}用作奖字符转换成int类型的数据(apply方法中可返回IntsRef的包装类),
 * 默认只处理英文大小写,数字和汉字,其它字符跳过(转换成int后小于或者等于0).默认的方式将终止符转换成1,数字转换成2~11,小写字母转换成12~37,大写英文字母
 * 转换成38~73,汉字大于74(汉字转换成int后数值满园在[{@link DefaultAlphabetTransformer#CN_CHAR_FIRST},{@link
 * DefaultAlphabetTransformer#CN_CHAR_LAST}]
 * 之间,做字母表的转换可减小字母的数值,从而减小base与check两个数组的大小.
 *
 * @author gaohang on 15/12/9.
 */
public class DoubleArrayTrie implements Dictionary {
  private static final int  INIT_ARRAY_SIZE = 50;
  private static final int  INIT_STATE      = 1;
  private static final char END_INPUT       = '#';
  /**
   * 表示已经没有后续状态了
   *
   * @see #nextState(int, int)
   */
  private static final int  NO_SUCH_STATE   = - 1;
  /**
   * 默认的check值
   */
  private static final int  NONE            = 0;
  private final AlphabetTransformer alphabetTransformer;
  private       int[]               base;
  private       int[]               check;

  private DoubleArrayTrie(final AlphabetTransformer alphabetTransformer) {
    this.alphabetTransformer = alphabetTransformer;
    this.base = new int[INIT_ARRAY_SIZE];
    this.check = new int[INIT_ARRAY_SIZE];
  }

  public static DoubleArrayTrie create(Iterable<String> words) {
    DoubleArrayTrie trie = new DoubleArrayTrie(DefaultAlphabetTransformer.INSTANCE);
    for (String word : words) {
      trie.add(word);
    }
    return trie;
  }

  /**
   * 添加一个词条
   */
  public void add(String word) {
    checkNotNull(word);
    IntsRef intsRef = new StringIntsRef(word + END_INPUT);
    for (int i = 0, j = intsRef.length(), s = INIT_STATE; i < j; i++) {
      if (s >= base.length) {
        base = grow(base, s * 2);
      }
      int elem = alphabetTransformer.wrap(intsRef.element(i));
      if (elem <= 0) {
        continue;
      }
      int t = base[s] + elem;
      if (t >= check.length) {
        check = grow(check, t * 2);
      }
      if (check[t] != NONE && check[t] != s) {
        //冲突,需要重新分配base[s]
        IntStack children = new IntStack();
        for (int k1 = base[s]; k1 < check.length; k1++) {
          int e = check[k1];
          if (e == s) {
            children.push(k1 - base[s]);
          }
        }
        int b = 1;
        base:
        for (; ; b++) {//b表示新的base[s]
          for (IntIterator iterator1 = children.iterator(); iterator1.hasNext(); ) {
            //t = base[s] + c,则c = t - base[s],此时t是child
            int c = iterator1.next();
            int nt1 = b + c;
            if (nt1 >= check.length) {
              check = grow(check, nt1 * 2);
            }
            if (check[nt1] != NONE) {
              continue base;
            }
          }
          if (check[b + elem] == NONE) {
            break;
          }
        }
        //找到了满足条件的b,做节点的转移
        for (IntIterator iterator = children.iterator(); iterator.hasNext(); ) {
          int child = iterator.next();
          //base[s] + child = t;
          int nt = b + child;
          int ot = child + base[s];
          check[nt] = check[ot];
          check[ot] = NONE;
          base[nt] = base[ot];

          for (int k = 0, m = check.length; k < m; k++) {
            if (check[k] == ot) {
              check[k] = nt;
            }
          }
        }
        base[s] = b;
        t = base[s] + elem;
      }
      check[t] = s;
      s = t;
    }
  }

  public static DoubleArrayTrie create(Iterable<String> words, AlphabetTransformer transformer) {
    DoubleArrayTrie trie = new DoubleArrayTrie(transformer);
    for (String word : words) {
      trie.add(word);
    }
    return trie;
  }

  @Override
  public boolean match(String word) {
    checkNotNull(word);
    IntsRef intsRef = new StringIntsRef(word + END_INPUT);
    for (int i = 0, j = intsRef.length(), s = INIT_STATE; i < j; i++) {
      if (s >= base.length) {
        return false;
      }
      int c = alphabetTransformer.wrap(intsRef.element(i));
      int t = base[s] + c;
      if (t >= check.length) {
        return false;
      }
      if (t >= check.length) {
        return false;
      }
      if (check[t] == s) {
        s = t;
      } else {
        return false;
      }
    }
    return true;
  }

  @Override
  public int startState() {
    return INIT_STATE;
  }

  @Override
  public int nextState(final int state, final int input) {
    if (state >= base.length) {
      return NO_SUCH_STATE;
    }
    int next = base[state] + alphabetTransformer.wrap(input);
    return next < check.length && check[next] == state ? next : NO_SUCH_STATE;
  }

  @Override
  public boolean isWordEnded(final int state) {
    return nextState(state, END_INPUT) != alphabetTransformer.unwrap(NO_SUCH_STATE);
  }

  public enum DefaultAlphabetTransformer implements AlphabetTransformer {
    INSTANCE;
    /**
     * 中文字符的起始数字
     */
    private static final int CN_CHAR_FIRST    = 19968;
    /**
     * 中文字符的最大字符数字
     */
    private static final int CN_CHAR_LAST     = 171941;
    /**
     * 结束符的int值
     */
    private static final int END_INPUT_VALUE  = 1;
    /**
     * 数字起始
     */
    private static final int DIGIT_START      = 2;
    /**
     * 小写字符起始
     */
    private static final int LOW_CASE_START   = 12;
    /**
     * 大写字符起始
     */
    private static final int UPPER_CASE_START = 38;
    /**
     * 汉字起始
     */
    private static final int CN_CHAR_START    = 74;

    public int wrap(final int elem) {
      if (elem >= 'a' && elem <= 'z') {
        return LOW_CASE_START + (elem - 'a');
      } else if (elem >= 'A' && elem <= 'Z') {
        return UPPER_CASE_START + (elem - 'A');
      } else if (elem >= CN_CHAR_FIRST && elem <= CN_CHAR_LAST) {
        return CN_CHAR_START + (elem - CN_CHAR_FIRST);
      } else if (elem >= '0' && elem <= '9') {
        return DIGIT_START + (elem - '0');
      } else if (elem == '#') {
        return END_INPUT_VALUE;
      }
      //不接受此字符
      return - 1;
    }

    public int unwrap(int c) {
      if (c < DIGIT_START) {
        return c;
      }
      if (c >= DIGIT_START && c < LOW_CASE_START) {
        return c - DIGIT_START + '0';
      }
      if (c >= LOW_CASE_START && c < UPPER_CASE_START) {
        return c - LOW_CASE_START + 'a';
      }
      if (c >= UPPER_CASE_START && c < CN_CHAR_START) {
        return c - UPPER_CASE_START + 'A';
      }
      return c - CN_CHAR_START + CN_CHAR_FIRST;
    }

  }
}
