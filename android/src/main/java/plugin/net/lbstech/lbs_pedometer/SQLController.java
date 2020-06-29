package plugin.net.lbstech.lbs_pedometer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

class SQLController {
    private final String TABLE_NAME = "pedometer";

    private static SQLController instance;
    private final SQLiteDatabase database;
    private static boolean isActivate = false;

    static SQLController getInstance(Context context){
        return instance == null
                ? instance = new SQLController(context)
                : instance;
    }

    private SQLController(Context context){
        // connect database
        final String DATABASE_NAME = "sensor";
        database = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        // create table
        database.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME
                + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "latitude REAL, " +
                        "longitude REAL, " +
                        "timestamp INTEGER, " +
                        "pressure REAL)");

        // 비정상 종료시 남아있는 데이터 삭제
        clear();
    }

    /**
     * SQLite DB에 insert 하는 메서드
     *
     * @param stepCnt 스텝 수
     * @param latitude 위도
     * @param longitude 경도
     */
    void insert(int stepCnt, double latitude, double longitude){
        synchronized (database){
            if( !isActivate ) isActivate = true;

            long timeMilli = Calendar.getInstance().getTimeInMillis();
            ContentValues values = new ContentValues();
            values.put("timestamp", timeMilli);
            values.put("stepCnt", stepCnt);
            values.put("latitude", latitude);
            values.put("longitude", longitude);
            database.insert(TABLE_NAME, null, values);
        }
    }

    /**
     * SQLite DB에 insert 하는 메서드
     * 모든 센서를 켰을 경우 사용하는 메서드이다.
     *
     */
    void insert(double lat, double lng, float pressure){
        synchronized (database){
            if( !isActivate ) isActivate = true;

            long timeMilli = Calendar.getInstance().getTimeInMillis();
            ContentValues values = new ContentValues();
            values.put("timestamp", timeMilli);
            values.put("latitude", lat);
            values.put("longitude", lng);
            values.put("pressure", pressure);
            database.insert(TABLE_NAME, null, values);
        }
    }

    /**
     *  테이블 데이터 삭제.
     */
    void clear(){
        synchronized (database){
            if (isActivate) isActivate = false;
            database.delete(TABLE_NAME, null, null);
        }
    }

    /**
     * 기록을 시작하고 App 을 종료한 이후 다시 시작하면 해당 메서드를 통해서 이전의 기록 모두 불러온다.
     *
     * @return 전체 쿼리 결과를 담은 list
     */
    ArrayList<HashMap<String, Object>> getHistory(){
        if ( !isActivate ) return null;

        long timeMilli = Calendar.getInstance().getTimeInMillis();
        Cursor cursor;
        synchronized (database){
            cursor = database.query(TABLE_NAME, null,
                "timestamp < ?", new String[]{String.valueOf(timeMilli)},
                null, null, "timestamp");
        }
        if (cursor != null){
            ArrayList<HashMap<String, Object>> result = new ArrayList<>();
            while(cursor.moveToNext()){
                HashMap<String, Object> row = new HashMap<>();
                row.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
                row.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
                row.put("latitude", cursor.getDouble(cursor.getColumnIndex("latitude")));
                row.put("longitude", cursor.getDouble(cursor.getColumnIndex("longitude")));
                row.put("pressure", cursor.getFloat(cursor.getColumnIndex("pressure")));
                result.add(row);
            }
            cursor.close();
            return result;
        }else{
            Log.i("PEDOLOG_SC", "getHistory...조회하려는 테이블이 비어있습니다.");
            return null;
        }
    }

    /**
     * 스텝수를 이용해서 마지막으로 저장한 값을 쿼리하는 메서드이다.
     *
     * @param stepCnt [{@link ServiceSensor}]가 센서값을 저장할 때 마다 static 변수인 [currentSavedStepCnt]에
     *                저장한다. 즉, 마지막으로 저장한 step 의 값이다.
     * @return 조회된 row 의 데이터를 Map으로 변환하여 전달.
     */
    HashMap<String, Object> getCurrentWhereStep(int stepCnt){
        Cursor cursor;
        synchronized (database){
            cursor = database.query(TABLE_NAME, null,
                    "timestamp = ?", new String[]{String.valueOf(stepCnt)},
                    null, null, "step");
        }
        if (cursor != null && cursor.moveToLast()){
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
            map.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
            map.put("latitude", cursor.getDouble(cursor.getColumnIndex("latitude")));
            map.put("longitude", cursor.getDouble(cursor.getColumnIndex("longitude")));
            map.put("pressure", cursor.getFloat(cursor.getColumnIndex("pressure")));
            cursor.close();
            return map;
        }else {
            Log.i("PEDOLOG_SC", "getCurrentWhereStep - 데이터 가져오기 실패");
            return null;
        }
    }


    HashMap<String, Object> getCurrent(){
        if(!isActivate) return new HashMap<>();

        Cursor cursor;
        synchronized (database){
            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY _id DESC LIMIT 1";
            cursor = database.rawQuery(query, null);
        }
        if (cursor != null && cursor.moveToFirst()){
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
            map.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
            map.put("latitude", cursor.getDouble(cursor.getColumnIndex("latitude")));
            map.put("longitude", cursor.getDouble(cursor.getColumnIndex("longitude")));
            map.put("pressure", cursor.getFloat(cursor.getColumnIndex("pressure")));
            cursor.close();
            return map;
        }else {
            Log.i("PEDOLOG_SC", "getCurrentWhereStep - 데이터 가져오기 실패");
            return null;
        }
    }

}
