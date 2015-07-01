/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <jni.h>
//#include <JNIHelp.h>


#include <android/log.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <string.h>
#include <string>
#include <sqlite3.h>
#include "SQLiteCommon.h"
//#include <utils/String8.h>

#include "com_epam_database_NativeSQLiteConnection.h"

using namespace std;
using namespace android;

#define LOG_TAG    "Log From JNI"
#define LOGI(x...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,x)
#define ALOG(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define ALOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#define ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define ALOGW(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
//#define LOG_WINDOW(...)  ALOGW(__VA_ARGS__)
// Set to 1 to use UTF16 storage for localized indexes.
#define UTF16_STORAGE 0

/*---------------------------------  ---------------------------------*/
static const int BUSY_TIMEOUT_MS = 2500;

static struct {
    jfieldID name;
    jfieldID numArgs;
    jmethodID dispatchCallback;
} gSQLiteCustomFunctionClassInfo;

static struct {
    jclass clazz;
} gStringClassInfo;

struct SQLiteConnection {
    // Open flags.
    // Must be kept in sync with the constants defined in SQLiteDatabase.java.
    enum {
        OPEN_READWRITE          = 0x00000000,
        OPEN_READONLY           = 0x00000001,
        OPEN_READ_MASK          = 0x00000001,
        NO_LOCALIZED_COLLATORS  = 0x00000010,
        CREATE_IF_NECESSARY     = 0x10000000,
    };

    sqlite3* const db;
    const int openFlags;
    const string path;
    const string label;

    volatile bool canceled;

    SQLiteConnection(sqlite3* db, int openFlags, const string& path, const string& label) :
        db(db), openFlags(openFlags), path(path), label(label), canceled(false) { }
};

// Called each time a statement begins execution, when tracing is enabled.
void sqliteTraceCallback(void *data, const char *sql) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOG("%s: \"%s\"\n",
            connection->label.c_str(), sql);
}

// Called each time a statement finishes execution, when profiling is enabled.
void sqliteProfileCallback(void *data, const char *sql, sqlite3_uint64 tm) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    ALOG( "%s: \"%s\" took %0.3f ms\n",
            connection->label.c_str(), sql, tm * 0.000001f);
}

// Called after each SQLite VM instruction when cancelation is enabled.
int sqliteProgressHandlerCallback(void* data) {
    SQLiteConnection* connection = static_cast<SQLiteConnection*>(data);
    return connection->canceled;
}

JNIEXPORT jlong JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeOpen(JNIEnv* env, jclass clazz, jstring pathStr, jint openFlags,
        jstring labelStr, jboolean enableTrace, jboolean enableProfile) {
    int sqliteFlags;
    if (openFlags & SQLiteConnection::CREATE_IF_NECESSARY) {
        sqliteFlags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
    } else if (openFlags & SQLiteConnection::OPEN_READONLY) {
        sqliteFlags = SQLITE_OPEN_READONLY;
    } else {
        sqliteFlags = SQLITE_OPEN_READWRITE;
    }

    const char* pathChars = env->GetStringUTFChars(pathStr, NULL);
    string path(pathChars);
//    String8 path(pathChars);
    env->ReleaseStringUTFChars(pathStr, pathChars);

    const char* labelChars = env->GetStringUTFChars(labelStr, NULL);
    string label(labelChars);
    env->ReleaseStringUTFChars(labelStr, labelChars);

    sqlite3* db;
    int err = sqlite3_open_v2(path.c_str(), &db, sqliteFlags, NULL);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception_errcode(env, err, "Could not open database");
        return 0;
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(db, NULL)) {
        throw_sqlite3_exception(env, db, "Could not open the database in read/write mode.");
        sqlite3_close(db);
        return 0;
    }

    // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
    err = sqlite3_busy_timeout(db, BUSY_TIMEOUT_MS);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, db, "Could not set busy timeout");
        sqlite3_close(db);
        return 0;
    }

    // Register custom Android functions.
    /*err = register_android_functions(db, UTF16_STORAGE);
    if (err) {
        throw_sqlite3_exception(env, db, "Could not register Android SQL functions.");
        sqlite3_close(db);
        return 0;
    }*/

    // Create wrapper object.
    SQLiteConnection* connection = new SQLiteConnection(db, openFlags, path, label);

    // Enable tracing and profiling if requested.
    if (enableTrace) {
        sqlite3_trace(db, &sqliteTraceCallback, connection);
    }
    if (enableProfile) {
        sqlite3_profile(db, &sqliteProfileCallback, connection);
    }

    ALOGV("Opened connection %p with label '%s'", db, label.c_str());
    return reinterpret_cast<jlong>(connection);
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeClose(JNIEnv* env, jclass clazz, jlong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    if (connection) {
        ALOGV("Closing connection %p", connection->db);
        int err = sqlite3_close(connection->db);
        if (err != SQLITE_OK) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            ALOGE("sqlite3_close(%p) failed: %d", connection->db, err);
            throw_sqlite3_exception(env, connection->db, "Count not close db.");
            return;
        }

        delete connection;
    }
}

