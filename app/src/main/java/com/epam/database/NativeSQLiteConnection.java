package com.epam.database;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import static android.database.Cursor.FIELD_TYPE_BLOB;
import static android.database.Cursor.FIELD_TYPE_FLOAT;
import static android.database.Cursor.FIELD_TYPE_INTEGER;
import static android.database.Cursor.FIELD_TYPE_NULL;
import static android.database.Cursor.FIELD_TYPE_STRING;

/**
 * Test class
 * Created by Dzmitry_Slutski on 22.12.2014.
 */
public class NativeSQLiteConnection {

    static {
        System.loadLibrary("NativeSQLite");
    }

    public static final String TAG = NativeSQLiteConnection.class.getSimpleName();
    // The native NativeSQLiteConnection pointer.  (FOR INTERNAL USE ONLY)
    private long mConnectionPtr;


    public NativeSQLiteConnection() {
        Log.d(TAG, "my NativeSQLiteConnection");
    }

    public void open(String path, String label) {
        mConnectionPtr = nativeOpen(path, SQLiteDatabase.CREATE_IF_NECESSARY,
                label,
                Log.isLoggable("SQLiteStatements", Log.VERBOSE),
                Log.isLoggable("SQLiteTime", Log.VERBOSE));
    }

    public void close() {
        if (mConnectionPtr != 0) {
            try {
                nativeClose(mConnectionPtr);
                mConnectionPtr = 0;
            } finally {

            }
        }
    }

    public long executeForLastInsertedRowId(String sql, Object[] bindArgs) {
        if (sql == null) {
            throw new IllegalArgumentException("sql must not be null.");
        }

        try {
            final PreparedStatement statement = acquirePreparedStatement(sql);
            try {
                bindArguments(statement, bindArgs);
                try {
                    return nativeExecuteForLastInsertedRowId(
                            mConnectionPtr, statement.mStatementPtr);
                } finally {
//                    detachCancellationSignal(cancellationSignal);
                }
            } finally {
                releasePreparedStatement(statement);
            }
        } catch (RuntimeException ex) {

            throw ex;
        } finally {
//            mRecentOperations.endOperation(cookie);
        }
    }

