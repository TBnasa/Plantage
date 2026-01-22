package com.example.orbitfocus;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import com.example.orbitfocus.model.Leaf;
import com.example.orbitfocus.model.LeafStatus;

/**
 * Plantage veritabanı yöneticisi.
 * Yaprak (Leaf) verilerini SQLite'da saklar.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Plantage.db";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_LEAVES = "leaves";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_IMAGES = "image_paths";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_CREATED_AT = "created_at";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_LEAVES + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_DATE + " TEXT UNIQUE,"
            + COLUMN_CONTENT + " TEXT,"
            + COLUMN_IMAGES + " TEXT,"
            + COLUMN_STATUS + " TEXT DEFAULT 'ACTIVE',"
            + COLUMN_CREATED_AT + " INTEGER"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tasks");
        db.execSQL("DROP TABLE IF EXISTS memories");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEAVES);
        onCreate(db);
    }

    /**
     * Yeni yaprak oluşturur.
     */
    public long createLeaf(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_CONTENT, "");
        values.put(COLUMN_IMAGES, "");
        values.put(COLUMN_STATUS, LeafStatus.ACTIVE.name());
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        long id = db.insertWithOnConflict(TABLE_LEAVES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return id;
    }

    /**
     * Yaprak içeriğini günceller (sadece yazı).
     */
    public void updateLeafContent(long id, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, content);
        db.update(TABLE_LEAVES, values, COLUMN_ID + " = ?", new String[] { String.valueOf(id) });
        db.close();
    }

    /**
     * Yaprak fotoğraflarını günceller.
     */
    public void updateLeafImages(long id, String imagePaths) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGES, imagePaths);
        db.update(TABLE_LEAVES, values, COLUMN_ID + " = ?", new String[] { String.valueOf(id) });
        db.close();
    }

    /**
     * Yaprak durumunu günceller.
     */
    public void updateLeafStatus(long id, LeafStatus status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, status.name());
        db.update(TABLE_LEAVES, values, COLUMN_ID + " = ?", new String[] { String.valueOf(id) });
        db.close();
    }

    /**
     * Tarihe göre yaprak getirir.
     */
    public Leaf getLeafByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LEAVES, null, COLUMN_DATE + " = ?",
                new String[] { date }, null, null, null);

        Leaf leaf = null;
        if (cursor.moveToFirst()) {
            leaf = cursorToLeaf(cursor);
        }
        cursor.close();
        db.close();
        return leaf;
    }

    /**
     * Tüm yaprakları tarihe göre sıralı getirir.
     */
    public List<Leaf> getAllLeaves() {
        List<Leaf> leaves = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_LEAVES + " ORDER BY " + COLUMN_DATE + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Leaf leaf = cursorToLeaf(cursor);
                // Durumu dinamik olarak hesapla
                LeafStatus currentStatus = leaf.calculateCurrentStatus();
                if (leaf.status != currentStatus) {
                    // Durum değişti, veritabanını güncelle
                    updateLeafStatus(leaf.id, currentStatus);
                    leaf.status = currentStatus;
                }
                leaves.add(leaf);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return leaves;
    }

    /**
     * Cursor'dan Leaf nesnesi oluşturur.
     */
    private Leaf cursorToLeaf(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
        String images = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGES));
        String statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));

        LeafStatus status;
        try {
            status = LeafStatus.valueOf(statusStr);
        } catch (Exception e) {
            status = LeafStatus.ACTIVE;
        }

        return new Leaf(id, date, content, images, status);
    }
}
