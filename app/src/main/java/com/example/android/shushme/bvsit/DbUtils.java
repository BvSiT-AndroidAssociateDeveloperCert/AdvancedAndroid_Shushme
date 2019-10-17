package com.example.android.shushme.bvsit;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class DbUtils {
    public static void dumpTable(Context context, Uri uri){
        //BvS log the full database table
        //Ex. URI SquawkProvider.SquawkMessages.CONTENT_URI
        Cursor cursor = context.getContentResolver().query(uri,null,null,null,null);
        while (cursor.moveToNext()){
            List<String> row = new LinkedList<>();
            for (int i=0;i<cursor.getColumnCount();i++){
                row.add(cursor.getColumnName(i)+"=" + cursor.getString(i));
            }
            Log.d("dumpTable", android.text.TextUtils.join(",", row));
        }
    }
}
