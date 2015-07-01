package com.sebbia.ormbenchmark;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.content.Context;

import com.sebbia.ormbenchmark.utils.Utils;

public abstract class Benchmark<T extends BenchmarkEntity> {

	public abstract void saveEntitiesInTransaction(List<T> entities);
	public abstract List<T> loadEntities();
	public abstract void clearCache();
	public abstract String getName();
	public abstract Class<?  extends T> getEntityClass();
	
	public void init(Context context) {}
	public void dispose(Context context) {}

	Random random = new Random();

	List<T> generateEntities(int count) {
		try {
			List<T> entities = new ArrayList<T>();
			String field1 = Utils.getRandomString(50);
			String field2 = Utils.getRandomString(100);
			String field3 = Utils.getRandomString(10);
			int field4 = random.nextInt();
			long field5 = random.nextLong();
			for (int i = 0; i < count; ++i) {
				T benchmarkEntity = getEntityClass().newInstance();
				benchmarkEntity.setField1(field1);
				benchmarkEntity.setField2(field2);
				benchmarkEntity.setField3(field3);
				benchmarkEntity.setField4(field4);
				benchmarkEntity.setField5(field5);
				entities.add(benchmarkEntity);
			}
			return entities;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
