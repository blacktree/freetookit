# freetookit
常用工具类

对一些常用功能的抽象以及可能会用到的基础代码，目的在于简化工程代码，让实现基础功能更简单，可读性更好

部分类的使用示例:

## 方法的lazy调用 
Java语法不支持lazy,如果想使用lazy调用，可使用@Lazy标记方法，并通过LazyProxy创建代理，使用方法如下:
```java
public interface TestService {
  @Lazy
  TestBean info();
}
```
```java
public class Test {
  
  public void create_jdk() throws Exception {
    final TestService testService = LazyProxy.newInstance(TestService.class, new Supplier<TestService>() {
      @Override
      public TestService get() {
        return new TestServiceImpl();
      }
    });
    TestBean bean = testService.info();//lazy
  }

}
```
## FluentOptional 
流式的Optional,需要多次判断空的,代码中需要多个if-else,即使使用Optional,例如:
```java 
World world = xxx;
Country country = world.getCountry("cn");
if (country != null) {
    Province province = country.getProvince("bj");
    if (province != null) {
        City city = province.getCity("bj");
        if (city != null) {
            Region region = city.getRegion("hd");
            if (region != null) {
                return region.getCode();
            }
        }
    }
}
return defaultCode;
```
代码非常的繁琐,FluentOptional可减少大量的判断,在配合lambda的时候,FluentOptional使用更加简便:
```java 
World world = xxx;
return FluentOptional.from(world)
        .flow(world -> world.getCountry("cn"))
        .flow(country -> country.getProvince("bj"))
        .flow(province -> province.getCity("bj"))
        .flow(city -> city.getRegion("hd"))
        .flow(region -> region.getCode())
        .or(defaultCode);
```

## TextTemplate 
简单的字符串模板功能,提供mybatis风格的placeholder,支持转义 
```java
final TextTemplate textTemplate = new TextTemplate("select * from }\\\\\\\\#{tableName} where");
final StringRef ref = textTemplate.rend(Collections.singletonMap("tableName", "hahatest"));
```

## MpscLinkedQueue
针对多线程生产,单线程消费的无锁队列,类似于ConcurrentLinkedQueue,但MpscLinkedQueue在消费端只能支持单线程,
如果使用多线程消费则是线程不安全的

## CopyOnWriterMap与CopyOnWriterHashSet
类似CopyOnWriterArrayList,使用copy-on-write的方式优化并发下的读性能

## GroupList与GroupIterable 
用于将多个List转换成一个List,与使用ArrayList不同的是,GroupList的创建开销非常小,
没有真正的创建一个新的ArrayList,它只是为多个List提供一个视图,类似的还有GroupIterable 

## IntStack
专为int类型的数据实现的栈,没有装箱与拆箱

```java 
public class GroupListTest {

    @Test
    public void test() {
        List<Integer>[] lists = xxx;
        GroupList<Integer> ints = GroupList.create(lists);
    }
}
```

## SingeValueList与SingleValueIterator 
当需要创建一个List,此List中所有元素都是一样时可使用SingleValueList代替ArrayList 
构造器如下: 

```java 

  public SingleValueList(int size, E value) { 
    this.size = size; 
    this.value = value; 
  } 

```

## MultiTable 
当需要使用
```java
Table<R, C, Collection<V>>
```
时,可以使用MultiTable,利用MultiTables创建MultiTable对象 
```java 
MultiTable<R, C, V> multiTable = MultiTables.newHashMultiTable();
```

## MappingMap 
为普通对象提供基于Map接口的访问属性的方式,例如使用spring-jdbc中的NamedParameterJdbcTemplate时,需要使用Map作为参数,
而DAO接口使用的是POJO,MappingMap可提供便利的方式让POJO适配Map接口 

```java 

Map<String, Object> params = MappingMap.fromNotNull(obj); 

```

## IterTransMap 
通过将Iterable对象中的元素转换成Map,转换后的map的key为指定属性值,value为对象本身 
可以使用HashBasedIterTransMap或者TreeBasedIterTransMap 

## HashBuildingMap 
通过将Iterable转换成Map,map中的key和value都为指定属性 

## ConcurrentSet 
并发安全的Set,不需要在使用的时候做上锁处理 

## ConcurrentTable 
并发安全的Table

## CountDownRunner 
类似于CountDownLatch,但不同的是CountDownRunner在计数变成0后执行一个任务 

```java 
CountDownRunner countDownRunner = new CountDownRunner(10, new Runnable() {
    @Override public void run() {
        //do something when count is zero.
    }
});
```

