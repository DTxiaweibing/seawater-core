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
    <version>1.0.1</version>
</dependency>
```

Gradle：

```groovy
implementation 'io.github.dtxiaweibing:seawater-core:1.0.1'
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

## 校准点（权威校验值）

以下为标准 EOS-80 (UNESCO 1983) 官方校验值，用于验证本库精度。
本库输出与权威参考值的偏差 < 1×10⁻⁵ kg/m³。

| 校验点 | 权威参考值 | 本库输出 | 偏差 |
|---|---|---|---|
| `density(5, 0, 0)` | 999.9667500 | 999.9667508 | < 1e-6 |
| `density(5, 35, 0)` | 1027.6754700 (Gill 1982) | 1027.6754653 | < 5e-6 |
| `density(25, 35, 0)` | 1023.3430600 (UNESCO) | 1023.3430585 | < 2e-6 |
| `density(25, 35, 1000)` | 1062.5381700 (Gill 1982) | 1062.5381718 | < 2e-6 |
| `density(0, 0, 0)` | 999.8425940 | 999.8425940 | 0 |

> 注：`density(t, s, p)` 参数顺序为 (温度℃, 盐度PSU, 压力bar)。
> 纯水密度 T⁵ 项系数为标准值 `6.536332e-9`（v1.0.1 已修正，旧版误写为 `6.536336e-9`）。

## 限制

- 温度 0~40 ℃；盐度 0~40 PSU（超出自动截断）
- 压力换算采用近似：10m 水柱 ≈ 1 bar，满足养殖/浅海场景

## License

Apache-2.0