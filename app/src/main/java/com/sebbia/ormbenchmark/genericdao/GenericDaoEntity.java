package com.sebbia.ormbenchmark.genericdao;



import com.sebbia.ormbenchmark.BenchmarkEntity;

import alex.bobro.genericdao.annotation.FieldAnnotation;
import alex.bobro.genericdao.annotation.TableAnnotation;
import alex.bobro.genericdao.entities.SQLiteType;

@TableAnnotation(tableName = "genericDAoTable")
public class GenericDaoEntity implements BenchmarkEntity {

	@FieldAnnotation
	private String field1;
	@FieldAnnotation
	private String field2;
	@FieldAnnotation
	private String field3;
	@FieldAnnotation(dbType = SQLiteType.INTEGER)
	private int field4;
	@FieldAnnotation(dbType = SQLiteType.INTEGER)
	private long field5;


	public GenericDaoEntity() {

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
