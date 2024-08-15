
package com.ABC.pioneer.sensor.ble.filter;

import android.bluetooth.le.ScanRecord;
import android.content.Context;

import com.ABC.pioneer.sensor.ble.BLEDevice;
import com.ABC.pioneer.sensor.ble.Configurations;

import com.ABC.pioneer.sensor.data.TextFile;
import com.ABC.pioneer.sensor.datatype.Data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//  设备筛选器，用于避免连接到绝对不能承载sensor服务的设备。
public class BLEDeviceFilter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final List<FilterPattern> filterPatterns;
    private final TextFile textFile;
    private final Map<Data, ShouldIgnore> samples = new HashMap<>();

    // 训练samples的计数
    private final static class ShouldIgnore {
        public long yes = 0;
        public long no = 0;
    }

    // 基于消息内容过滤设备的pattern
    public final static class FilterPattern {
        public final String regularExpression;
        public final Pattern pattern;
        public FilterPattern(final String regularExpression, final Pattern pattern) {
            this.regularExpression = regularExpression;
            this.pattern = pattern;
        }
    }

    // 匹配过滤器pattern
    public final static class MatchingPattern {
        public final FilterPattern filterPattern;
        public final String message;
        public MatchingPattern(FilterPattern filterPattern, String message) {
            this.filterPattern = filterPattern;
            this.message = message;
        }
    }

    /// BLE设备筛选器，用于将设备与BLESensorConfiguration.deviceFilterFeaturePatterns中定义的筛选器进行匹配。
    public BLEDeviceFilter() {
        this(null, null, Configurations.deviceFilterFeaturePatterns);
    }

    // BLE设备筛选器，用于将设备与BLESensorConfiguration.deviceFilterFeaturePatterns进行匹配
    // 并将广播数据写入文件进行分析。
    public BLEDeviceFilter(final Context context, final String file) {
        this(context, file, Configurations.deviceFilterFeaturePatterns);
    }

    /// BLE设备过滤器，用于根据给定的patterns匹配设备
    // 并将广播数据写入文件进行分析。
    public BLEDeviceFilter(final Context context, final String file, final String[] patterns) {
        if (context == null || file == null) {
            textFile = null;
        } else {
            textFile = new TextFile(context, file);
            if (textFile.empty()) {
                textFile.write("time,ignore,featureData,scanRecordRawData,identifier,rssi,deviceModel,deviceName");
            }
        }
        if (Configurations.deviceFilterTrainingEnabled || patterns == null || patterns.length == 0) {
            filterPatterns = null;
        } else {
            filterPatterns = compilePatterns(patterns);
        }
    }

    // Pattern匹配函数
    // 在特征数据的十六进制表示形式上使用正则表达式可最大程度地提高灵活性和可用性

    /// 按顺序将消息与所有pattern匹配，返回匹配pattern，或返回为null
    protected static FilterPattern match(final List<FilterPattern> filterPatterns, final String message) {
        if (message == null) {
            return null;
        }
        for (final FilterPattern filterPattern : filterPatterns) {
            try {
                final Matcher matcher = filterPattern.pattern.matcher(message);
                if (matcher.find()) {
                    return filterPattern;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    /// 将正则表达式编译为patterns.
    protected static List<FilterPattern> compilePatterns(final String[] regularExpressions) {
        final List<FilterPattern> filterPatterns = new ArrayList<>(regularExpressions.length);
        for (final String regularExpression : regularExpressions) {
            try {
                final Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
                if (regularExpression != null && !regularExpression.isEmpty() && pattern != null) {
                    final FilterPattern filterPattern = new FilterPattern(regularExpression, pattern);
                    filterPatterns.add(filterPattern);
                }
            } catch (Throwable e) {
            }
        }
        return filterPatterns;
    }

    /// 从特定于制造商的数据中提取消息
    protected final static List<Data> extractMessages(final byte[] rawScanRecordData) {
        // 解析扫描响应数据中的raw扫描记录数据
        if (rawScanRecordData == null || rawScanRecordData.length == 0) {
            return null;
        }
        final BLEScanResponseData bleScanResponseData = BLEParser.parseScanResponse(rawScanRecordData, 0);
        // 将扫描响应数据解析为特定于制造商的数据
        if (bleScanResponseData == null || bleScanResponseData.segments == null || bleScanResponseData.segments.isEmpty()) {
            return null;
        }
        final List<BLEManuData> bleManuDataList = BLEParser.extractManufacturerData(bleScanResponseData.segments);
        // 将制造商特定的数据解析为消息
        if (bleManuDataList == null || bleManuDataList.isEmpty()) {
            return null;
        }
        final List<BLEAppleManuSeg> bleAppleManuSegs = BLEParser.extractAppleManufacturerSegments(bleManuDataList);
        // 将数据段转换为消息
        if (bleAppleManuSegs == null || bleAppleManuSegs.isEmpty()) {
            return null;
        }
        final List<Data> messages = new ArrayList<>(bleAppleManuSegs.size());
        for (BLEAppleManuSeg segment : bleAppleManuSegs) {
            if (segment != null && segment.raw != null && segment.raw.value.length > 0) {
                messages.add(segment.raw);
            }
        }
        return messages;
    }

    // Filtering函数

    /// 从扫描记录中提取特征数据
    private List<Data> extractFeatures(final ScanRecord scanRecord) {
        if (scanRecord == null) {
            return null;
        }
        // 获取相应数据
        final List<Data> featureList = new ArrayList<>();
        final List<Data> messages = extractMessages(scanRecord.getBytes());
        if (messages != null) {
            featureList.addAll(messages);
        }
        return featureList;
    }

    /// 将训练示例添加到自适应滤波器。
    public synchronized void train(final BLEDevice device, final boolean ignore) {
        final ScanRecord scanRecord = device.scanRecord();
        // 从扫描记录中获取特征数据
        if (scanRecord == null) {
            return;
        }
        final Data scanRecordData = (scanRecord.getBytes() == null ? null : new Data(scanRecord.getBytes()));
        if (scanRecordData == null) {
            return;
        }
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null) {
            return;
        }
        // 更新忽略要素数据的是/否计数
        for (Data featureData : featureList) {
            ShouldIgnore shouldIgnore = samples.get(featureData);
            if (shouldIgnore == null) {
                shouldIgnore = new ShouldIgnore();
                samples.put(featureData, shouldIgnore);
            }
            if (ignore) {
                shouldIgnore.yes++;
            } else {
                shouldIgnore.no++;
            }
            // 将样本写入文本文件进行分析
            if (textFile == null) {
                return;
            }
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('"');
            stringBuilder.append(dateFormatter.format(new Date()));
            stringBuilder.append('"');
            stringBuilder.append(',');
            stringBuilder.append(ignore ? 'Y' : 'N');
            stringBuilder.append(',');
            stringBuilder.append(featureData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(scanRecordData.hexEncodedString());
            stringBuilder.append(',');
            stringBuilder.append(device.identifier.value);
            stringBuilder.append(',');
            if (device.rssi() != null) {
                stringBuilder.append(device.rssi().value);
            }
            stringBuilder.append(',');
            if (device.model() != null) {
                stringBuilder.append('"');
                stringBuilder.append(device.model());
                stringBuilder.append('"');
            }
            stringBuilder.append(',');
            if (device.deviceName() != null) {
                stringBuilder.append('"');
                stringBuilder.append(device.deviceName());
                stringBuilder.append('"');
            }
            textFile.write(stringBuilder.toString());
        }
    }

    /// 将过滤器模式与数据项进行匹配，并返回第一个匹配项
    protected final static MatchingPattern match(final List<FilterPattern> patternList, final Data rawData) {
        // 没有匹配到的pattern
        if (patternList == null || patternList.isEmpty()) {
            return null;
        }
        // 如果raw data为空
        if (rawData == null || rawData.value == null || rawData.value.length == 0) {
            return null;
        }
        // 提取信息
        final List<Data> messages = extractMessages(rawData.value);
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (Data message : messages) {
            if (message == null) {
                continue;
            }
            try {
                final String hexEncodedString = message.hexEncodedString();
                final FilterPattern pattern = match(patternList, hexEncodedString);
                if (pattern != null) {
                    return new MatchingPattern(pattern, hexEncodedString);
                }
            } catch (Throwable e) {

            }
        }
        return null;
    }

    /// 将扫描记录消息与所有已注册模式匹配，返回匹配模式或为空。
    public MatchingPattern match(final BLEDevice device) {
        try {
            final ScanRecord scanRecord = device.scanRecord();
            // 没有任何扫描记录数据就无法匹配设备
            if (scanRecord == null) {
                return null;
            }
            // 无法匹配数据为空的扫描记录
            final byte[] bytes = scanRecord.getBytes();
            if (bytes == null) {
                return null;
            }
            final Data rawData = new Data(bytes);
            // 尝试配对
            final MatchingPattern matchingPattern = match(filterPatterns, rawData);
            if (matchingPattern == null || matchingPattern.filterPattern == null || matchingPattern.filterPattern.pattern == null || matchingPattern.filterPattern.regularExpression == null || matchingPattern.message == null) {
                return null;
            } else {
                return matchingPattern;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    /// 是否应该根据扫描记录数据忽略设备？
    private boolean ignoreBasedOnStatistics(final BLEDevice device) {
        final ScanRecord scanRecord = device.scanRecord();
        // 不要忽略没有任何扫描记录数据的设备信息
        if (scanRecord == null) {
            return false;
        }
        // 从扫描记录中提取特征数据
        // 不要忽略没有任何特征数据的设备信息
        final List<Data> featureList = extractFeatures(scanRecord);
        if (featureList == null) {
            return false;
        }
        for (Data featureData : featureList) {
            // 获取training example统计信息
            final ShouldIgnore shouldIgnore = samples.get(featureData);
            // 不要基于未知特征数据忽略设备
            if (shouldIgnore == null) {
                return false;
            }
            // 即使仅仅有一个符合的例子，也不忽略设备信息
            if (shouldIgnore.no > 0) {
                return false;
            }
            // 如果签名已注册忽略两次以上，则忽略设备信息
            if (shouldIgnore.yes > 2) {
                return true;
            }
        }
        // 即使以上的规则都不符合，也不忽略设备信息
        return false;
    }
}
