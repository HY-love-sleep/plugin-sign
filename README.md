# 签名模块

## 1、使用

1. 添加依赖

   ```
   <dependency>
       <groupId>com.cubigdata.sec</groupId>
       <artifactId>plugin-sign-client</artifactId>
       <version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. nacos增加配置

   命名空间根据项目自定义， 程序需要保证程序能读取  Data ID：sign_info  Group:SYS_DICT_GROUP下的配置文件

   ```
   [
       {
           "appKey":"eXHOOGmg",
           "appSecret":"e1b53f17dc9bb09f16801cc9803c27c12f3ff0dd",
           "signType":"common"
       },
       {
           "appKey":"ythpt_client",
           "appSecret":"68904d00df72889db98192984dc11aa690e34e8e",
           "signType":"common"
       },
       {
           "appKey":"client_credentials",
           "appSecret":"client_credentials",
           "signType":"iam"
       },
       {
           "appKey":"aqdn_risen",
           "appSecret":"f6f8766807b707919d5b92ee18561a100794ebcc",
           "signType":"common"
       },
       {
           "appKey":"aqdn_dplus",
           "appSecret":"bf7ecf771782c4b6b41fc0f8238106e07c1652b1",
           "signType":"common"
       },
       {
           "appKey":"123456",
           "appSecret":"123456",
           "signType":"feign"
       },
       {
           "appKey":"hy971122",
           "appSecret":"123456",
           "signType":"feign"
       }
   ]
   ```

3. hutool版本尽量高一点， 否则可能会有兼容性问题， 测试5.8.21是可以的

   ```
   <dependency>
       <groupId>cn.hutool</groupId>
       <artifactId>hutool-all</artifactId>
       <version>5.8.21</version>
   </dependency>
   ```

4. 在请求签名接口时， 需要在header中带入**appKey、timestamp和sign**；



## 2、签名校验流程

1. 请求参数获取
  CommonSignHandler.handle 方法调用 SignUtil.getAllParams 方法获取请求的所有参数。
  SignUtil.getAllParams 方法从请求头和请求体中提取所有参数，并进行以下处理：
  从请求头中获取 appKey 和 timestamp。
  检查 timestamp 是否为空，如果为空则抛出异常。
  检查 timestamp 是否超过5分钟，如果超过则抛出异常。
  从请求URL中获取参数并解码。
  如果请求方法不是GET，则从请求体中获取参数并解码。
  将所有参数按键名排序并返回一个 SortedMap。
2. Nonce 校验
  CommonSignHandler.handle 方法检查是否启用了 nonce 校验。
  如果启用，从请求头中获取 nonce。
  检查 nonce 是否为空，如果为空则抛出异常。
  检查 nonce 是否已经存在于Redis中，如果存在则拒绝请求，否则将 nonce 存入Redis并设置过期时间为1分钟。
3. 签名生成与校验
  CommonSignHandler.handle 方法调用 buildSignature 方法生成签名。
  buildSignature 方法将所有参数按键名排序，并将参数名和参数值连接成字符串。
  在字符串的头部和尾部分别添加 appSecret。
  使用MD5算法对字符串进行加密，并将结果转换为大写。
  从请求头中获取 signature。
  比较生成的签名和请求头中的签名是否一致，如果一致则校验通过，否则校验失败。

```Mermaid
graph TD
    A[开始] --> B[获取所有参数]
    B --> C{Nonce 校验启用?}
    C -->|是| D[获取Nonce]
    D --> E{Nonce 为空?}
    E -->|是| F[拒绝请求: 缺少Nonce]
    E -->|否| G[检查Nonce是否存在]
    G -->|存在| H[拒绝请求: Nonce 已存在]
    G -->|不存在| I[将Nonce存入Redis]
    C -->|否| J[跳过Nonce校验]
    I --> K[生成签名]
    J --> K
    K --> L[获取请求头中的签名]
    L --> M{签名一致?}
    M -->|是| N[签名校验通过]
    M -->|否| O[签名校验失败]
    O --> P[记录日志: 签名校验失败]
    N --> Q[结束]

```





## 3、签名计算流程

1. 参数排序
  输入: 一个 SortedMap<String, Object> 类型的参数集合，其中键是参数名，值是参数值。
  过程: SortedMap 会自动按键名进行排序。
2. 参数拼接
  过程:
  遍历排序后的参数集合，将每个参数的键和值连接成字符串，格式为 key=value。
  将这些 key=value 字符串用 & 符号连接起来。
  在拼接好的字符串最后添加 appSecret。
  使用 MD5 算法对最终的字符串进行加密，并将结果转换为大写。





**示例：**
假设请求有以下参数：

```
appKey: myAppKey
timestamp: 1672531200
param1: value1
param2: value2
```


排序后的参数顺序为: 

```
appKey, param1, param2, timestamp
```


按照 key=value 的格式拼接每个参数:

```
appKey=myAppKey
param1=value1
param2=value2
timestamp=1672531200
```

用 & 符号连接这些字符串:

```
appKey=myAppKey&param1=value1&param2=value2&timestamp=1672531200
```

添加 appSecret:
假设 appSecret 是 mySecret
在拼接好的字符串最后添加 appSecret:

```
appKey=myAppKey&param1=value1&param2=value2&timestamp=1672531200&appSecret=mySecret
```

生成签名:
使用 MD5 算法对字符串进行加密，并将结果转换为大写。
假设 MD5 加密后的结果是 E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855

将这个字符串添加到header中， key为sign



