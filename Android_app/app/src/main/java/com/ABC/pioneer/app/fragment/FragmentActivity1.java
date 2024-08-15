package com.ABC.pioneer.app.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ABC.pioneer.app.MainActivity;
import com.ABC.pioneer.app.R;
import com.ABC.pioneer.app.Target;
import com.ABC.pioneer.app.TargetListAdapter;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.ImmediateSendData;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.Proximity;
import com.ABC.pioneer.sensor.datatype.SensorType;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;
import com.ABC.pioneer.sensor.service.PioneerDb;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class FragmentActivity1 extends Fragment implements  AdapterView.OnItemClickListener, SensorDelegate {
    public static boolean foreground = false;
    private long didDetect = 0, didRead = 0, didMeasure = 0, didShare = 0, didReceive = 0;
    private final Map<TargetIdentifier, PayloadData> targetIdentifiers = new ConcurrentHashMap<>();
    private final Map<PayloadData, Target> payloads = new ConcurrentHashMap<>();
    private final List<Target> targets = new ArrayList<>();
    private TargetListAdapter targetListAdapter = null;
    private AlertDialog dialog;
    private View view;
    private final Context context = MainActivity.getInstance();
    private TextView Read;
    private TextView Measure;
    private TextView Detect;
    private TextView Share;
    private TextView count;
    private ListView targetsListView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstanceState){
        android.view.View view = inflater.inflate(R.layout.activity_bluetooth,container,false);
        this.view = view;
        initView(view);
        return view;
    }

    private void initView(View view){
        Read = (TextView)view.findViewById(R.id.didReadCount);
        Measure = (TextView)view.findViewById(R.id.didMeasureCount);
        Detect = (TextView)view.findViewById(R.id.didDetectCount);
        Share = (TextView)view.findViewById(R.id.didShareCount);
        count = (TextView)view.findViewById(R.id.detection);
        targetsListView = (ListView)view.findViewById(R.id.targets);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        //测试特定于UI的过程，以从传感器收集数据进行展示
        @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch onOffSwitch = Objects.requireNonNull(getActivity()).findViewById(R.id.sensorOnOffSwitch);
        MainActivity.sensor.add(this);
        onOffSwitch.setChecked(true);
        MainActivity.sensor.start();
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    MainActivity.sensor.start();
                } else {
                    MainActivity.sensor.stop();
                }
            }
        });
        //测试特定于UI的过程，以从传感器收集数据进行展示
        targetListAdapter = new TargetListAdapter(context, targets);
        targetsListView.setAdapter(targetListAdapter);
        targetsListView.setOnItemClickListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saveInstanceState){
        super.onViewCreated(view, saveInstanceState);

    }
    // 更新目标表
    private synchronized void updateTargets() {
        // 根据短名称删除重复目标，并在时间戳上最后更新
        final Map<String,Target> shortNames = new HashMap<>(payloads.size());
        for (Map.Entry<PayloadData,Target> entry : payloads.entrySet()) {
            final String shortName = entry.getKey().shortName();
            final Target target = entry.getValue();
            final Target duplicate = shortNames.get(shortName);
            if (duplicate == null || duplicate.lastUpdatedAt().getTime() < target.lastUpdatedAt().getTime()) {
                shortNames.put(shortName, target);
            }
        }
        // 按字母顺序获取目标列表
        final List<Target> targetList = new ArrayList<>(shortNames.values());
        Collections.sort(targetList, new Comparator<Target>() {
            @Override
            public int compare(Target t0, Target t1) {
                return t0.payloadData().shortName().compareTo(t1.payloadData().shortName());
            }
        });
        // 更新用户界面
        count.setText("接收数据包 (" + targetListAdapter.getCount() + ")");
        targetListAdapter.clear();
        targetListAdapter.addAll(targetList);
    }
    // 更新相应检测结果
    private void updateCounts() {
        Detect.setText(Long.toString(this.didDetect));
        Read.setText(Long.toString(this.didRead));
        Measure.setText(Long.toString(this.didMeasure));
        Share.setText(Long.toString(this.didShare));

    }

    @Override
    public void onResume() {
        super.onResume();
        foreground = true;
        updateCounts();
        updateTargets();
    }

    @Override
    public void onPause() {
        foreground = false;
        super.onPause();
    }



    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        this.didDetect++;
        if (foreground) {
            final String text = Long.toString(this.didDetect);
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Detect.setText(text);
                }
            });
        }
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        this.didRead++;
        targetIdentifiers.put(fromTarget, didRead);
        Target target = payloads.get(didRead);
        if (target != null) {
            target.didRead(new Date());
        } else {
            payloads.put(didRead, new Target(fromTarget, didRead));
        }
        if (foreground) {
            final String text = Long.toString(this.didRead);
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Read.setText(text);
                    updateTargets();
                }
            });
        }
        PioneerDb db  = new PioneerDb(context,"payloads",null,1);
        db.insertPayloadData(didRead);
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        this.didShare++;
        final Date now = new Date();
        for (PayloadData didRead : didShare) {
            targetIdentifiers.put(fromTarget, didRead);
            Target target = payloads.get(didRead);
            if (target != null) {
                target.didRead(new Date());
            } else {
                payloads.put(didRead, new Target(fromTarget, didRead));
            }
        }
        if (foreground) {
            final String text = Long.toString(this.didShare);
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Share.setText(text);
                    updateTargets();
                }
            });
        }
    }



    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        this.didMeasure++;
        final PayloadData didRead = targetIdentifiers.get(fromTarget);
        if (didRead != null) {
            final Target target = payloads.get(didRead);
            if (target != null) {
                target.targetIdentifier(fromTarget);
                target.proximity(didMeasure);
            }
        }
        if (foreground) {
            final String text = Long.toString(this.didMeasure);
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Measure.setText(text);
                }
            });
        }
    }

    @Override
    public void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget) {
        this.didReceive++;
        final PayloadData didRead = new PayloadData(didReceive.data.value);
        if (didRead != null) {
            final Target target = payloads.get(didRead);
            if (target != null) {
                targetIdentifiers.put(fromTarget, didRead);
                target.targetIdentifier(fromTarget);
                target.received(didReceive);
            }
        }
        if (foreground) {
            final String text = Long.toString(this.didReceive);
            Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateTargets();
                }
            });
        }
    }




    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int i, long l) {
        final Target target = targetListAdapter.getItem(i);
        final PayloadData payloadData = target.payloadData();
        SimpleDateFormat dateformat=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        dateformat.setTimeZone(TimeZone.getTimeZone("GMT+08"));
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        LayoutInflater inflater = (LayoutInflater) Objects.requireNonNull(getContext()).getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageview = inflater.inflate(R.layout.activity_payload, null);
        TextView ContactIdentifier = (TextView)messageview.findViewById(R.id.ContactIdentifier);
        TextView StartTime = (TextView)messageview.findViewById(R.id.StartTime);
        TextView EndTime = (TextView)messageview.findViewById(R.id.EndTime);
        ContactIdentifier.append(SpecificUsePayloadSupplier.parseContactIdentifierToStr(payloadData));
        final long startTime = SpecificUsePayloadSupplier.parseStartTimeToLong(payloadData);
        StartTime.append(dateformat.format(startTime));
        EndTime.append(dateformat.format(startTime+360000));
        builder.setView(messageview);
        builder.create();
        dialog = builder.show();
    }

}
