package jp.shint.terracraft.jei;

/** Orthographic camera shared by drawing and picking the altar's model faces. */
final class RitualProjection {
    record Point(float x, float y, float depth) { }
    private final double sin, cos;
    private static final double PITCH = Math.toRadians(25);

    RitualProjection(double yaw) { sin = Math.sin(yaw); cos = Math.cos(yaw); }

    Point project(float x, float y, float z) {
        double horizontal = cos * x - sin * z;
        double forward = sin * x + cos * z;
        return new Point((float) (103 + horizontal * 20),
                (float) (55 + (Math.sin(PITCH) * forward - Math.cos(PITCH) * y) * 20),
                (float) (Math.cos(PITCH) * forward + Math.sin(PITCH) * y));
    }

    double facing(float x, float y, float z) {
        return Math.cos(PITCH) * (sin * x + cos * z) + Math.sin(PITCH) * y;
    }

    /** Map a normalized UV rectangle onto a projected planar block face. */
    static float[] faceTransform(Point[] p, float[] u, float[] v) {
        float du1 = u[1] - u[0], du3 = u[3] - u[0], dv1 = v[1] - v[0], dv3 = v[3] - v[0];
        float det = du1 * dv3 - du3 * dv1;
        if (Math.abs(det) < 1e-6F) return null;
        float ax = ((p[1].x - p[0].x) * dv3 - (p[3].x - p[0].x) * dv1) / det;
        float ay = ((p[1].y - p[0].y) * dv3 - (p[3].y - p[0].y) * dv1) / det;
        float bx = ((p[3].x - p[0].x) * du1 - (p[1].x - p[0].x) * du3) / det;
        float by = ((p[3].y - p[0].y) * du1 - (p[1].y - p[0].y) * du3) / det;
        return new float[]{ax, ay, bx, by, p[0].x - ax * u[0] - bx * v[0], p[0].y - ay * u[0] - by * v[0]};
    }

    static boolean contains(Point[] points, double x, double y) {
        boolean positive = false, negative = false;
        for (int i = 0; i < 4; i++) {
            Point a = points[i], b = points[(i + 1) % 4];
            double cross = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x);
            positive |= cross > 0.001;
            negative |= cross < -0.001;
        }
        return !(positive && negative) && (positive || negative);
    }
}
