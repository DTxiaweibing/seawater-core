package io.github.dtxiaweibing.seawater;

/**
 * EOS-80 海水密度/盐度计算
 * 采用联合国教科文组织 EOS-80 (UNESCO 1983) 标准公式。
 * 支持温度、盐度、压力(或深度)三参数计算海水密度。
 *
 * 权威校准点 (偏差 < 1e-5 kg/m³)：
 *   density(5, 0, 0)      = 999.9667508   (参考 999.96675)
 *   density(5, 35, 0)     = 1027.6754653  (Gill 1982, 参考 1027.67547)
 *   density(25, 35, 0)    = 1023.3430585  (UNESCO, 参考 1023.34306)
 *   density(25, 35, 1000) = 1062.5381718  (Gill 1982, 参考 1062.53817)
 *   density(0, 0, 0)      = 999.842594    (精确)
 */
public class SeawaterDensity {

    private SeawaterDensity() {}

    /**
     * 纯水密度 (kg/m³)，EOS-80 标准，温度范围 0~40 ℃。
     */
    public static double pureWaterDensity(double tempC) {
        double T = tempC;
        return 999.842594
                + 6.793952e-2 * T
                - 9.095290e-3 * T * T
                + 1.001685e-4 * T * T * T
                - 1.120083e-6 * T * T * T * T
                + 6.536332e-9 * T * T * T * T * T;
    }

    /**
     * 海平面 (1 atm) 海水密度，EOS-80，盐度 0~40 PSU。
     */
    public static double densityAtSeaLevel(double tempC, double salinity) {
        double S = clampSalinity(salinity);
        double T = tempC;
        double rhoW = pureWaterDensity(T);
        double A = 0.824493
                - 4.0899e-3 * T
                + 7.6438e-5 * T * T
                - 8.2467e-7 * T * T * T
                + 5.3875e-9 * T * T * T * T;
        double B = -5.72466e-3
                + 1.0227e-4 * T
                - 1.6546e-6 * T * T;
        double C = 4.8314e-4;
        return rhoW + A * S + B * Math.pow(S, 1.5) + C * S * S;
    }

    /**
     * 含压力海水密度 (kg/m³)。
     * EOS-80 公式：ρ(S,T,p) = ρ(S,T,0) / (1 - p/K)
     *
     * @param tempC    温度 (℃)
     * @param salinity 实用盐度 (PSU, 0~40)
     * @param pressure 压力 (bar)
     */
    public static double density(double tempC, double salinity, double pressure) {
        double S = clampSalinity(salinity);
        double T = tempC;
        double p = pressure;
        double rho0 = densityAtSeaLevel(T, S);
        double K = secantBulkModulus(T, S, p);
        return rho0 / (1.0 - p / K);
    }

    /**
     * 深度对应的海水密度 (kg/m³)。
     * 压力按近似换算：p(bar) ≈ depth(m) / 10。
     *
     * @param tempC       温度 (℃)
     * @param salinity    实用盐度 (PSU, 0~40)
     * @param depthMeters 水深 (m)
     */
    public static double densityAtDepth(double tempC, double salinity, double depthMeters) {
        double pressureBar = depthToPressureBar(depthMeters);
        return density(tempC, salinity, pressureBar);
    }

    /**
     * EOS-80 割线体积模量 K(S,T,p)，单位 bar。
     */
    public static double secantBulkModulus(double tempC, double salinity, double pressure) {
        double S = clampSalinity(salinity);
        double T = tempC;
        double p = pressure;

        double Kw = 19652.21
                + 148.4206 * T
                - 2.327105 * T * T
                + 1.360477e-2 * T * T * T
                - 5.155288e-5 * T * T * T * T;

        double K0 = Kw
                + (54.6746 - 0.603459 * T + 1.09987e-2 * T * T - 6.1670e-5 * T * T * T) * S
                + (7.944e-2 + 1.6483e-2 * T - 5.3009e-4 * T * T) * Math.pow(S, 1.5);

        double Aw = 3.239908
                + 1.43713e-3 * T
                + 1.16092e-4 * T * T
                - 5.77905e-7 * T * T * T;

        double A_K = Aw
                + (2.2838e-3 - 1.0981e-5 * T - 1.6078e-6 * T * T) * S
                + 1.91075e-4 * Math.pow(S, 1.5);

        double Bw = 8.50935e-5
                - 6.12293e-6 * T
                + 5.2787e-8 * T * T;

        double B_K = Bw
                + (-9.9348e-7 + 2.0816e-8 * T + 9.1697e-10 * T * T) * S;

        return K0 + A_K * p + B_K * p * p;
    }

    /**
     * 由密度反推盐度 (二分法迭代)，海平面状态。
     *
     * @param tempC         温度 (℃)
     * @param targetDensity 目标密度 (kg/m³)
     * @return 盐度 (PSU, 0~40)
     */
    public static double solveSalinity(double tempC, double targetDensity) {
        return solveSalinity(tempC, targetDensity, 0.0);
    }

    /**
     * 由密度反推盐度 (二分法迭代)，可指定压力。
     *
     * @param tempC         温度 (℃)
     * @param targetDensity 目标密度 (kg/m³)
     * @param pressure      压力 (bar)
     * @return 盐度 (PSU, 0~40)
     */
    public static double solveSalinity(double tempC, double targetDensity, double pressure) {
        double S_low = 0.0;
        double S_high = 40.0;
        double rho_low = density(tempC, S_low, pressure);
        double rho_high = density(tempC, S_high, pressure);

        if (targetDensity <= rho_low) return 0.0;
        if (targetDensity >= rho_high) return 40.0;

        double S_mid = 0;
        for (int i = 0; i < 100; i++) {
            S_mid = (S_low + S_high) / 2.0;
            double rho_mid = density(tempC, S_mid, pressure);
            if (rho_mid < targetDensity) {
                S_low = S_mid;
            } else {
                S_high = S_mid;
            }
            if (S_high - S_low < 1e-7) break;
        }
        return (S_low + S_high) / 2.0;
    }

    /**
     * 水深(m) → 压力(bar)：近似 10 m 水柱 ≈ 1 bar。
     */
    public static double depthToPressureBar(double depthMeters) {
        return depthMeters / 10.0;
    }

    /**
     * 压力(bar) → 水深(m)。
     */
    public static double pressureBarToDepth(double pressureBar) {
        return pressureBar * 10.0;
    }

    private static double clampSalinity(double s) {
        if (s < 0) return 0;
        if (s > 40) return 40;
        return s;
    }
}