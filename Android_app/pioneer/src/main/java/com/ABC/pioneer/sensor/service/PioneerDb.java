package com.ABC.pioneer.sensor.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;


import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.TimeInterval;
import com.ABC.pioneer.sensor.payload.Crypto.HMACSHA256;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;
import com.ABC.pioneer.sensor.payload.Crypto.ContactIdentifier;
import com.ABC.pioneer.sensor.payload.Crypto.ContactKey;
import com.ABC.pioneer.sensor.payload.Crypto.GenerateKey;
import com.ABC.pioneer.sensor.payload.Crypto.MatchingKey;

import java.nio.charset.StandardCharsets;


public class PioneerDb extends SQLiteOpenHelper{
    private static final int table_num = 15; // 我们一共要存15天的表
    private static final long periodMillis = TimeInterval.minutes(6).millis();
    private static final long dayMillis = TimeInterval.minutes(24*60).millis();
    // 第一个表存放的是当前天的数据，之后往前推
    public PioneerDb(Context context, String name, CursorFactory factory,int version)
    {
        super(context,name,factory,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        for(int i=1;i<=table_num;i++)
        {
            String table_name = "payload" + i;
            db.execSQL("create table "+ table_name +" (id INTEGER PRIMARY KEY AUTOINCREMENT, contactidentifier VARCHAR(20), mac BLOB,rawdata BLOB)");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,int oldVersion, int newVersion){
    }

    public int getTableNum()
    {
        return table_num;
    }

    // 清空当前数据库
    public void deleteCurTable()
    {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        String expTb = "payload" + 1;
        database.execSQL("delete from " + expTb);
        database.close();
    }

    // 每天晚上12点更新
    public void updateTable()
    {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            // 清空过期的表
            String expTb = "payload" + table_num;
            database.execSQL("delete from " + expTb);

            // 先暂存这张表
            database.execSQL("ALTER TABLE " + expTb + " RENAME TO " + "tmpTb");
            // 前13张表后移

            for(int i=table_num-1;i>=1;i--)
            {
                String old_Tb = "payload" + i;
                String new_Tb = "payload" + (i+1);
                database.execSQL("ALTER TABLE " + old_Tb + " RENAME TO " + new_Tb);
            }

            // 把暂存的表设为最新的表
            database.execSQL("ALTER TABLE " + "tmpTb" + " RENAME TO " + "payload1");

            database.setTransactionSuccessful();
        }
        finally
        {
            database.endTransaction();
            database.close();
        }
    }


    // 插入数据
    public void insertPayloadData(PayloadData payloadData) {
        if (!(payloadData == null || payloadData.value.length == 0)) {
            SQLiteDatabase database = getWritableDatabase();
            // 解析payload数据
            Data contactidentifer = SpecificUsePayloadSupplier.parseContactIdentifier(payloadData);
            Data mac = SpecificUsePayloadSupplier.parseMac(payloadData);
            Data rawdata = SpecificUsePayloadSupplier.parseRawData(payloadData);
            // 插入到数据库的今天的表中
            // 由于mac是乱码，所以直接存BLOB结构
            ContentValues values = new ContentValues();
            values.put("contactidentifier", new String(contactidentifer.value, StandardCharsets.UTF_8));
            values.put("mac", mac.value);
            values.put("rawdata", rawdata.value);
            database.insert("payload1", null, values);
            database.close();
        }
    }

    // 根据matchingkey 以及 距离当日的offset天数 进行匹配
    public boolean matchMatchingKey(MatchingKey matchingKey,int offset)
    {

        // 由matchingKey生成contactIdentifiers
        final ContactKey[] contactKeys = GenerateKey.contactKeys(matchingKey);
        final ContactIdentifier[] contactIdentifiers = new ContactIdentifier[contactKeys.length];
        for (int i=contactKeys.length; i-->0;) {
            contactIdentifiers[i] = GenerateKey.contactIdentifier(contactKeys[i]);
        }

        // 去对应的数据库表中查找有没有匹配的contactIdentifier
        SQLiteDatabase db = getWritableDatabase();
        for(int i=contactIdentifiers.length-1;i>=0;i--)
        {
            if(matchContactIdentifier(db,offset,matchingKey,contactIdentifiers[i],i))
                return true;
        }
        db.close();
        return false;
    }

    //
    private static boolean matchContactIdentifier(SQLiteDatabase database,int offset,MatchingKey matchingKey,ContactIdentifier contactIdentifier,int period)
    {
        // 取出所有contactIdentifier对应的记录
        String tbName = "payload" + (offset+1);
        String contactIdentifierStr = new String(contactIdentifier.value,StandardCharsets.UTF_8);
        Cursor cursor = database.rawQuery("SELECT * FROM " + tbName + " WHERE contactidentifier = ?",new String[]{contactIdentifierStr});


        while(cursor.moveToNext())
        {
            // 如果找到则进行mac验证
            // mac验证
            byte [] macBytes = cursor.getBlob(cursor.getColumnIndex("mac"));
            byte [] rawdataBytes = cursor.getBlob(cursor.getColumnIndex("rawdata"));
            try {
                if (HMACSHA256.VerifyMAC(rawdataBytes, matchingKey.value, macBytes))
                {
                    cursor.close();
                    return true;
                }
                cursor.close();
                return true;
            }
            catch(Exception e)
            {
                cursor.close();
                return false;
            }
        }
        cursor.close();
        return false;
    }


}