## RedisSpinLock 
利用redis实现的分布式锁,但实现过程没有考虑到redis的宕机带来的问题
```java 
RedisSpinLock lock = RedisSpinLock.newBuilder(jedisPool, "bookingIdLock")
                                        .setLockFailedWaiting(10)
                                        .setExpireSeconds(10)
                                        .create();
```
## DCL 
用于封装双重校验锁,使用双重校验锁的代码示例:
```java
private Map<String, Object> cache = xxx;
public Object getInstance(String key) {
 if (!cache.containsKey(key)) {
   synchronized(this) {
     if (!cache.containsKey(key)) {
       //create object and put to cache
       xxx
     }
   }
 }
}
```
换成DCL后:
```java 
private Map<String, Object> cache = xxx;
private DCL<String> dcl = DCL.<String>create().check(key -> cache.containsKey(key)).absent(key -> {//create Object and put to cache});

public Object getInstance(String key) {
  return dcl.done(key);
}
``` 

## ThreadGroupExecutor
利用MpscLinkedQueue实现的Executor,其中每个线程一个队列,利用轮循的方式选择线程添加任务,没有使用work-stealing算法,
如果任务耗时差异比较大,会有线程负载不均衡的问题.没有使用阻塞,如果队列长时间为空,会比较耗cpu
```java
 ThreadGroupExecutor exec = new ThreadGroupExecutor(Runtime.getRuntime().availableProcessors());
 for (int i = 0; i < 100; i++) {
   exec.execute(new Runnable() {
     @Override
     public void run() {
       System.out.println(Thread.currentThread());
     }
   });
 }
 exec.shutdown();
```

## IOStreams 
在文件比较大时,可能需要按行读按行处理,但是这样带来的问题在于将读取文件的代码与处理数据的代码混在了一起,
可以使用Guava将它分开,例如:
```java
CharStreams.readLine(reader, new LineProcessor<T>() {
  public boolean processLine(String line) throws IOException {
      if (line ....) {
          return false;//不需要再继续读
      }
      ...
  }
  public T getResult() {
      return xxx;
  }
});
```
这样代码比较繁琐,不自然,使用IOStreams的代码更自然:
```java 
for (String line : IOStreams.lines(reader)) {
    if (line...) {
        break;//停止读，因为是lazy的，当前行处理结束前，下一行不会被读入到内存
    }
    ...
}
```
IOStreams使用的lazy的方式读取文件,在使用上和一次读取所有内容到一个List再分别处理List中的元素是一样

## DirectoryWrapper 
对目录做删除时,需要删除此目录中所有的文件和子目录,将这个过程封装成DirectoryWrapper:
```java 
DirectoryWrapper dir = DirectoryWrapper.wrap(new File("/user/test"));
dir.delete();
```

## CalendarWrapper
Calendar类的很多方法语义不明,此类用于包装Calendar,目前只提供了日期相加的功能:
```java
CalendarWrapper calendar = CalendarWrapper.getInstance();
calendar.add(DateField.DAY, 1);
```

## CloseableIterable
可被关闭的Iterable对象,例如按行读取并处理文件,在处理结束后关闭:
```java
try (CloseableIterable<String> lines = IOStreams.lines(new File("/user/test"))) {
    for (String line : lines) {
        System.out.println(line);
    }    
}
```

## ModifiableInteger/ModifiableLong 
Integer和Long这类对象是不能修改的,ModifiableInteger/ModifiableLong提供了可修改的Integer和Long

## ReverseStringBuilder 
与普通的StringBuilder不同的是,ReverseStringBuilder将append后的数据从反方向创建String
```java 
ReverseStringBuilder sb = new ReverseStringBuilder();
String s = sb.append('a').append('b').append('c').toString();
//s的值是"cba"
```

## UnrepeatableInvokeProxy 
用于防止同一时刻的重复调用,例如防止重复提交 
如果是单机应用:
```java
 Object proxy = UnrepeatableInvokeProxy.getProxy(target);
```
如果是多机应用,以redis为例:
```java 
UnrepeatableInvokeProxy.getProxy(obj, new InvocationContextHandler() {
    @Override public boolean set(final InvocationContext invocationContext) {
        //if in redis return false else
        //set to redis
        return true;//true if save exists
    }

    @Override public void remove(final InvocationContext invocationContext) {
        //remove from redis
    }
});
```
在target对象需要防止重复调用的方法上加上@Unrepeatable注释,并指定参数 
```java
 
 @Unrepeatable({1, 2})
 public void f(int a, int b, int c) {
 }
```
表示通过第二个和第三个参数做防止同一时刻重复调用

