
package com.ABC.pioneer.sensor.datatype;

/// 时间点上的加速度（x，y，z）以米/秒为单位
public class InertiaLocationReference implements LocationReference {
    public final double x, y, z, magnitude;

    public InertiaLocationReference(Double x, Double y, Double z) {
        this.x = (x == null ? 0 : x);
        this.y = (y == null ? 0 : y);
        this.z = (z == null ? 0 : z);
        this.magnitude = Math.sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
    }

    public String description() {
        return "Inertia(magnitude=" + magnitude + ",x=" + x + ",y=" + y + ",z=" + z + ")";
    }
}
