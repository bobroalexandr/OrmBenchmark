package com.sebbia.ormbenchmark.noorm;

import java.util.Date;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.orm.dsl.Table;
import com.sebbia.ormbenchmark.BenchmarkEntity;
import com.sebbia.ormbenchmark.Blob;
import com.sebbia.ormbenchmark.utils.Utils;

@Table(name = "entity")
public class NoOrmEntity implements BenchmarkEntity {
	
	public static NoOrmEntity fromCursor(Cursor cursor) {
		NoOrmEntity noOrmEntity = new NoOrmEntity();
		noOrmEntity.field1 = cursor.getString(1);
		noOrmEntity.field2 = cursor.getString(2);
		noOrmEntity.field3 = cursor.getString(3);
		noOrmEntity.field4 = cursor.getInt(4);
		noOrmEntity.field5 = cursor.getLong(5);
		return noOrmEntity;
	}
	
	private String field1;
	private String field2;
	private String field3;
	private int field4;
	private long field5;

	public NoOrmEntity() {

	}

	@Override
	public String getField1() {
		return field1;
	}

	@Override
	public void setField1(String field1) {
		this.field1 = field1;
	}

	@Override
	public String getField2() {
		return field2;
	}

	@Override
	public void setField2(String field2) {
		this.field2 = field2;
	}

	@Override
	public String getField3() {
		return field3;
	}

	@Override
	public void setField3(String field3) {
		this.field3 = field3;
	}

	@Override
	public int getField4() {
		return field4;
	}

	@Override
	public void setField4(int field4) {
		this.field4 = field4;
	}

	@Override
	public long getField5() {
		return field5;
	}

	@Override
	public void setField5(long field5) {
		this.field5 = field5;
	}

	public void bindToStatement(SQLiteStatement statement) {
		statement.bindString(1, field1);
		statement.bindString(2, field2);
		statement.bindString(3, field3);
		statement.bindLong(4, field4);
		statement.bindLong(5, field5);
	}

}