JNIEXPORT jlong JNICALL Java_com_epam_database_NativeSQLiteConnection_nativePrepareStatement(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jstring sqlString) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    jsize sqlLength = env->GetStringLength(sqlString);
    const jchar* sql = env->GetStringCritical(sqlString, NULL);
    sqlite3_stmt* statement;
    int err = sqlite3_prepare16_v2(connection->db,
            sql, sqlLength * sizeof(jchar), &statement, NULL);
    env->ReleaseStringCritical(sqlString, sql);

    if (err != SQLITE_OK) {
        // Error messages like 'near ")": syntax error' are not
        // always helpful enough, so construct an error string that
        // includes the query itself.
        const char *query = env->GetStringUTFChars(sqlString, NULL);
        char *message = (char*) malloc(strlen(query) + 50);
        if (message) {
            strcpy(message, ", while compiling: "); // less than 50 chars
            strcat(message, query);
        }
        env->ReleaseStringUTFChars(sqlString, query);
        throw_sqlite3_exception(env, connection->db, message);
        free(message);
        return 0;
    }

    ALOGV("Prepared statement %p on connection %p", statement, connection->db);
    return reinterpret_cast<jlong>(statement);
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeFinalizeStatement(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    // We ignore the result of sqlite3_finalize because it is really telling us about
    // whether any errors occurred while executing the statement.  The statement itself
    // is always finalized regardless.
    ALOGV("Finalized statement %p on connection %p", statement, connection->db);
    sqlite3_finalize(statement);
}

JNIEXPORT jint JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeGetParameterCount(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_bind_parameter_count(statement);
}

JNIEXPORT jboolean JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeIsReadOnly(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_stmt_readonly(statement) != 0;
}

JNIEXPORT jint JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeGetColumnCount(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    return sqlite3_column_count(statement);
}

JNIEXPORT jstring JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeGetColumnName(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index) {
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    const jchar* name = static_cast<const jchar*>(sqlite3_column_name16(statement, index));
    if (name) {
        size_t length = 0;
        while (name[length]) {
            length += 1;
        }
        return env->NewString(name, length);
    }
    return NULL;
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeBindNull(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_null(statement, index);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeBindLong(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jlong value) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_int64(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeBindDouble(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jdouble value) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_bind_double(statement, index, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeBindString(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jstring valueString) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    jsize valueLength = env->GetStringLength(valueString);
    const jchar* value = env->GetStringCritical(valueString, NULL);
    int err = sqlite3_bind_text16(statement, index, value, valueLength * sizeof(jchar),
            SQLITE_TRANSIENT);
    env->ReleaseStringCritical(valueString, value);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeBindBlob(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr, jint index, jbyteArray valueArray) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    jsize valueLength = env->GetArrayLength(valueArray);
    jbyte* value = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(valueArray, NULL));
    int err = sqlite3_bind_blob(statement, index, value, valueLength, SQLITE_TRANSIENT);
    env->ReleasePrimitiveArrayCritical(valueArray, value, JNI_ABORT);
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeResetStatementAndClearBindings(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = sqlite3_reset(statement);
    if (err == SQLITE_OK) {
        err = sqlite3_clear_bindings(statement);
    }
    if (err != SQLITE_OK) {
        throw_sqlite3_exception(env, connection->db, NULL);
    }
}

static int executeNonQuery(JNIEnv* env, SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err == SQLITE_ROW) {
        throw_sqlite3_exception(env,
                "Queries can be performed using SQLiteDatabase query or rawQuery methods only.");
    } else if (err != SQLITE_DONE) {
        throw_sqlite3_exception(env, connection->db);
    }
    return err;
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeExecute(JNIEnv* env, jclass clazz, jlong connectionPtr,
        jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    executeNonQuery(env, connection, statement);
}

JNIEXPORT jint JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeExecuteForChangedRowCount(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(env, connection, statement);
    return err == SQLITE_DONE ? sqlite3_changes(connection->db) : -1;
}

JNIEXPORT jlong JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeExecuteForLastInsertedRowId(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeNonQuery(env, connection, statement);
    return err == SQLITE_DONE && sqlite3_changes(connection->db) > 0
            ? sqlite3_last_insert_rowid(connection->db) : -1;
}

static int executeOneRowQuery(JNIEnv* env, SQLiteConnection* connection, sqlite3_stmt* statement) {
    int err = sqlite3_step(statement);
    if (err != SQLITE_ROW) {
        throw_sqlite3_exception(env, connection->db);
    }
    return err;
}

JNIEXPORT jlong JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeExecuteForLong(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(env, connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        return sqlite3_column_int64(statement, 0);
    }
    return -1;
}

JNIEXPORT jstring JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeExecuteForString(JNIEnv* env, jclass clazz,
        jlong connectionPtr, jlong statementPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    sqlite3_stmt* statement = reinterpret_cast<sqlite3_stmt*>(statementPtr);

    int err = executeOneRowQuery(env, connection, statement);
    if (err == SQLITE_ROW && sqlite3_column_count(statement) >= 1) {
        const jchar* text = static_cast<const jchar*>(sqlite3_column_text16(statement, 0));
        if (text) {
            size_t length = sqlite3_column_bytes16(statement, 0) / sizeof(jchar);
            return env->NewString(text, length);
        }
    }
    return NULL;
}

enum CopyRowResult {
    CPR_OK,
    CPR_FULL,
    CPR_ERROR,
};

JNIEXPORT jint JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeGetDbLookaside(JNIEnv* env, jobject clazz, jlong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);

    int cur = -1;
    int unused;
    sqlite3_db_status(connection->db, SQLITE_DBSTATUS_LOOKASIDE_USED, &cur, &unused, 0);
    return cur;
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeCancel(JNIEnv* env, jobject clazz, jlong connectionPtr) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = true;
}

JNIEXPORT void JNICALL Java_com_epam_database_NativeSQLiteConnection_nativeResetCancel(JNIEnv* env, jobject clazz, jlong connectionPtr,
        jboolean cancelable) {
    SQLiteConnection* connection = reinterpret_cast<SQLiteConnection*>(connectionPtr);
    connection->canceled = false;

    if (cancelable) {
        sqlite3_progress_handler(connection->db, 4, sqliteProgressHandlerCallback,
                connection);
    } else {
        sqlite3_progress_handler(connection->db, 0, NULL, NULL);
    }
}