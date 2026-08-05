# seawater-core

EOS-80 海水密度/盐度计算库（纯 Java，零依赖）

基于联合国教科文组织 **EOS-80 (UNESCO 1983)** 国际标准公式，支持温度、盐度、压力（或深度）三参数的完整海水密度计算。

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dtxiaweibing/seawater-core)](https://search.maven.org/artifact/io.github.dtxiaweibing/seawater-core)

## 特性

- 纯 Java 8+，无任何第三方依赖，可运行于任意 JVM/Android
- 完整 EOS-80 公式，含压力（深度）项
- 密度 ⇄ 盐度 双向换算（二分法求解）
- 水深 / 压力单位换算

## 安装

Maven 加入依赖即可，自动从中央仓库拉取：

```xml
<dependency>
    <groupId>io.github.dtxiaweibing</groupId>
    <artifactId>seawater-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle：

```groovy
implementation 'io.github.dtxiaweibing:seawater-core:1.0.0'
```

## 使用

```java
import io.github.dtxiaweibing.seawater.SeawaterDensity;

// 海水密度：温度25℃、盐度30PSU、海平面
double rho = SeawaterDensity.densityAtSeaLevel(25.0, 30.0);
// → 约 1019.6 kg/m³

// 含深度：水深5米（约0.5 bar）
double rhoDeep = SeawaterDensity.densityAtDepth(25.0, 30.0, 5.0);

// 含压力：直接指定压力(bar)
double rhoP = SeawaterDensity.density(25.0, 30.0, 0.5);

// 由密度反推盐度
double sal = SeawaterDensity.solveSalinity(25.0, 1021.5);
// → 约 32.56 PSU

// 纯水密度
double pw = SeawaterDensity.pureWaterDensity(25.0);

// 水深/压力换算
double bar = SeawaterDensity.depthToPressureBar(10.0);   // → 1.0 bar
double m   = SeawaterDensity.pressureBarToDepth(1.0);     // → 10.0 m
```

## API

| 方法 | 说明 |
|---|---|
| `pureWaterDensity(tempC)` | 纯水密度 (kg/m³)，0~40 ℃ |
| `densityAtSeaLevel(tempC, salinity)` | 海平面海水密度 (kg/m³) |
| `density(tempC, salinity, pressure)` | 含压力海水密度，pressure 单位 bar |
| `densityAtDepth(tempC, salinity, depthMeters)` | 指定水深的海水密度（近似 10m 水柱 ≈ 1 bar） |
| `solveSalinity(tempC, targetDensity)` | 密度 → 盐度 (PSU)，海平面 |
| `solveSalinity(tempC, targetDensity, pressure)` | 密度 → 盐度，可指定压力 |
| `secantBulkModulus(tempC, salinity, pressure)` | EOS-80 割线体积模量 (bar) |
| `depthToPressureBar(depthMeters)` | 水深 → 压力 |
| `pressureBarToDepth(pressureBar)` | 压力 → 水深 |

## 限制

- 温度 0~40 ℃；盐度 0~40 PSU（超出自动截断）
- 压力换算采用近似：10m 水柱 ≈ 1 bar，满足养殖/浅海场景

## License

Apache-2.0