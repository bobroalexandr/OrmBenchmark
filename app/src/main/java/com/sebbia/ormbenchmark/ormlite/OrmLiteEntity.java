package com.sebbia.ormbenchmark.ormlite;


import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.sebbia.ormbenchmark.BenchmarkEntity;
import com.sebbia.ormbenchmark.Blob;
import com.sebbia.ormbenchmark.utils.Utils;

@DatabaseTable(tableName = "entity")
public class OrmLiteEntity implements BenchmarkEntity {
	@DatabaseField(columnName = "id", generatedId = true)
	private long id;
	@DatabaseField(columnName = "field1")
	private String field1;
	@DatabaseField(columnName = "field2")
	private String field2;
	@DatabaseField(columnName = "field3")
	private String field3;
	@DatabaseField(columnName = "field4")
	private int field4;
	@DatabaseField(columnName = "field5")
	private long field5;


	public OrmLiteEntity() {

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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
}
