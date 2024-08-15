package com.ABC.pioneer.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.ABC.pioneer.app.MainActivity;
import com.ABC.pioneer.app.R;
import com.ABC.pioneer.app.TokenActivity;


public class FragmentActivity2 extends Fragment {
    LinearLayout token_pre;
    private final Context context = MainActivity.getInstance();
    private AlertDialog dialog;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saveInstanceState){
        android.view.View view = inflater.inflate(R.layout.activity_token_pre,container,false);
        initView(view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        token_pre.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent it=new Intent(getContext(), TokenActivity.class);//启动TokenActivity
                startActivity(it);
            }
        });

    }

    private void initView(View view){
        token_pre = (LinearLayout) view.findViewById(R.id.token_pre);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saveInstanceState) {
        super.onViewCreated(view, saveInstanceState);
    }


}
