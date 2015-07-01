package com.sebbia.ormbenchmark.genericdao;

import android.content.Context;

import com.sebbia.ormbenchmark.Benchmark;
import com.sebbia.ormbenchmark.BenchmarkApp;

import java.util.List;

import alex.bobro.genericdao.ContextContentProvider;
import alex.bobro.genericdao.GenericDao;
import alex.bobro.genericdao.RequestParameters;
import alex.bobro.genericdao.Scheme;

/**
 * Created by alex on 6/18/15.
 */
public class GenericDaoBenchmark extends Benchmark<GenericDaoEntity> {

    @Override
    public void saveEntitiesInTransaction(List<GenericDaoEntity> entities) {
        GenericDao.getInstance().saveCollection(entities, new RequestParameters.Builder().withNotificationMode(RequestParameters.NotificationMode.AFTER_ALL).build(), null);
    }

    @Override
    public List<GenericDaoEntity> loadEntities() {
        return GenericDao.getInstance().getObjects(GenericDaoEntity.class);
    }

    @Override
    public void clearCache() {

    }

    @Override
    public String getName() {
        return "GenericDao";
    }

    @Override
    public Class<? extends GenericDaoEntity> getEntityClass() {
        return GenericDaoEntity.class;
    }


    @Override
    public void init(Context context) {
        super.init(context);
        Scheme.init(GenericDaoEntity.class);
        ContextContentProvider testContentProvider = new ContextContentProvider(BenchmarkApp.getInstance());
        GenericDao.init(testContentProvider);
        GenericDao.getInstance().delete(GenericDaoEntity.class);
    }
}
