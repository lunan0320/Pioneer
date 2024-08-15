
package com.ABC.pioneer.sensor.datatype;

import java.text.SimpleDateFormat;
import java.util.Date;

/// 遇到记录，描述某一时刻与目标的接近程度
public class Encounter {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public Date timestamp = null;
    public Proximity proximity = null;
    public PayloadData payload = null;

    public Encounter(Proximity didMeasure, PayloadData withPayload, Date timestamp) {
        this.timestamp = timestamp;
        this.proximity = didMeasure;
        this.payload = withPayload;
    }

    public Encounter(Proximity didMeasure, PayloadData withPayload) {
        this(didMeasure, withPayload, new Date());
    }

    public Encounter(String row) {
        final String[] fields = row.split(",", -1);
        if (!(fields.length >= 6)) {
            return;
        }
        try {
            if (fields[0] != null && !fields[0].isEmpty()) {
                this.timestamp = dateFormatter.parse(fields[0]);
            }
        } catch (Throwable e) {
        }
        Calibration calibration = null;
        try {
            if (fields[3] != null && fields[4] != null && !fields[3].isEmpty() && !fields[4].isEmpty()) {
                final double calibrationValue = Double.parseDouble(fields[3]);
                final CalibrationMeasurementUnit calibrationUnit = CalibrationMeasurementUnit.valueOf(fields[4]);
                calibration = new Calibration(calibrationUnit, calibrationValue);
            }
        } catch (Throwable e) {
        }
        try {
            if (fields[1] != null && fields[2] != null && !fields[1].isEmpty() && !fields[2].isEmpty()) {
                final double proximityValue = Double.parseDouble(fields[1]);
                final ProximityMeasurementUnit proximityUnit = ProximityMeasurementUnit.valueOf(fields[2]);
                this.proximity = new Proximity(proximityUnit, proximityValue, calibration);
            }
        } catch (Throwable e) {
        }
        if (fields[5] != null) {
            this.payload = new PayloadData(fields[5]);
        }
    }

    public String csvString() {
        final String f0 = (timestamp == null ? "" : dateFormatter.format(timestamp));
        final String f1 = (proximity == null || proximity.value == null ? "" : proximity.value.toString());
        final String f2 = (proximity == null || proximity.unit == null ? "" : proximity.unit.name());
        final String f3 = (proximity == null || proximity.calibration == null || proximity.calibration.value == null ? "" : proximity.calibration.value.toString());
        final String f4 = (proximity == null || proximity.calibration == null || proximity.calibration.unit == null ? "" : proximity.calibration.unit.name());
        final String f5 = (payload == null ? "" : payload.base64EncodedString());
        return f0 + "," + f1 + "," + f2 + "," + f3 + "," + f4 + "," + f5;
    }

    public boolean isValid() {
        return timestamp != null && proximity != null && payload != null;
    }
}
