package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {LocalTripTable.class}, version = 1)
public abstract class LocalTripDatabase extends RoomDatabase {
    private static LocalTripDatabase instance;
    public static LocalTripDatabase getInstance(final Context context) {
        if (instance == null) {
            synchronized (LocalTripDatabase.class) {
                if (instance == null) {
                    //creating database instance
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            LocalTripDatabase.class, "LocalTripDatabase").build();
                }
            }
        }
        return instance;
    }
    public abstract LocalTripTableDao localTripTableDao();
}
