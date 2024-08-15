

package com.ABC.pioneer.sensor.datatype;

import com.ABC.pioneer.sensor.ble.Configurations;

import java.util.Objects;

/// 伪设备地址，可在不依赖设备mac地址的情况下缓存设备有效负载可能会经常更改，例如A10和A20。
public class PseudoDeviceAddress {
    private static RandomSource randomSource = new RandomSource(RandomSource.Method.Random);
    public final long address;
    public final byte[] data;

    public PseudoDeviceAddress(final RandomSource.Method method) {
        // 蓝牙设备地址为48位（6字节），使用相同的长度以提供相同的避免冲突的选择。随机，安全随机单例，
        // 安全随机和符合NIST的安全随机之间进行选择，因为随机源随机是非阻塞的并且具有 在这种情况下，
        // 我们被修改为从可靠来源获得熵。 为此目的，它经过验证和推荐是足够安全的。
        // -SecureRandomSingleton在闲置设备上运行4-8小时后会阻塞，并且不适用于此用例，不建议使用
        // -SecureRandom在闲置设备上运行4-8小时后会阻塞，不适合此使用案例，不建议使用
        // -SecureRandomNIST在闲置设备上运行6个小时后会被阻止，因此不适用于此用例，不建议使用
        if (randomSource == null || randomSource.method != method) {
            randomSource = new RandomSource(method);
        }
        this.data = encode(randomSource.nextLong());
        this.address = decode(this.data);
    }

    /// 默认构造函数使用Random作为随机源，请参见上文和RandomSource了解详细信息。
    public PseudoDeviceAddress() {
        this(Configurations.pseudoDeviceAddressRandomisation);
    }

    public PseudoDeviceAddress(final byte[] data) {
        this.address = decode(data);
        this.data = encode(this.address);
    }

    public PseudoDeviceAddress(final long value) {
        this.data = encode(value);
        this.address = decode(data);
    }

    protected final static byte[] encode(final long value) {
        final Data encoded = new Data();
        encoded.append(new Int64(value));
        return encoded.subdata(2, 6).value;
    }

    protected final static long decode(final byte[] data) {
        final Data decoded = new Data((byte) 0, 2);
        decoded.append(new Data(data));
        if (decoded.value.length < 8) {
            decoded.append(new Data((byte) 0, 8 - decoded.value.length));
        }
        final Int64 int64 = decoded.int64(0);
        return (int64 == null ? 0 : decoded.int64(0).value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PseudoDeviceAddress that = (PseudoDeviceAddress) o;
        return address == that.address;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return Base64.encode(data);
    }
}
