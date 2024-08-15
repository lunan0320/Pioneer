
package com.ABC.pioneer.sensor.ble;

import com.ABC.pioneer.sensor.datatype.RandomSource;
import com.ABC.pioneer.sensor.datatype.TimeInterval;

import java.util.UUID;

/// 定义BLE传感器配置数据，例如 服务和特征性UUID
public class Configurations {
    // BLE服务和特征性UUID，以及制造商ID

    //  Beacon: Service UUID。这是一个固定的UUID，使iOS设备即使在后台模式下也可以找到彼此。
    //  Android设备将需要首先使用制造商代码找到Apple设备，然后发现服务以识别实际的信标。
    // -Service and characteristic UUIDs 是V4 UUID，它们是通过进行网络搜索以确保不返回任何结果而随机生成并经过测试的唯一性。
    //   默认的Pioneer需要的UUID是 428132af-4746-42d3-801e-4572d65bfd9b
    // -通过在基本UUID中设置值xxxx切换到16位UUID 0000xxxx-0000-1000-8000-00805F9B34FB
    public static UUID serviceUUID = UUID.fromString("9ea51088-5add-438b-a269-dd6289844034");
    //  用于android控制外围设备和中央设备之间连接的Signal Characteristic，例如:使彼此保持暂停状态
    // -Characteristic UUID是随机生成的V4 UUID，已通过进行网络搜索来测试其唯一性，以确保其不返回任何结果。
    public final static UUID androidSignalCharacteristicUUID = UUID.fromString("0ff27076-1fb2-4551-9670-7b468af8a9e7");
    //  用于ios控制外围设备和中央设备之间连接的Signal Characteristic，例如:使彼此保持暂停状态
    //  -Characteristic UUID是随机生成的V4 UUID，已通过进行网络搜索来测试其唯一性，以确保其不返回任何结果。
    public final static UUID iosSignalCharacteristicUUID = UUID.fromString("64451d23-b364-4967-bb43-51b82a56f3a5");
    //  主要payload Characteristic UUID（读取），用于将有效载荷数据从外围设备分配到中央，例如:身份数据
    //  -特征性UUID是随机生成的V4 UUID，已通过进行网络搜索来测试其唯一性，以确保其不返回任何结果。
    public final static UUID payloadCharacteristicUUID = UUID.fromString("2b5362a6-c995-4ab7-b744-cb366d4785bd");


    /// 标准蓝牙服务和特征
    /// 这些都是BLE标准中的固定UUID。
    /// 通用访问服务的标准蓝牙Service UUID
    /// -BLE标准的Service UUID
    public final static UUID bluetoothGenericAccessServiceUUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    /// 通用访问服务的标准蓝牙Characteristic UUID：设备名称
    /// - BLE标准中的Characteristic UUID
    public final static UUID bluetoothGenericAccessServiceDeviceNameCharacteristicUUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    /// 通用访问服务的标准蓝牙Characteristic UUID：设备名称
    /// - BLE标准中的Characteristic UUID
    public final static UUID bluetoothGenericAccessServiceAppearanceCharacteristicUUID = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    /// 设备信息服务的标准蓝牙Service UUID
    /// - BLE标准中的Service UUID
    public final static UUID bluetoothDeviceInformationServiceUUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    /// 设备信息服务的标准蓝牙Characteristic UUID：Model
    /// - BLE标准中的Characteristic UUID
    public final static UUID bluetoothDeviceInformationServiceModelCharacteristicUUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    /// 设备信息服务的标准蓝牙Characteristic UUID: 制造商数据
    /// - BLE标准中的Characteristic UUID
    public final static UUID bluetoothDeviceInformationServiceManufacturerCharacteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    /// 设备信息服务的标准蓝牙Characteristic UUID: TX Power
    /// - BLE标准中的Characteristic UUID
    public final static UUID bluetoothDeviceInformationServiceTxPowerCharacteristicUUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");

    /// 在Android上使用制造商数据来存储伪设备地址
    /// -临时设定的ID
    public final static int manufacturerIdForSensor = 65530;
    /// BLE广告Apple的制造商ID，用于扫描后台iOS设备
    public final static int manufacturerIdForApple = 76;

    // BLE signal characteristic操作码

    /// Signal characteristic用于写入有效负载的动作码，1字节动作代码 + 2字节小段int16的整型数据（payload长度）+payload数据
    public final static byte signalCharacteristicActionWritePayload = (byte) 1;
    /// /// Signal characteristic用于写入RSSI的动作码，1字节动作代码 + 4字节小段int32的RSSI数据
    public final static byte signalCharacteristicActionWriteRSSI = (byte) 2;
    /// Signal characteristic用于写入共享payload的动作码，1字节动作代码 + 2字节小段int16的整型数据（payload长度）+payload sharing数据
    public final static byte signalCharacteristicActionWritePayloadSharing = (byte) 3;
    /// 立即写操作码
    public final static byte signalCharacteristicActionWriteImmediate = (byte) 4;

    // 应用程序可配置BLE功能

    /// 除了默认的Pioneer通信过程之外，还定期更新payload数据
    /// - 使用此选项可根据应用程序payload寿命启用常规payload读取。
    /// - 设置为.never以禁用此功能。
    /// - 有效负载更新将以didRead的形式报告给SensorDelegate。
    /// - 设置立即生效，无需重新启动BLESensor，也可以在BLESensor处于活动状态时应用。
    public static TimeInterval payloadDataUpdateTimeInterval = TimeInterval.never;

    //  过滤重复payload数据并抑制传感器代理响应(didRead:fromTarget)
    /// - 设置为.never以禁用此功能。
    /// - 设置时间间隔N以过滤最近N秒内看到的重复有效载荷数据
    /// - 例如：60表示在最后一分钟过滤重复项
    /// - 从所有目标过滤所有出现的payload数据
    public static TimeInterval filterDuplicatePayloadData = TimeInterval.never;

    // 共享有效负载的到期时间，以确保仅共享最近看到的有效负载
    public static TimeInterval payloadSharingExpiryTimeInterval = new TimeInterval(5 * TimeInterval.minute.value);

    /// 广播刷新时间间隔
    public static TimeInterval advertRefreshTimeInterval = TimeInterval.minutes(15);

    /// 用于生成伪设备地址的随机化方法，有关详细信息，请参见PseudoDeviceAddress和RandomSource文件。
    /// - 设置为Random以获得可靠的连续运行
    /// - 其他方法将在4-8小时后导致阻塞并在空闲设备上中断操作
    /// - 阻塞会在 应用初始化，广告刷新和影响系统服务时 发生
    public static RandomSource.Method pseudoDeviceAddressRandomisation = RandomSource.Method.Random;

    public static boolean deviceIntrospectionEnabled = false;

    public static boolean deviceFilterTrainingEnabled = false;

    /// 根据消息模式定义设备过滤规则
    /// - 避免连接到无法托管传感器服务的设备
    /// - 与广告中的每个制造商特定的数据消息（十六进制格式）匹配
    /// - Java正则表达式模式，不区分大小写，可在消息中的任何位置找到模式
    /// - 切记要在消息开头添加^以进行匹配
    /// - 在开发环境中使用deviceFilterTrainingEnabled来识别模式
    public static String[] deviceFilterFeaturePatterns = new String[]{
            "^10....04",
            "^10....14",
            "^0100000000000000000000000000000000",
            "^05","^07","^09",
            "^00","^1002","^06","^08","^03","^0C","^0D","^0F","^0E","^0B"
    };

    /// 启用惯性传感器
    public static boolean inertiaSensorEnabled = false;
}


