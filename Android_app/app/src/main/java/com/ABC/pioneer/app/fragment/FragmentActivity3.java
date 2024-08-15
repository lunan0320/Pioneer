package com.ABC.pioneer.app.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ABC.pioneer.app.R;
import com.ABC.pioneer.sensor.SensorArray;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;

import java.util.Date;

public class FragmentActivity3 extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstanceState){
        android.view.View view = inflater.inflate(R.layout.activity_user,container,false);
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        ((TextView) getActivity().findViewById(R.id.device)).setText("设备名:" + SensorArray.deviceDescription);
        ((TextView) getActivity().findViewById(R.id.payload)).setText("本机数据包:"+SpecificUsePayloadSupplier.updatePayload(new Date()).shortName());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saveInstanceState){
        super.onViewCreated(view, saveInstanceState);

    }



}
