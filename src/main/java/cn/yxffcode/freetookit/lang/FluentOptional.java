package cn.yxffcode.freetookit.lang;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * 流式的{@link Optional}
 * <p>
 * 使用方式:<pre>
 *     {@code
 *          T result = FluentOptional.from(obj)
 *                                   .flow(i -> i.xxx())
 *                                   .flow(i -> i.yyy())
 *                                   .flow(i -> i.zzz())
 *                                   .or(defaultValue);
 *     }
 * </pre>
 * 在整个过程中不需要担心会发生NPE
 *
 * @author gaohang on 15/11/30.
 */
public class FluentOptional<T> {

  private static final FluentOptional<Object> ABSENT = from(Optional.absent());
  private final Optional<? extends T> optional;

  public FluentOptional(final Optional<? extends T> optional) {
    this.optional = optional;
  }

  public static <T> FluentOptional from(T obj) {
    return new FluentOptional(Optional.fromNullable(obj));
  }

  public static <T> FluentOptional from(Optional<? extends T> src) {
    return new FluentOptional(src);
  }

  public T or(T def) {
    return isPresent() ? get() : def;
  }

  public boolean isPresent() {
    return optional.isPresent();
  }

  public T get() {
    return optional.get();
  }

  /**
   * 将此Optional传递,表示一次方法调用
   */
  public <A> FluentOptional<A> flow(Function<T, A> function) {
    if (!isPresent()) {
      return (FluentOptional<A>) ABSENT;
    }
    A result = function.apply(get());
    if (result == null) {
      return (FluentOptional<A>) ABSENT;
    }
    return from(Optional.of(result));
  }

}
