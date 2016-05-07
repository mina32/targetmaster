package org.boofcv.objecttracking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.SQLException;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by moraffy on 5/4/2016.
 */
public class TargetMasterData {

    private static TargetMasterData sInstance;

    public static final String TAG = "TargetMasterData";

    private static String DB_PATH = "";
    public static final String DB_NAME = "TargetMasterAsset.db";
    public static final int DB_VERSION = 1;

    public static final String TABLE = "TargetMaster";
//    public static final String USERS_TABLE = "Users";


    //public static final String C_ID = "_id";    // TODO Probably use this for users
    public static final String TIME_TEXT = "time";
    public static final String DATE_TEXT = "date";
    public static final String DISTANCE_TEXT = "distance";
    public static final String WIND_SPEED_TEXT = "wind_speed";
    public static final String WIND_DIRECTION_TEXT = "wind_direction";
    public static final String LOCATION_TEXT = "location";
    public static final String VIDEO_PATH_TEXT = "videopath";



    private final Context context;
    private DbHelper dbHelper;
    private SQLiteDatabase db;

    // Constructor
    private TargetMasterData(Context context) {
        Log.d(TAG, "Constructor ");
        this.context = context;
        dbHelper = new DbHelper();

        //Asset code
        createDatabase();
        open();

        //Cursor testdata = mDbHelper.getTestData();
        //mDbHelper.close();
    }

    public static synchronized TargetMasterData getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new TargetMasterData(context.getApplicationContext());
        }
        return sInstance;
    }

    // Query, Insert, update
    //Sample Query
    public Cursor getTestData()
    {
        try
        {
            String sql ="SELECT * FROM " + TABLE;

            Cursor mCur = db.rawQuery(sql, null);
            if (mCur!=null)
            {
                mCur.moveToNext();
            }
            return mCur;
        }
        catch (SQLException mSQLException)
        {
            Log.e(TAG, "getTestData >>"+ mSQLException.toString());
            throw mSQLException;
        }
    }

    public long SaveInfo(String time, String date, String distance,  String wind_speed,
                         String wind_direction, String location, String video_path)
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(TIME_TEXT, time);
        initialValues.put(DATE_TEXT, date);
        initialValues.put(DISTANCE_TEXT, distance);
        initialValues.put(WIND_SPEED_TEXT, wind_speed);
        initialValues.put(WIND_DIRECTION_TEXT, wind_direction);
        initialValues.put(LOCATION_TEXT, location);
        initialValues.put(VIDEO_PATH_TEXT, video_path);

        return db.insert(TABLE, null, initialValues);

    }

    public Cursor fetchQuestions(String inputText) throws SQLException {
        Log.w(TAG, inputText);
        Cursor mCursor = null;
        if (inputText == null  ||  inputText.length () == 0)  {
            mCursor = db.query(TABLE, null,
/*                  new String[] {KEY_ROWID, KEY_CODE,
                          KEY_NAME, KEY_CONTINENT, KEY_REGION},*/
                    null, null, null, null, null);

        }
        else {
            mCursor = db.query(TABLE, null,
/*                    SQLITE_TABLE, new String[] {KEY_ROWID,
                            KEY_CODE, KEY_NAME, KEY_CONTINENT, KEY_REGION},*/
                    LOCATION_TEXT + " like '%" + inputText + "%'", null,
                    null, null, null, null);
        }
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    // Asset code
    public TargetMasterData createDatabase() throws SQLException {
        try {
            dbHelper.createDataBase();
        } catch (IOException mIOException) {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
        return this;
    }

    //Asset code
    public TargetMasterData open() throws SQLException {
        try {
            dbHelper.openDataBase();
            dbHelper.close();
            db = dbHelper.getReadableDatabase();
        } catch (SQLException mSQLException) {
            Log.e(TAG, "open >>" + mSQLException.toString());
            throw mSQLException;
        }
        return this;
    }

    //Asset code
    public void close() {
        dbHelper.close();
    }


    class DbHelper extends SQLiteOpenHelper {

        public DbHelper() {
            super(context, DB_NAME, null, DB_VERSION);
            //Asset code
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
            } else {
                DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
            }
        }

        //Asset code
        public void createDataBase() throws IOException {
            //If the database does not exist, copy it from the assets.

            boolean mDataBaseExist = checkDataBase();
            if (!mDataBaseExist) {
                this.getReadableDatabase();
                this.close();
                try {
                    //Copy the database from assets
                    copyDataBase();
                    Log.e(TAG, "createDatabase database created");
                } catch (IOException mIOException) {
                    throw new Error("ErrorCopyingDataBase");
                }
            }
        }

        //Asset code
        //Open the database, so we can query it
        public boolean openDataBase() throws SQLException {
            String mPath = DB_PATH + DB_NAME;
            //Log.v("mPath", mPath);
            db = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
            //mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            return db != null;
        }

        //Asset code
        //Check that the database exists here: /data/data/your package/databases/Da Name
        private boolean checkDataBase() {
            File dbFile = new File(DB_PATH + DB_NAME);
            //Log.v("dbFile", dbFile + "   "+ dbFile.exists());
            return dbFile.exists();
        }

        //Asset code
        //Copy the database from assets
        private void copyDataBase() throws IOException {
            InputStream mInput = context.getAssets().open(DB_NAME);
            String outFileName = DB_PATH + DB_NAME;
            OutputStream mOutput = new FileOutputStream(outFileName);
            byte[] mBuffer = new byte[1024];
            int mLength;
            while ((mLength = mInput.read(mBuffer)) > 0) {
                mOutput.write(mBuffer, 0, mLength);
            }
            mOutput.flush();
            mOutput.close();
            mInput.close();
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "onCreate");
/*            String sql = String.format("create table %s (%s int primary key, %s int, %s text, %s text)",
                    QUESTION_TABLE, C_ID, QN_TEXT, ANS_BOOL, HINT_TEXT);
            Log.d(TAG, sql);
            db.execSQL(sql);*/
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade");
            db.execSQL("drop table if exists " + TABLE);
            onCreate(db);
        }

        @Override
        public synchronized void close() {
            if (db != null)
                db.close();
            super.close();
        }
    }
}

