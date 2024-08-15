
package com.ABC.pioneer.sensor.datatype;

/// 通用回调函数
public interface Callback<T> {
    void accept(T value);
}
