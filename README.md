# VideoCache
![](https://img.shields.io/badge/platform-android-orange.svg)
![](https://img.shields.io/badge/language-java-yellow.svg)
![](https://jitpack.io/v/com.iwdael/videocache.svg)
![](https://img.shields.io/badge/build-passing-brightgreen.svg)
![](https://img.shields.io/badge/license-apache--2.0-green.svg)
![](https://img.shields.io/badge/api-19+-green.svg)

Android视频缓存框架，避免多次或重复下载消耗更多的时间和流量。

## 特点
 - 流式传输期间缓存到磁盘
 - 离线使用缓存资源
 - 部分加载
 - 缓存限制（最大缓存大小，最大文件数）
 - 多个客户端访问同一缓存

## 说明
使用代理URL替代原始URL来开启URL，Android P及以上请关闭禁止明文通信
```
@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);

    HttpProxyCacheServer proxy = getProxy();
    String proxyUrl = proxy.getProxyUrl(VIDEO_URL);
    videoView.setVideoPath(proxyUrl);
}

private HttpProxyCacheServer getProxy() {
    // should return single instance of HttpProxyCacheServer shared for whole app.
}
```
为了保证正常工作，您应该为整个应用程序使用单个实例HttpProxyCacheServer。例如，您可以将共享代理存储在您的Application：
```
public class App extends Application {

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer(this);
    }
}
```
### 磁盘缓存限制
默认情况下HttpProxyCacheServer使用 512Mb 缓存文件。您可以更改此值：
```java
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .maxCacheSize(1024 * 1024 * 1024)       // 1 Gb for cache
            .build();
}
```
或者可以限制缓存中的文件总数：
```java
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .maxCacheFilesCount(20)
            .build();
}
```
甚至实施您自己的DiskUsage策略：
```java
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .diskUsage(new MyCoolDiskUsageStrategy())
            .build();
}
```
### 为缓存文件提供名称
默认AndroidVideoCache使用视频 url 的 MD5 作为文件名。但在某些情况下 url 不稳定，它可能包含一些生成的部分（例如会话令牌）。在这种情况下，缓存机制将被破坏。要修复它，您必须提供自己的FileNameGenerator：
```
public class MyFileNameGenerator implements FileNameGenerator {

    // Urls contain mutable parts (parameter 'sessionToken') and stable video's id (parameter 'videoId').
    // e. g. http://example.com?videoId=abcqaz&sessionToken=xyz987
    public String generate(String url) {
        Uri uri = Uri.parse(url);
        String videoId = uri.getQueryParameter("videoId");
        return videoId + ".mp4";
    }
}
```
```
HttpProxyCacheServer proxy = HttpProxyCacheServer.Builder(context)
    .fileNameGenerator(new MyFileNameGenerator())
    .build()
```
### 添加自定义 http 标头
您可以在以下帮助下向请求添加自定义标头HeadersInjector：
```
public class UserAgentHeadersInjector implements HeaderInjector {

    @Override
    public Map<String, String> addHeaders(String url) {
        return Maps.newHashMap("User-Agent", "Cool app v1.1");
    }
}
```
```
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .headerInjector(new UserAgentHeadersInjector())
            .build();
}

```
## 如何配置
将本仓库引入你的项目:
### Step 1. 添加JitPack仓库到Build文件
合并以下代码到项目根目录下的build.gradle文件的repositories尾。

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

### Step 2. 添加依赖
合并以下代码到需要使用的application Module的dependencies尾。
```Java
	dependencies {
	  ...
          compile 'com.iwdael:videocache:$version'
	}
```

## 感谢
Power by [danikula/AndroidVideoCache](https://github.com/danikula/AndroidVideoCache)