    public void execute(String sql, Object[] bindArgs) {
        if (sql == null) {
            throw new IllegalArgumentException("sql must not be null.");
        }
        try {
            final PreparedStatement statement = acquirePreparedStatement(sql);
            try {
                bindArguments(statement, bindArgs);
                nativeExecute(mConnectionPtr, statement.mStatementPtr);
            } finally {
                releasePreparedStatement(statement);
            }
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    private void releasePreparedStatement(PreparedStatement statement) {
        statement.mInUse = false;
        if (statement.mInCache) {
            try {
                nativeResetStatementAndClearBindings(mConnectionPtr, statement.mStatementPtr);
            } catch (SQLiteException ex) {
                // The statement could not be reset due to an error.  Remove it from the cache.
                // When remove() is called, the cache will invoke its entryRemoved() callback,
                // which will in turn call finalizePreparedStatement() to finalize and
                // recycle the statement.


            }
        } else {
            finalizePreparedStatement(statement);
        }
    }

    private void recyclePreparedStatement(PreparedStatement statement) {
        statement.mSql = null;
        statement.mPoolNext = null;
    }

    private void finalizePreparedStatement(PreparedStatement statement) {
        nativeFinalizeStatement(mConnectionPtr, statement.mStatementPtr);
        recyclePreparedStatement(statement);
    }

    private PreparedStatement acquirePreparedStatement(String sql) {
        PreparedStatement statement = null;

        final long statementPtr = nativePrepareStatement(mConnectionPtr, sql);
        try {
            final int numParameters = nativeGetParameterCount(mConnectionPtr, statementPtr);
            final int type = DatabaseUtils.getSqlStatementType(sql);
            final boolean readOnly = nativeIsReadOnly(mConnectionPtr, statementPtr);
            statement = obtainPreparedStatement(sql, statementPtr, numParameters, type, readOnly);

        } catch (RuntimeException ex) {
            // Finalize the statement if an exception occurred and we did not add
            // it to the cache.  If it is already in the cache, then leave it there.
            if (statement == null || !statement.mInCache) {
                nativeFinalizeStatement(mConnectionPtr, statementPtr);
            }
            throw ex;
        }
        statement.mInUse = true;
        return statement;
    }

    private PreparedStatement obtainPreparedStatement(String sql, long statementPtr,
                                                      int numParameters, int type, boolean readOnly) {
        PreparedStatement statement = new PreparedStatement();

        statement.mSql = sql;
        statement.mStatementPtr = statementPtr;
        statement.mNumParameters = numParameters;
        statement.mType = type;
        statement.mReadOnly = readOnly;
        return statement;
    }

    public static int getTypeOfObject(Object obj) {
        if (obj == null) {
            return Cursor.FIELD_TYPE_NULL;
        } else if (obj instanceof byte[]) {
            return Cursor.FIELD_TYPE_BLOB;
        } else if (obj instanceof Float || obj instanceof Double) {
            return Cursor.FIELD_TYPE_FLOAT;
        } else if (obj instanceof Long || obj instanceof Integer
                || obj instanceof Short || obj instanceof Byte) {
            return Cursor.FIELD_TYPE_INTEGER;
        } else {
            return Cursor.FIELD_TYPE_STRING;
        }
    }

    private void bindArguments(PreparedStatement statement, Object[] bindArgs) {
        final int count = bindArgs != null ? bindArgs.length : 0;
        if (count != statement.mNumParameters) {
            throw new SQLiteBindOrColumnIndexOutOfRangeException(
                    "Expected " + statement.mNumParameters + " bind arguments but "
                            + count + " were provided.");
        }
        if (count == 0) {
            return;
        }

        final long statementPtr = statement.mStatementPtr;
        for (int i = 0; i < count; i++) {
            final Object arg = bindArgs[i];
            switch (getTypeOfObject(arg)) {
                case FIELD_TYPE_NULL:
                    nativeBindNull(mConnectionPtr, statementPtr, i + 1);
                    break;
                case FIELD_TYPE_INTEGER:
                    nativeBindLong(mConnectionPtr, statementPtr, i + 1,
                            ((Number) arg).longValue());
                    break;
                case FIELD_TYPE_FLOAT:
                    nativeBindDouble(mConnectionPtr, statementPtr, i + 1,
                            ((Number) arg).doubleValue());
                    break;
                case FIELD_TYPE_BLOB:
                    nativeBindBlob(mConnectionPtr, statementPtr, i + 1, (byte[]) arg);
                    break;
                case FIELD_TYPE_STRING:
                default:
                    if (arg instanceof Boolean) {
                        // Provide compatibility with legacy applications which may pass
                        // Boolean values in bind args.
                        nativeBindLong(mConnectionPtr, statementPtr, i + 1,
                                ((Boolean) arg).booleanValue() ? 1 : 0);
                    } else {
                        nativeBindString(mConnectionPtr, statementPtr, i + 1, arg.toString());
                    }
                    break;
            }
        }
    }

    private static final class PreparedStatement {
        // Next item in pool.
        public PreparedStatement mPoolNext;

        // The SQL from which the statement was prepared.
        public String mSql;

        // The native sqlite3_stmt object pointer.
        // Lifetime is managed explicitly by the connection.
        public long mStatementPtr;

        // The number of parameters that the prepared statement has.
        public int mNumParameters;

        // The statement type.
        public int mType;

        // True if the statement is read-only.
        public boolean mReadOnly;

        // True if the statement is in the cache.
        public boolean mInCache;

        // True if the statement is in use (currently executing).
        // We need this flag because due to the use of custom functions in triggers, it's
        // possible for SQLite calls to be re-entrant.  Consequently we need to prevent
        // in use statements from being finalized until they are no longer in use.
        public boolean mInUse;
    }

    /*------------------------- Native methods -------------------------*/

    private static native long nativeOpen(String path, int openFlags, String label,
                                          boolean enableTrace, boolean enableProfile);

    private static native void nativeClose(long connectionPtr);

    private static native void nativeRegisterLocalizedCollators(long connectionPtr, String locale);

    private static native long nativePrepareStatement(long connectionPtr, String sql);

    private static native void nativeFinalizeStatement(long connectionPtr, long statementPtr);

    private static native int nativeGetParameterCount(long connectionPtr, long statementPtr);

    private static native boolean nativeIsReadOnly(long connectionPtr, long statementPtr);

    private static native int nativeGetColumnCount(long connectionPtr, long statementPtr);

    private static native String nativeGetColumnName(long connectionPtr, long statementPtr,
                                                     int index);

    private static native void nativeBindNull(long connectionPtr, long statementPtr,
                                              int index);

    private static native void nativeBindLong(long connectionPtr, long statementPtr,
                                              int index, long value);

    private static native void nativeBindDouble(long connectionPtr, long statementPtr,
                                                int index, double value);

    private static native void nativeBindString(long connectionPtr, long statementPtr,
                                                int index, String value);

    private static native void nativeBindBlob(long connectionPtr, long statementPtr,
                                              int index, byte[] value);

    private static native void nativeResetStatementAndClearBindings(
            long connectionPtr, long statementPtr);

    private static native void nativeExecute(long connectionPtr, long statementPtr);

    private static native long nativeExecuteForLong(long connectionPtr, long statementPtr);

    private static native String nativeExecuteForString(long connectionPtr, long statementPtr);

    private static native int nativeExecuteForBlobFileDescriptor(
            long connectionPtr, long statementPtr);

    private static native int nativeExecuteForChangedRowCount(long connectionPtr, long statementPtr);

    private static native long nativeExecuteForLastInsertedRowId(
            long connectionPtr, long statementPtr);

    private static native long nativeExecuteForCursorWindow(
            long connectionPtr, long statementPtr, long windowPtr,
            int startPos, int requiredPos, boolean countAllRows);

    private static native int nativeGetDbLookaside(long connectionPtr);

    private static native void nativeCancel(long connectionPtr);

    private static native void nativeResetCancel(long connectionPtr, boolean cancelable);
}
