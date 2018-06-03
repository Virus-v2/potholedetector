package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

@Database(entities = {LocalTripEntity.class}, version = 1)
@TypeConverters({MyLocationConverter.class})
public abstract class LocalTripDatabase extends RoomDatabase {
    private static LocalTripDatabase instance;
    public static LocalTripDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (LocalTripDatabase.class) {
                if (instance == null) {
                    //creating database instance
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            LocalTripDatabase.class, "LocalTripDatabase")
                            .addCallback(roomDatabaseCallback).build();
                }
            }
        }
        return instance;
    }
    public abstract LocalTripTableDao localTripTableDao();
    private static RoomDatabase.Callback roomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            //this method is called when app is started
            super.onOpen(db);
            new PopulateDBAsync(instance).execute();
        }
    };

    private static class PopulateDBAsync extends AsyncTask<Void, Void, Void> {

        private final LocalTripTableDao localTripTableDao;

        public PopulateDBAsync(LocalTripDatabase instance) {
            localTripTableDao = instance.localTripTableDao();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //TODO: CHECK WHETHER TO INCLUDE DELETE AND INSERT FUNCTIONS
            localTripTableDao.getAllTrips();
            return null;
        }
    }
}
