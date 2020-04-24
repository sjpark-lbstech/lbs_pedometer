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
                        "stepCnt INTEGER, " +
                        "latitude REAL, " +
                        "longitude REAL, " +
                        "timestamp INTEGER, " +
                        "acX REAL, acY REAL, acZ REAL, " +
                        "gyX REAL, gyY REAL, gyZ REAL, " +
                        "maX REAL, maY REAL, maZ REAL, " +
                        "veX REAL, veY REAL, veZ REAL, veCos REAL, veAccuracy REAL, " +
                        "pressure REAL, " +
                        "tmpDegree REAL," +
                        "humidity REAL, " +
                        "step INTEGER, motion INTEGER, sigMotion INTEGER)");

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
    void insert(int stepCnt, double lat, double lng, float acX, float acY, float acZ,
                float gyX, float gyY, float gyZ, float maX, float maY, float maZ,
                float veX, float veY, float veZ, float veCos, float veAccuracy,
                float pressure, float tmpDegree, float humidity,
                boolean step, boolean motion, boolean sigMotion){
        synchronized (database){
            if( !isActivate ) isActivate = true;

            long timeMilli = Calendar.getInstance().getTimeInMillis();
            ContentValues values = new ContentValues();
            values.put("timestamp", timeMilli);
            values.put("stepCnt", stepCnt);
            values.put("latitude", lat);
            values.put("longitude", lng);
            values.put("acX", acX);
            values.put("acY", acY);
            values.put("acZ", acZ);
            values.put("gyX", gyX);
            values.put("gyY", gyY);
            values.put("gyZ", gyZ);
            values.put("maX", maX);
            values.put("maY", maY);
            values.put("maZ", maZ);
            values.put("veX", veX);
            values.put("veY", veY);
            values.put("veZ", veZ);
            values.put("veCos", veCos);
            values.put("veAccuracy", veAccuracy);
            values.put("pressure", pressure);
            values.put("tmpDegree", tmpDegree);
            values.put("humidity", humidity);
            values.put("step", step ? 1 : 0);
            values.put("motion", motion ? 1 : 0);
            values.put("sigMotion", sigMotion ? 1 : 0);
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
                null, null, "step");
        }
        if (cursor != null){
            ArrayList<HashMap<String, Object>> result = new ArrayList<>();
            while(cursor.moveToNext()){
                HashMap<String, Object> row = new HashMap<>();
                row.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
                row.put("stepCnt", cursor.getInt(cursor.getColumnIndex("stepCnt")));
                row.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
                row.put("latitude", cursor.getDouble(cursor.getColumnIndex("latitude")));
                row.put("longitude", cursor.getDouble(cursor.getColumnIndex("longitude")));
                row.put("acX", cursor.getFloat(cursor.getColumnIndex("acX")));
                row.put("acY", cursor.getFloat(cursor.getColumnIndex("acY")));
                row.put("acZ", cursor.getFloat(cursor.getColumnIndex("acZ")));
                row.put("gyX", cursor.getFloat(cursor.getColumnIndex("gyX")));
                row.put("gyY", cursor.getFloat(cursor.getColumnIndex("gyY")));
                row.put("gyZ", cursor.getFloat(cursor.getColumnIndex("gyZ")));
                row.put("maX", cursor.getFloat(cursor.getColumnIndex("maX")));
                row.put("maY", cursor.getFloat(cursor.getColumnIndex("maY")));
                row.put("maZ", cursor.getFloat(cursor.getColumnIndex("maZ")));
                row.put("veX", cursor.getFloat(cursor.getColumnIndex("veX")));
                row.put("veY", cursor.getFloat(cursor.getColumnIndex("veY")));
                row.put("veZ", cursor.getFloat(cursor.getColumnIndex("veZ")));
                row.put("veCos", cursor.getFloat(cursor.getColumnIndex("veCos")));
                row.put("veAccuracy", cursor.getFloat(cursor.getColumnIndex("veAccuracy")));
                row.put("pressure", cursor.getFloat(cursor.getColumnIndex("pressure")));
                row.put("tmpDegree", cursor.getFloat(cursor.getColumnIndex("tmpDegree")));
                row.put("humidity", cursor.getFloat(cursor.getColumnIndex("humidity")));
                row.put("step", cursor.getInt(cursor.getColumnIndex("step")) == 1);
                row.put("motion", cursor.getInt(cursor.getColumnIndex("motion")) == 1);
                row.put("sigMotion", cursor.getInt(cursor.getColumnIndex("sigMotion")) == 1);
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
            map.put("stepCnt", cursor.getInt(cursor.getColumnIndex("stepCnt")));
            map.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
            map.put("latitude", cursor.getDouble(cursor.getColumnIndex("latitude")));
            map.put("longitude", cursor.getDouble(cursor.getColumnIndex("longitude")));
            map.put("acX", cursor.getFloat(cursor.getColumnIndex("acX")));
            map.put("acY", cursor.getFloat(cursor.getColumnIndex("acY")));
            map.put("acZ", cursor.getFloat(cursor.getColumnIndex("acZ")));
            map.put("gyX", cursor.getFloat(cursor.getColumnIndex("gyX")));
            map.put("gyY", cursor.getFloat(cursor.getColumnIndex("gyY")));
            map.put("gyZ", cursor.getFloat(cursor.getColumnIndex("gyZ")));
            map.put("maX", cursor.getFloat(cursor.getColumnIndex("maX")));
            map.put("maY", cursor.getFloat(cursor.getColumnIndex("maY")));
            map.put("maZ", cursor.getFloat(cursor.getColumnIndex("maZ")));
            map.put("veX", cursor.getFloat(cursor.getColumnIndex("veX")));
            map.put("veY", cursor.getFloat(cursor.getColumnIndex("veY")));
            map.put("veZ", cursor.getFloat(cursor.getColumnIndex("veZ")));
            map.put("veCos", cursor.getFloat(cursor.getColumnIndex("veCos")));
            map.put("veAccuracy", cursor.getFloat(cursor.getColumnIndex("veAccuracy")));
            map.put("pressure", cursor.getFloat(cursor.getColumnIndex("pressure")));
            map.put("tmpDegree", cursor.getFloat(cursor.getColumnIndex("tmpDegree")));
            map.put("humidity", cursor.getFloat(cursor.getColumnIndex("humidity")));
            map.put("step", cursor.getInt(cursor.getColumnIndex("step")) == 1);
            map.put("motion", cursor.getInt(cursor.getColumnIndex("motion")) == 1);
            map.put("sigMotion", cursor.getInt(cursor.getColumnIndex("sigMotion")) == 1);
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
            String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY id DESC LIMIT 1";
            cursor = database.rawQuery(query, null);
        }
        if (cursor != null && cursor.moveToFirst()){
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
            map.put("stepCnt", cursor.getInt(cursor.getColumnIndex("stepCnt")));
            map.put("timestamp", cursor.getLong(cursor.getColumnIndex("timestamp")));
            map.put("latitude", cursor.getDouble(cursor.getColumnIndex("latitude")));
            map.put("longitude", cursor.getDouble(cursor.getColumnIndex("longitude")));
            map.put("acX", cursor.getFloat(cursor.getColumnIndex("acX")));
            map.put("acY", cursor.getFloat(cursor.getColumnIndex("acY")));
            map.put("acZ", cursor.getFloat(cursor.getColumnIndex("acZ")));
            map.put("gyX", cursor.getFloat(cursor.getColumnIndex("gyX")));
            map.put("gyY", cursor.getFloat(cursor.getColumnIndex("gyY")));
            map.put("gyZ", cursor.getFloat(cursor.getColumnIndex("gyZ")));
            map.put("maX", cursor.getFloat(cursor.getColumnIndex("maX")));
            map.put("maY", cursor.getFloat(cursor.getColumnIndex("maY")));
            map.put("maZ", cursor.getFloat(cursor.getColumnIndex("maZ")));
            map.put("veX", cursor.getFloat(cursor.getColumnIndex("veX")));
            map.put("veY", cursor.getFloat(cursor.getColumnIndex("veY")));
            map.put("veZ", cursor.getFloat(cursor.getColumnIndex("veZ")));
            map.put("veCos", cursor.getFloat(cursor.getColumnIndex("veCos")));
            map.put("veAccuracy", cursor.getFloat(cursor.getColumnIndex("veAccuracy")));
            map.put("pressure", cursor.getFloat(cursor.getColumnIndex("pressure")));
            map.put("tmpDegree", cursor.getFloat(cursor.getColumnIndex("tmpDegree")));
            map.put("humidity", cursor.getFloat(cursor.getColumnIndex("humidity")));
            map.put("step", cursor.getInt(cursor.getColumnIndex("step")) == 1);
            map.put("motion", cursor.getInt(cursor.getColumnIndex("motion")) == 1);
            map.put("sigMotion", cursor.getInt(cursor.getColumnIndex("sigMotion")) == 1);
            cursor.close();
            return map;
        }else {
            Log.i("PEDOLOG_SC", "getCurrentWhereStep - 데이터 가져오기 실패");
            return null;
        }
    }

}
