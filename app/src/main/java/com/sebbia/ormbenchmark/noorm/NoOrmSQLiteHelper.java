package com.sebbia.ormbenchmark.noorm;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.provider.BaseColumns;

public class NoOrmSQLiteHelper extends SQLiteOpenHelper {

	static final class EntityTable implements BaseColumns {
		static final String TABLE = "entity";
		static final String ID = BaseColumns._ID;
		static final String FIELD_1 = "field1";
		static final String FIELD_2 = "field2";
		static final String FIELD_3 = "field3";
		static final String FIELD_4 = "field4";
		static final String FIELD_5 = "field5";
	}

	private static final int DATABASE_VERSION = 1;

	private SQLiteStatement insertStatement;

	public NoOrmSQLiteHelper(Context context) {
		super(context, "no_orm", null, DATABASE_VERSION);
		createInsertStatement();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS "
				+ EntityTable.TABLE
				+ " (" + EntityTable.ID + " INTEGER PRIMARY KEY AUTOINCREMENT"
				+ ", " + EntityTable.FIELD_1 + " TEXT"
				+ ", " + EntityTable.FIELD_2 + " TEXT"
				+ ", " + EntityTable.FIELD_3 + " TEXT"
				+ ", " + EntityTable.FIELD_4 + " INTEGER"
				+ ", " + EntityTable.FIELD_5 + " INTEGER"
				+ ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + EntityTable.TABLE);
		onCreate(db);
	}

	public void insertInTransaction(List<NoOrmEntity> entities) {
		SQLiteDatabase db = getWritableDatabase();
		try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                db.beginTransactionNonExclusive();
            } else {
                db.beginTransaction();
            }
			for (NoOrmEntity entity : entities) {
				insert(entity);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	public void insert(NoOrmEntity entity) {
		insertStatement.clearBindings();
		entity.bindToStatement(insertStatement);
		insertStatement.executeInsert();
	}

	public List<NoOrmEntity> getAllEntities() {
		List<NoOrmEntity> result = new ArrayList<NoOrmEntity>();
		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(EntityTable.TABLE, null, null, null, null, null, null);
			while (cursor.moveToNext()) {
				result.add(NoOrmEntity.fromCursor(cursor));
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}
	
	public NoOrmEntity findEntity(long id) {
		NoOrmEntity entity = null;
		Cursor cursor = null;
		try {
			cursor = getReadableDatabase().query(EntityTable.TABLE, null, EntityTable.ID + " =?", new String[]{ String.valueOf(id) }, null, null, null);
			if (cursor.moveToNext())
				entity = NoOrmEntity.fromCursor(cursor);
		} finally {
			cursor.close();
		}
		return entity;
	}

	private void createInsertStatement() {
		insertStatement = getWritableDatabase().compileStatement(
				"INSERT INTO " + EntityTable.TABLE
						+ " (" + EntityTable.FIELD_1
						+ "," + EntityTable.FIELD_2
						+ "," + EntityTable.FIELD_3
						+ "," + EntityTable.FIELD_4
						+ "," + EntityTable.FIELD_5 + ")"
						+ " VALUES (?, ?, ?, ?, ?)");
	}
}
