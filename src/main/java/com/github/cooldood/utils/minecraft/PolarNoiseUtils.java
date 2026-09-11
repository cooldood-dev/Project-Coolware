package ddlc.yuri.utils.client;

import java.util.Random;

/**
 * MASSIVELY randomized noise library used by the polar rotation pipeline.
 * Provides an absurd number of distinct noise/randomization algorithms so that
 * the polar rotation manager can layer and blend them into near-unrecognizable,
 * human-like wobble.
 */
public class PolarNoiseUtils {

    // ---------------------------------------------------------------
    //  Classic Perlin permutation table
    // ---------------------------------------------------------------
    private static final int[] PERM = new int[512];
    private static final int[] P = {
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225,
            140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148,
            247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
            57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175,
            74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122,
            60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54,
            65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169,
            200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64,
            52, 217, 226, 250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212,
            207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213,
            119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
            129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104,
            218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241,
            81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157,
            184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93,
            222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
    };

    static {
        Random rng = new Random();
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;
        for (int i = 255; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        for (int i = 0; i < 256; i++) {
            PERM[i] = perm[i];
            PERM[i + 256] = perm[i];
        }
    }

    private static final int[][] GRAD3 = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };

    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    private static final double F3 = 1.0 / 3.0;
    private static final double G3 = 1.0 / 6.0;

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double dot(int[] g, double x, double y) {
        return g[0] * x + g[1] * y;
    }

    // ---------------------------------------------------------------
    //  1. Classic 2D Perlin
    // ---------------------------------------------------------------
    public static double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;

        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        double u = fade(xf);
        double v = fade(yf);

        int aa = PERM[PERM[xi] + yi];
        int ab = PERM[PERM[xi] + yi + 1];
        int ba = PERM[PERM[xi + 1] + yi];
        int bb = PERM[PERM[xi + 1] + yi + 1];

        double x1 = lerp(u, dot(GRAD3[aa % 12], xf, yf), dot(GRAD3[ba % 12], xf - 1, yf));
        double x2 = lerp(u, dot(GRAD3[ab % 12], xf, yf - 1), dot(GRAD3[bb % 12], xf - 1, yf - 1));

