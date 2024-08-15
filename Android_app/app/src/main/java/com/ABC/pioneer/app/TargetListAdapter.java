
package com.ABC.pioneer.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;


//用于在UI上显示目标列表的目标列表适配器
public class TargetListAdapter extends ArrayAdapter<Target> {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

    public TargetListAdapter(@NonNull Context context, List<Target> targets) {
        super(context, R.layout.listview_targets_row, targets);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final Target target = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_targets_row, parent, false);
        }
        final TextView textLabel = (TextView) convertView.findViewById(R.id.targetTextLabel);
        final TextView detailedTextLabel = (TextView) convertView.findViewById(R.id.targetDetailedTextLabel);
        SimpleDateFormat Dateformat=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        Dateformat.setTimeZone(TimeZone.getTimeZone("GMT+08"));
        final StringBuilder labelText = new StringBuilder(target.payloadData().shortName());

        final String didReceive =  " (接收时间 :  " + Dateformat.format(SpecificUsePayloadSupplier.parseStartTimeToLong(target.payloadData())) + ")";

        textLabel.setText(labelText.toString() + didReceive);
        if(SpecificUsePayloadSupplier.checkPayloadtime(target.payloadData())) {
            detailedTextLabel.setText("更新时间 : " + dateFormatter.format(target.lastUpdatedAt()));
        }
        return convertView;
    }
}