## FSLogQueue
将内存队列外移,使用文件作为队列
不给示例

## SpringBasedClassScanner
基于Spring,做类扫描
```java 
List<Class<?>> classes = SpringBasedClassScanner.getInstance()
                .doScan("com.xxxx.service", new Predicate<Class<?>>() {
                    @Override public boolean apply(final Class<?> type) {
                        return type.getAnnotation(SomeAnnotation.class) != null;
                    }
                });
```

## CharSequenceMatcher与CharsMatcher 
用于做字符串的匹配,区分于String.indexOf,这两个类使用了KMP算法,对于同一个模式串,可以使用KMP生成一次next数组,然后匹配多个目标串
```java 
CharSequenceMatcher charsMatcher = CharSequenceMatcher.create("hello").buildNextIfAbsent();
{
    int idx = charsMatcher.indexOf("hello world");
}
{
    int idx = charsMatcher.indexOf("hello everybody");
}
```

## DoubleArrayTrie 
双数组字典树,用作字典 
```java 
{
  List<String> words = Arrays.asList("limiku", "limika", "limikb",
                                            "limikc",
                                            "likla",
                                            "limlb",
                                            "mimik");
  DoubleArrayTrie trie = DoubleArrayTrie.create(words);

  for (String word : words) {
    assertTrue(trie.match(word));
  }
  //不存在的不能匹配成功
  assertFalse(trie.match("limi"));
}
{
  List<String> words = Arrays.asList("厘米网", "厘米库", "厘米百", "厘米米", "去哪儿", "百度");
  DoubleArrayTrie trie = DoubleArrayTrie.create(words);

  for (String word : words) {
    assertTrue(trie.match(word));
  }
  assertFalse(trie.match("厘"));
}
{
  List<String> words = Arrays.asList("qunar", "去哪儿");
  DoubleArrayTrie trie = DoubleArrayTrie.create(words);

  for (String word : words) {
    assertTrue(trie.match(word));
  }

}
```

## DictionaryTokenFilter 
基于字典的关键词过虑器 
```java 
DictionaryTokenFilter tokenFilter = new DictionaryTokenFilter(doubleArrayTrie);
Iterator<String> keyWords = tokenFilter.getMatched("xxx");
```

## TreePrefixSearcher与SortedArrayPrefixSearcher
针对量不是太大的词条,实现前缀搜索,可选择使用基于数组或者基于TreeSet的实现
```java
 TreePrefixSearcher searcher = new TreePrefixSearcher(items);
 Iterable<String> selectedItems = searcher.search("abc");
```
代码返回所有以"abc"开头(包括"abc")的词条

## GenericTrie
普通的字典树实现,可用于前缀搜索
```java 
GenericTrie searcher = new GenericTrie(items);
Iterable<String> selectedItems = searcher.search("abc");
```

## Automaton 
有穷自动机的实现 
```java 
DefaultAutomaton automaton = new DefaultAutomaton.DictionaryBuilder().addWord(
                                          new StringIntSequence("hello"))
                                          .addWord(new StringIntSequence("helle"))
                                          .addWord(new StringIntSequence("halle"))
                                          .addWord(new StringIntSequence("halla"))
                                          .addWord(new StringIntSequence("hallb"))
                                          .addWord(new StringIntSequence("abc"))
                                          .addWord(new StringIntSequence("dkk"))
                                          .addWord(new StringIntSequence("dkkk"))
                                          .build();
```

## NamedExtensionLoader 
java中自带的SPI加载器不能给服务类字义名字,此接口的实现用于加载带命名的服务实现类 
使用ExtensionLoaders类得到加载器

## PageResolver 
分页器,用于执行类似于分页查询的功能
```java 
Iterable<Integer> elems = new PageResolver<Integer>(pageSize) {
            @Override protected List<Integer> nextPage(final int off) {
                //query db for a page
            }
        }.getAll();
```

## MpscQueue
用于多生产者单消费者的线程安全的无锁队列,当有多个消费者时,非线程安全 
默认使用的线程数是cpu核心数
```java 
ThreadGroupExecutor exec = new ThreadGroupExecutor();
```
也可以指定线程数
```java
ThreadGroupExecutor exec = new ThreadGroupExecutor(nthread);
```
可以指定线程工厂 
```java 
ThreadGroupExecutor exec = new ThreadGroupExecutor(nthread, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
      }
    });
```