        return lerp(v, x1, x2);
    }

    // ---------------------------------------------------------------
    //  2. 2D Perlin, value range normalized to roughly [-1, 1]
    // ---------------------------------------------------------------
    public static double perlin2D(double x, double y) {
        return noise(x, y) * 2.0 - 1.0;
    }

    // ---------------------------------------------------------------
    //  3. Simplex 2D
    // ---------------------------------------------------------------
    public static double simplex2D(double xin, double yin) {
        double n0, n1, n2;
        double s = (xin + yin) * F2;
        int i = fastfloor(xin + s);
        int j = fastfloor(yin + s);
        double t = (i + j) * G2;
        double X0 = i - t;
        double Y0 = j - t;
        double x0 = xin - X0;
        double y0 = yin - Y0;

        int i1, j1;
        if (x0 > y0) { i1 = 1; j1 = 0; } else { i1 = 0; j1 = 1; }

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        int ii = i & 255;
        int jj = j & 255;
        int gi0 = PERM[ii + PERM[jj]] % 12;
        int gi1 = PERM[ii + i1 + PERM[jj + j1]] % 12;
        int gi2 = PERM[ii + 1 + PERM[jj + 1]] % 12;

        double t0 = 0.5 - x0 * x0 - y0 * y0;
        n0 = t0 < 0 ? 0.0 : t0 * t0 * t0 * t0 * dot(GRAD3[gi0], x0, y0);
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        n1 = t1 < 0 ? 0.0 : t1 * t1 * t1 * t1 * dot(GRAD3[gi1], x1, y1);
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        n2 = t2 < 0 ? 0.0 : t2 * t2 * t2 * t2 * dot(GRAD3[gi2], x2, y2);

        return 70.0 * (n0 + n1 + n2);
    }

    private static int fastfloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    // ---------------------------------------------------------------
    //  4. Simplex 3D
    // ---------------------------------------------------------------
    private static final int[][] GRAD3D = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };

    public static double simplex3D(double xin, double yin, double zin) {
        double n0, n1, n2, n3;
        double s = (xin + yin + zin) * F3;
        int i = fastfloor(xin + s);
        int j = fastfloor(yin + s);
        int k = fastfloor(zin + s);
        double t = (i + j + k) * G3;
        double X0 = i - t;
        double Y0 = j - t;
        double Z0 = k - t;
        double x0 = xin - X0;
        double y0 = yin - Y0;
        double z0 = zin - Z0;

        int i1, j1, k1, i2, j2, k2;
        if (x0 >= y0) {
            if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
            else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
            else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
        } else {
            if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; }
            else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; }
            else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
        }

        double x1 = x0 - i1 + G3;
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3;
        double y2 = y0 - j2 + 2.0 * G3;
        double z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3;
        double y3 = y0 - 1.0 + 3.0 * G3;
        double z3 = z0 - 1.0 + 3.0 * G3;

        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;

        int gi0 = PERM[ii + PERM[jj + PERM[kk]]] % 12;
        int gi1 = PERM[ii + i1 + PERM[jj + j1 + PERM[kk + k1]]] % 12;
        int gi2 = PERM[ii + i2 + PERM[jj + j2 + PERM[kk + k2]]] % 12;
        int gi3 = PERM[ii + 1 + PERM[jj + 1 + PERM[kk + 1]]] % 12;

        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
        n0 = t0 < 0 ? 0.0 : t0 * t0 * t0 * t0 * dot(GRAD3D[gi0], x0, y0);
        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
        n1 = t1 < 0 ? 0.0 : t1 * t1 * t1 * t1 * dot(GRAD3D[gi1], x1, y1);
        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
        n2 = t2 < 0 ? 0.0 : t2 * t2 * t2 * t2 * dot(GRAD3D[gi2], x2, y2);
        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
        n3 = t3 < 0 ? 0.0 : t3 * t3 * t3 * t3 * dot(GRAD3D[gi3], x3, y3);

        return 32.0 * (n0 + n1 + n2 + n3);
    }

    // ---------------------------------------------------------------
    //  5. Value noise (hash-based, no gradients)
    // ---------------------------------------------------------------
    private static double hash(double x, double y) {
        long h = Double.doubleToLongBits(x) * 374761393 + Double.doubleToLongBits(y) * 668265263;
        h = (h ^ (h >> 13)) * 1274126177L;
        long finalH = (h ^ (h >> 16)) & 0xFFFFFFFFL;
        return (finalH & 0xFFFFFF) / (double) 0x1000000 * 2.0 - 1.0;
    }

    private static double smoothStep(double t) {
        return t * t * (3 - 2 * t);
    }

    public static double valueNoise(double x, double y) {
        int x0 = fastfloor(x);
        int y0 = fastfloor(y);
        double fx = x - x0;
        double fy = y - y0;
        double v00 = hash(x0, y0);
        double v10 = hash(x0 + 1, y0);
        double v01 = hash(x0, y0 + 1);
        double v11 = hash(x0 + 1, y0 + 1);
        double ux = smoothStep(fx);
        double uy = smoothStep(fy);
        return lerp(uy, lerp(ux, v00, v10), lerp(ux, v01, v11));
    }

    // ---------------------------------------------------------------
    //  6. Gradient noise via permutation table blend (perlin without fade)
    // ---------------------------------------------------------------
    public static double gradientNoise(double x, double y) {
        int xi = fastfloor(x) & 255;
        int yi = fastfloor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        int aa = PERM[PERM[xi] + yi];
        int ab = PERM[PERM[xi] + yi + 1];
        int ba = PERM[PERM[xi + 1] + yi];
        int bb = PERM[PERM[xi + 1] + yi + 1];

        double x1 = lerp(xf, dot(GRAD3[aa % 12], xf, yf), dot(GRAD3[ba % 12], xf - 1, yf));
        double x2 = lerp(xf, dot(GRAD3[ab % 12], xf, yf - 1), dot(GRAD3[bb % 12], xf - 1, yf - 1));

        return lerp(yf, x1, x2) * 2.0 - 1.0;
    }

    // ---------------------------------------------------------------
    //  7. Cellular / Worley noise
    // ---------------------------------------------------------------
    public static double cellularNoise(double x, double y) {
        int X = fastfloor(x);
        int Y = fastfloor(y);
        double minDist = 8.0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                double cellX = X + i + hash(X + i, Y + j);
                double cellY = Y + j + hash(X + i, Y + j + 31);
                double dx = cellX - x;
                double dy = cellY - y;
                double dist = dx * dx + dy * dy;
                if (dist < minDist) minDist = dist;
            }
        }
        return Math.sqrt(minDist) - 1.0;
    }

    // ---------------------------------------------------------------
    //  8. Turbulence noise: cumulative |perlin| at increasing freq
    // ---------------------------------------------------------------
    public static double turbulence(double x, double y, int octaves) {
        double value = 0;
        double amp = 1.0;
        double freq = 1.0;
        for (int i = 0; i < octaves; i++) {
            value += amp * Math.abs(noise(x * freq, y * freq));
            amp *= 0.5;
            freq *= 2.0;
        }
        return value;
    }

    // ---------------------------------------------------------------
    //  9. Ridged multifractal noise
    // ---------------------------------------------------------------
    public static double ridgedNoise(double x, double y, int octaves, double lacunarity, double gain) {
        double value = 0;
        double amp = 0.5;
        double freq = 1.0;
        double prev = 1.0;
        for (int i = 0; i < octaves; i++) {
            double n = prev - Math.abs(noise(x * freq, y * freq) * 2.0 - 1.0);
            n = n * n;
            value += n * amp;
            prev = n;
            amp *= gain;
            freq *= lacunarity;
        }
        return value * 2.0 - 1.0;
    }

    // ---------------------------------------------------------------
    //  10. Billow noise: |perlin| summed, gives cloud-like bumps
    // ---------------------------------------------------------------
    public static double billow(double x, double y, int octaves) {
        double value = 0;
        double amp = 1.0;
        double freq = 1.0;
        for (int i = 0; i < octaves; i++) {
            value += amp * (1.0 - Math.abs(noise(x * freq, y * freq)));
            amp *= 0.5;
            freq *= 2.0;
        }
        return value * 2.0 - 1.0;
    }

    // ---------------------------------------------------------------
    //  11. Dual-octave blend (two different lacunarity/persistence runs)
    // ---------------------------------------------------------------
    public static double dualFbm(double x, double y, int octaves, double lacA, double perA, double lacB, double perB, double blend) {
        double a = fbm(x, y, octaves, lacA, perA);
        double b = fbm(x + 100.7, y + 233.9, octaves, lacB, perB);
        return lerp(blend, a, b);
    }

    // ---------------------------------------------------------------
    //  Standard fbm
    // ---------------------------------------------------------------
    public static double fbm(double x, double y, int octaves, double lacunarity, double persistence) {
        double value = 0;
        double amplitude = 1.0;
        double frequency = 1.0;

        for (int i = 0; i < octaves; i++) {
            value += amplitude * noise(x * frequency, y * frequency);
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return value;
    }

    // ---------------------------------------------------------------
    //  fbm built on simplex
    // ---------------------------------------------------------------
    public static double fbmSimplex(double x, double y, int octaves, double lacunarity, double persistence) {
        double value = 0;
        double amplitude = 1.0;
        double frequency = 1.0;
        for (int i = 0; i < octaves; i++) {
            value += amplitude * simplex2D(x * frequency, y * frequency);
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return value;
    }

    // ---------------------------------------------------------------
    //  fbm built on value noise
    // ---------------------------------------------------------------
    public static double fbmValue(double x, double y, int octaves, double lacunarity, double persistence) {
        double value = 0;
        double amplitude = 1.0;
        double frequency = 1.0;
        for (int i = 0; i < octaves; i++) {
            value += amplitude * valueNoise(x * frequency, y * frequency);
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return value;
    }

    // ---------------------------------------------------------------
    //  Rotating domain warp (warp with angle varying per octave)
    // ---------------------------------------------------------------
    public static double domainWarp(double x, double y, double strength, double scale, int octaves) {
        double warpX = fbm(x, y, octaves, 2.0, 0.5);
        double warpY = fbm(x + 5.2, y + 1.3, octaves, 2.0, 0.5);

        return fbm(x + warpX * strength * scale, y + warpY * strength * scale, octaves, 2.0, 0.5);
    }

    // ---------------------------------------------------------------
    //  Rotating domain warp built on simplex, with rotation
    // ---------------------------------------------------------------
    public static double domainWarpRotating(double x, double y, double strength, double scale, int octaves, double angle) {
        double cosA = Math.cos(angle);
        double sinA = Math.sin(angle);
        double rx = x * cosA - y * sinA;
        double ry = x * sinA + y * cosA;

        double warpX = fbmSimplex(rx, ry, octaves, 2.0, 0.5);
        double warpY = fbmSimplex(rx + 3.14, ry + 7.77, octaves, 2.0, 0.5);

        double wx = rx + warpX * strength * scale;
        double wy = ry + warpY * strength * scale;

        return fbmSimplex(wx, wy, octaves, 2.0, 0.5);
    }

    // ---------------------------------------------------------------
    //  Blended noise - mix perlin + simplex + value
    // ---------------------------------------------------------------
    public static double blendedNoise(double x, double y, int octaves, double perlinW, double simplexW, double valueW) {
        double total = perlinW + simplexW + valueW;
        if (total <= 0) return 0;
        double v1 = fbm(x, y, octaves, 2.0, 0.5) * perlinW;
        double v2 = fbmSimplex(x + 17.3, y + 29.1, octaves, 2.3, 0.45) * simplexW;
        double v3 = fbmValue(x + 41.7, y - 8.9, octaves, 1.9, 0.55) * valueW;
        return (v1 + v2 + v3) / total;
    }

    // ---------------------------------------------------------------
    //  Pseudo-random white-noise flutter [-1, 1]
    // ---------------------------------------------------------------
    public static double jitter(double x) {
        return hash(x, x * 31.7 + 0.5);
    }
}
