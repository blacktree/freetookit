package cn.yxffcode.freetookit.io;

import cn.yxffcode.freetookit.collection.ImmutableIterator;
import cn.yxffcode.freetookit.lang.CloseableIterable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 输入输出流相关的工具
 *
 * @author gaohang on 15/10/23.
 */
public final class IOStreams {
  private IOStreams() {
  }

  /**
   * 按行读取
   * <p>
   * 使用lazy的读，只有在返回的Iterable对象上迭代一次才会读一行
   * <p>
   * 当读取的文件比较大时，不合适一次将事个文件的行读入内存，需要逐行读并处理。在使用guava的CharStreams时比较繁锁，例如：
   * <pre>
   *      CharStreams.readLine(reader, new LineProcessor<T>() {
   *          public boolean processLine(String line) throws IOException {
   *              if (line ....) {
   *                  return false;//不需要再继续读
   *              }
   *              ...
   *          }
   *          public T getResult() {
   *              return xxx;
   *          }
   *      });
   * </pre>
   * 换成此方法，代码如下：
   * <pre>
   *     for (String line : IOStreams.lines(reader)) {
   *         if (line...) {
   *             break;//停止读，因为是lazy的，当前行处理结束前，下一行不会被读入到内存
   *         }
   *         ...
   *     }
   * </pre>
   */
  public static Iterable<String> lines(final BufferedReader reader) {
    checkNotNull(reader);
    return new Iterable<String>() {
      @Override public Iterator<String> iterator() {
        return new ImmutableIterator<String>() {

          private String line;

          @Override public boolean hasNext() {
            try {
              return (line = reader.readLine()) != null;
            } catch (IOException e) {
              throw new IOReaderException(e);
            }
          }

          @Override public String next() {
            return line;
          }
        };
      }
    };
  }

  public static CloseableIterable<String> lines(final File src) {

    return new CloseableIterable<String>() {

      private BufferedReader in;

      @Override public void close() {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            throw new IOReaderException(e);
          }
        }
      }

      @Override public Iterator<String> iterator() {
        try {
          in = new BufferedReader(new FileReader(src));
          return lines(in).iterator();
        } catch (FileNotFoundException e) {
          throw new IOReaderException(e);
        }
      }
    };
  }

  public static BufferedReader toBufferedReader(InputStream in) {
    return new BufferedReader(new InputStreamReader(in));
  }

  public static BufferedReader openClasspath(String classpath) {
    return toBufferedReader(IOStreams.class.getResourceAsStream(classpath));
  }

}
