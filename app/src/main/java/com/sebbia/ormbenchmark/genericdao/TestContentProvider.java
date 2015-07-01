package com.sebbia.ormbenchmark.genericdao;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sebbia.ormbenchmark.BenchmarkApp;

import java.util.List;

import alex.bobro.genericdao.GenericDaoContentProvider;
import alex.bobro.genericdao.GenericDaoSQLiteHelper;

/**
 * Created by alex on 5/13/15.
 */
public class TestContentProvider extends GenericDaoContentProvider {

    GenericDaoSQLiteHelper genericDaoSQLiteHelper;

    @Override
    public SQLiteOpenHelper getDbHelper() {
        if(genericDaoSQLiteHelper == null)
            genericDaoSQLiteHelper = new GenericDaoSQLiteHelper(BenchmarkApp.getInstance(),"lol",null,1) {
                @Override
                protected void appendSchemes(List<Class> classes) {

                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

                }
            };

        return genericDaoSQLiteHelper;
    }
}
