# 背景
随着交易业务和基础知识的沉淀，愈发觉得扩展点可以在大型交易分布式架构中可以做更多的事情。

经过一个月的思考，决定将 **单点领域扩展点（savior-ext）** 从原有的 **savior架构** 中剥离开来，升级成为 **分布式领域扩展点（sext）** ，之后单独去维护和迭代。

# 坐标
官网：[轻量级分布式技术解决方案框架 - Savior](http://savior.sunjinxin.cn/)

分布式领域扩展点（新 · 完全兼容上一个大版本 · 可直接替换坐标）
```xml
<dependency>
    <groupId>cn.sunjinxin.savior</groupId>
    <artifactId>sext</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

领域扩展点（旧）
```xml
<dependency>
    <groupId>cn.sunjinxin.savior</groupId>
    <artifactId>savior</artifactId>
    <version>1.0.5</version>
</dependency>
```

# 设计理念
作为一名工程师，我深知一个框架引入到系统中会带来很大的便利，但同时也会让系统设计和架构升级以及业务迭代变得更加复杂。

所以该分布式领域扩展点框架具备以下几种特性
1. 轻量化（引入轻量、API轻量，达到一键启动）
2. 可插拔（防止应用架构变得沉重，业务点可随时一键剔除，一键替换）
3. 可编排（可达到工作流的编排能力，对业务能够达到充分的抽象化）
4. 可配置（基于配置中心能力，对框架所有能力和服务动态配置）
5. 可灰度（可通过对流量进行染色，设置流量权重，达到灰度可能）
6. 可监控（对框架所覆盖的业务指标和相关基础设施指标进行监控）
7. 可降级（对编排的扩展点进行熔断降级，提高系统的可用性）
8. 可扩展（基本的业务扩展能力，以及供开发人员在应用层扩展框架的能力）

# 设计图
![在这里插入图片描述](https://i-blog.csdnimg.cn/direct/39df3a28333d42f5a6d322c539429b75.png)

# Quick Start
在启动类上标注@Savior注解，即可启动Savior框架所有组件的功能。

```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import cn.sunjinxin.savior.core.anno.Savior;

/**
 * @author issavior
 * @date 1314/05/20 00:00:00
 */
@Savior
@SpringBootApplication
public class AppRun {
    public static void main(String[] args) {
        SpringApplication.run(AppRun.class, args);
    }
}
```
# 编排能力
## 前言
有这样的一种场景：针对于一次会话，同时会调很多外部服务，同时这些RPC服务会有多种直接或间接的关系，是否有更高效的方式能够让我们的一次会话时间变得更高效，同时也能够保证系统的相对稳定性呢？

## 设计思想
如果开发者在设计之初，对于领域边界以及子域能力划分比较清晰，那我们编排的业务扩展点就不会杂乱无章。

在SpringBoot启动时，伴随着IOC容器的初始化，领域扩展点容器也随之完成全量向量视图初始化、向量深度视图初始化、拓扑排序之后的容器初始化。

基于动态线程池思想，可以通过扩展点容器中的编排任务，动态调整线程池？根据拓扑排序后的结果，异步执行编排的任务，完成召回、过滤以及核心业务逻辑。

## 设计图
![在这里插入图片描述](https://i-blog.csdnimg.cn/direct/97ed43dbaacf48958993e6fca4954f36.png)
## 详细设计
1. 容器初始化时，从云端配置中心拉取扩展点编排配置文件，本地编排配置文件可以做兜底，也可以调节两者权重
2. 拿到编排数据源之后，渲染本地向量广度视图以及向量深度视图
3. 通过Kahn算法，将两个视图清洗成拓扑排序之后的容器
4. 通过权衡算法，初始化动态线程池，并预热核心线程
5. 将上述操作数据上传到我们的扩展点监控中心
6. 通过监控水位线，可动态配置线程池相关核心参数
7. 当一次会话开始时，会通过拓扑排序，并发去执行扩展点任务
8. 通过向量视图，可以针对所有扩展点做召回和过滤处理
## 核心算法伪代码
```java
/**
 * Kahn算法
 *
 * @author issavior
 */
public class KahnTopologicalSort {

    /**
     * 向量广度视图
     */
    private final Map<Integer, List<Integer>> adjList = new HashMap<>();

    /**
     * 向量深度视图
     */
    private final Map<Integer, Integer> inDegree = new HashMap<>();

    /**
     * 拓扑排序结果
     */
    private final List<Integer> topoOrder = new ArrayList<>();


    /**
     * 渲染向量视图和向量深度
     *
     * @param u 向量头
     * @param v 向量尾
     */
    public void addEdge(int u, int v) {
        // 渲染视图
        adjList.putIfAbsent(u, new ArrayList<>());
        adjList.get(u).add(v);
        // 更新入度  
        inDegree.put(v, inDegree.getOrDefault(v, 0) + 1);
        inDegree.putIfAbsent(u, 0);

    }

    /**
     * 拓扑排序
     *
     * @return 业务节点顺序
     */
    public List<Integer> topologicalSort() {
        Queue<Integer> queue = new LinkedList<>();

        // 将所有入度为0的节点加入队列  
        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            topoOrder.add(current);

            // 遍历当前节点的所有邻接点  
            for (int neighbor : adjList.getOrDefault(current, Collections.emptyList())) {
                // 减少邻接点的入度  
                int newInDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newInDegree);

                // 如果邻接点的入度变为0，则加入队列  
                if (newInDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // 检查是否所有节点都被访问过，若为有环图，初始化报错
        if (topoOrder.size() != inDegree.size()) {
            throw new IllegalStateException("Graph has a cycle and cannot be topologically sorted.");
        }

        return topoOrder;
    }
}
```



# 相关组件

Savior框架中的组件亦可以独立引入，目前支持的组件：

|组件|说明  |
|--|--|
| savior-ext |扩展点  |
| savior-mq |消息队列  |
| savior-toc |超时中心  |
| savior-rule |规则引擎  |
| savior-lock |分布式锁  |
| savior-retry |重试机制  |
| savior-event |事件总线  |
| savior-cache |多级缓存  |
| savior-workflow |工作流  |
| ...... |......  |

