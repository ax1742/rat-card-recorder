package dm.app.card.fuck.df.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class CardDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "card_collection.db"
        private const val DB_VERSION = 1
        const val TABLE_CARD = "card_collection"
        const val COL_ID = "id"
        const val COL_RANK = "rank"
        const val COL_SUIT = "suit"
        const val COL_COLLECTED = "collected"
        const val COL_COLLECTED_AT = "collected_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CARD (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RANK TEXT NOT NULL,
                $COL_SUIT TEXT NOT NULL,
                $COL_COLLECTED INTEGER NOT NULL DEFAULT 0,
                $COL_COLLECTED_AT INTEGER DEFAULT NULL,
                UNIQUE($COL_RANK, $COL_SUIT)
            )
        """)
        initCards(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CARD")
        onCreate(db)
    }

    private fun initCards(db: SQLiteDatabase) {
        val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val suits = listOf("spade", "heart", "diamond", "club")
        val now = System.currentTimeMillis()

        val cv = ContentValues()
        for (rank in ranks) {
            for (suit in suits) {
                cv.clear()
                cv.put(COL_RANK, rank)
                cv.put(COL_SUIT, suit)
                cv.put(COL_COLLECTED, 0)
                cv.putNull(COL_COLLECTED_AT)
                db.insert(TABLE_CARD, null, cv)
            }
        }
        // Jokers
        for (jokerSuit in listOf("big_joker", "small_joker")) {
            cv.clear()
            cv.put(COL_RANK, "JOKER")
            cv.put(COL_SUIT, jokerSuit)
            cv.put(COL_COLLECTED, 0)
            cv.putNull(COL_COLLECTED_AT)
            db.insert(TABLE_CARD, null, cv)
        }
    }

    fun setCollected(rank: String, suit: String, collected: Boolean) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(COL_COLLECTED, if (collected) 1 else 0)
        if (collected) {
            cv.put(COL_COLLECTED_AT, System.currentTimeMillis())
        } else {
            cv.putNull(COL_COLLECTED_AT)
        }
        db.update(TABLE_CARD, cv, "$COL_RANK=? AND $COL_SUIT=?", arrayOf(rank, suit))
    }

    fun isCollected(rank: String, suit: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CARD, arrayOf(COL_COLLECTED),
            "$COL_RANK=? AND $COL_SUIT=?", arrayOf(rank, suit),
            null, null, null
        )
        val result = if (cursor.moveToFirst()) cursor.getInt(0) == 1 else false
        cursor.close()
        return result
    }

    fun getCollectedCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARD WHERE $COL_COLLECTED=1", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun isAllCollected(): Boolean = getCollectedCount() == 54

    fun getEarliestCollectionTime(): Long? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT MIN($COL_COLLECTED_AT) FROM $TABLE_CARD WHERE $COL_COLLECTED=1",
            null
        )
        val result = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        cursor.close()
        return result
    }

    fun getAllCollected(): Map<Pair<String, String>, Boolean> {
        val db = readableDatabase
        val cursor = db.query(TABLE_CARD, arrayOf(COL_RANK, COL_SUIT, COL_COLLECTED), null, null, null, null, null)
        val map = mutableMapOf<Pair<String, String>, Boolean>()
        while (cursor.moveToNext()) {
            val rank = cursor.getString(0)
            val suit = cursor.getString(1)
            val collected = cursor.getInt(2) == 1
            map[Pair(rank, suit)] = collected
        }
        cursor.close()
        return map
    }

    fun exportToJson(startTime: Long?): String {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CARD,
            arrayOf(COL_RANK, COL_SUIT, COL_COLLECTED, COL_COLLECTED_AT),
            null, null, null, null, null
        )

        val cardsArray = JSONArray()
        while (cursor.moveToNext()) {
            val card = JSONObject().apply {
                put("rank", cursor.getString(0))
                put("suit", cursor.getString(1))
                put("collected", cursor.getInt(2) == 1)
                if (!cursor.isNull(3)) put("collected_at", cursor.getLong(3))
            }
            cardsArray.put(card)
        }
        cursor.close()

        return JSONObject().apply {
            put("version", 1)
            put("start_time", startTime ?: 0)
            put("cards", cardsArray)
        }.toString(2)
    }

    fun importFromJson(json: String): Long? {
        val root = JSONObject(json)
        val cards = root.getJSONArray("cards")
        val startTime = if (root.has("start_time") && root.getLong("start_time") > 0)
            root.getLong("start_time") else null

        val db = writableDatabase
        db.beginTransaction()
        try {
            for (i in 0 until cards.length()) {
                val card = cards.getJSONObject(i)
                val cv = ContentValues().apply {
                    put(COL_COLLECTED, if (card.getBoolean("collected")) 1 else 0)
                    if (card.has("collected_at")) put(COL_COLLECTED_AT, card.getLong("collected_at"))
                    else putNull(COL_COLLECTED_AT)
                }
                db.update(
                    TABLE_CARD, cv,
                    "$COL_RANK=? AND $COL_SUIT=?",
                    arrayOf(card.getString("rank"), card.getString("suit"))
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return startTime
    }
}
