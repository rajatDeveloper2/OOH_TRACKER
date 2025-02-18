package com.oohtracker.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * The Room Magic is in this file, where you map a Java method call to an SQL query.
 * <p>
 * When you are using complex data types, such as Date, you have to also supply type converters.
 * To keep this example basic, no types that require type converters are used.
 * See the documentation at
 * https://developer.android.com/topic/libraries/architecture/room.html#type-converters
 */

@Dao
public interface WordDao {

    // LiveData is a data holder class that can be observed within a given lifecycle.
    // Always holds/caches latest version of data. Notifies its active observers when the
    // data has changed. Since we are getting all the contents of the database,
    // we are notified whenever any of the database contents have changed.
    // If you want ordered by alphabets, use below line:
    //    @Query("SELECT * FROM word_database ORDER BY word ASC")
    @Query("SELECT * FROM word_database")
    LiveData<List<Word>> getAlphabetizedWords();

    @Query("SELECT * FROM word_database WHERE word = :qWord LIMIT 1")
    Word findByWord(String qWord);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Word word);

    @Query("DELETE FROM word_database WHERE word = :qWord")
    void delete(String qWord);

    @Query("DELETE FROM word_database")
    void deleteAll();
}
