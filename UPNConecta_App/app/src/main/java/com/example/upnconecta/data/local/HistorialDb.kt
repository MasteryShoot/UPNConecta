package com.example.upnconecta.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.upnconecta.data.MiEvaluacion

/**
 * Base SQLite local "upn_conecta.db".
 *
 * No guarda la sesión (eso queda en SharedPreferences): guarda el historial de
 * evaluaciones del usuario para mostrarlo en el Perfil aunque no haya internet,
 * y las sedes que el usuario consultó hace poco.
 */
class HistorialDb(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_DB, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $T_HISTORIAL (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                correo      TEXT    NOT NULL,
                tipo        TEXT    NOT NULL,      -- AREA o DOCENTE
                referencia  TEXT    NOT NULL,      -- sede|area o código del docente
                titulo      TEXT    NOT NULL,
                subtitulo   TEXT    NOT NULL,
                promedio    REAL    NOT NULL,
                fecha       INTEGER NOT NULL,
                UNIQUE (correo, tipo, referencia)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $T_VISITAS (
                correo      TEXT    NOT NULL,
                sede        TEXT    NOT NULL,
                fecha       INTEGER NOT NULL,
                PRIMARY KEY (correo, sede)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_HISTORIAL")
        db.execSQL("DROP TABLE IF EXISTS $T_VISITAS")
        onCreate(db)
    }

    // ------------------------------------------------------------ Historial

    /** Inserta o reemplaza la evaluación (si se edita, queda una sola fila). */
    fun guardarEvaluacion(correo: String, tipo: String, referencia: String,
                          titulo: String, subtitulo: String, promedio: Float) {
        val valores = ContentValues().apply {
            put("correo", correo)
            put("tipo", tipo)
            put("referencia", referencia)
            put("titulo", titulo)
            put("subtitulo", subtitulo)
            put("promedio", promedio)
            put("fecha", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(T_HISTORIAL, null, valores, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun eliminarEvaluacion(correo: String, tipo: String, referencia: String) {
        writableDatabase.delete(
            T_HISTORIAL, "correo = ? AND tipo = ? AND referencia = ?",
            arrayOf(correo, tipo, referencia)
        )
    }

    fun historial(correo: String): List<MiEvaluacion> {
        val lista = mutableListOf<MiEvaluacion>()
        readableDatabase.query(
            T_HISTORIAL, arrayOf("titulo", "subtitulo", "promedio", "fecha"),
            "correo = ?", arrayOf(correo), null, null, "fecha DESC"
        ).use { c ->
            while (c.moveToNext()) {
                lista += MiEvaluacion(c.getString(0), c.getString(1), c.getFloat(2), c.getLong(3))
            }
        }
        return lista
    }

    // --------------------------------------------------------------- Visitas

    fun registrarVisita(correo: String, sedeId: String) {
        val valores = ContentValues().apply {
            put("correo", correo)
            put("sede", sedeId)
            put("fecha", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(T_VISITAS, null, valores, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Últimas sedes consultadas, de la más reciente a la más antigua. */
    fun sedesRecientes(correo: String, limite: Int = 3): List<String> {
        val lista = mutableListOf<String>()
        readableDatabase.query(
            T_VISITAS, arrayOf("sede"), "correo = ?", arrayOf(correo),
            null, null, "fecha DESC", limite.toString()
        ).use { c -> while (c.moveToNext()) lista += c.getString(0) }
        return lista
    }

    companion object {
        private const val NOMBRE_DB = "upn_conecta.db"
        private const val VERSION = 1
        private const val T_HISTORIAL = "historial_evaluacion"
        private const val T_VISITAS = "sede_visitada"
        const val TIPO_AREA = "AREA"
        const val TIPO_DOCENTE = "DOCENTE"
    }
}
