package com.sebbia.ormbenchmark.activeandroid.sebbia;

import java.util.Date;


import com.activeandroid.sebbia.Model;
import com.activeandroid.sebbia.annotation.Column;
import com.activeandroid.sebbia.annotation.Table;
import com.sebbia.ormbenchmark.BenchmarkEntity;
import com.sebbia.ormbenchmark.Blob;
import com.sebbia.ormbenchmark.utils.Utils;

@Table(name = "entity")
public class ActiveAndroidSebbiaEntity extends Model implements BenchmarkEntity {

	@Column(name = "field1")
	String field1;
	@Column(name = "field2")
	String field2;
	@Column(name = "field3")
	String field3;
	@Column(name = "field4")
	int field4;
	@Column(name = "field5")
	long field5;

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
