package com.oohtracker.room;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * View Model to keep a reference to the word repository and
 * an up-to-date list of all words.
 */

public class FileDataViewModel {

    private final WordRepository mRepository;
    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.

    private final LiveData<List<Word>> mAllWords;

    public FileDataViewModel(Application application) {
        mRepository = new WordRepository(application);
        mAllWords = mRepository.getAllWords();
    }

    public LiveData<List<Word>> getAllWords() {
        return mAllWords;
    }

    public void insert(Word word) {
        mRepository.insert(word);
    }

    public void delete(String word) {
        mRepository.delete(word);
    }
    public void deleteAll() {
        mRepository.deleteAll();
    }

}
