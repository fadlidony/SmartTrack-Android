package com.idivisiontech.transporttracker.Settings

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object{
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "db.db"
        private val KEY = "key"
        private val VALUE = "value"
        private val TABLE_NAME = "settings"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        createSettingTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
    }

    private fun createSettingTable(db: SQLiteDatabase?){
        val query = "CREATE TABLE " + TABLE_NAME +
                " (" + KEY + " TEXT UNIQUE, " +
                VALUE + " TEXT)"
        db?.execSQL(query)
    }

    private fun set(key: String, value: String){
        val db = this.writableDatabase

        db.delete(TABLE_NAME, KEY + "=?", arrayOf(key))

        val values = ContentValues()
        values.put(KEY,key)
        values.put(VALUE,value)
        db.insert(TABLE_NAME,null,values)
        db.close()
    }

    private fun get(key: String) : String?{
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE key = '"+ key +"'",null)
        if(cursor.count == 0){
            return null
        }
        cursor.moveToFirst()
        return cursor.getString(cursor.getColumnIndex(VALUE))
    }



}