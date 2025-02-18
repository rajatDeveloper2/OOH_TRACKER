package com.oohtracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.oohtracker.R;
import com.oohtracker.room.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * FavoritesAdapter for showing all saved words
 */
public class FileDataAdapter extends RecyclerView.Adapter<FileDataAdapter.ViewHolder> {

    private List<Word> words;


    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;// definitionsTextView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            textView = (TextView) view.findViewById(R.id.favoritesTextView);
            //definitionsTextView = (TextView) view.findViewById(R.id.favoritesDefinitionTextView);
        }

       /* public TextView getDefinitionsTextView() {
            return definitionsTextView;
        }*/

        public TextView getTextView() {
            return textView;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param words String[] containing the data to populate views to be used
     *              by RecyclerView.
     */
    public FileDataAdapter(List<Word> words) {
        this.words = words;
        /*if (words.size() == 0) {
            this.words = new ArrayList<>();
            this.words.add(new Word("Hurrah! No files pending to upload.", "")) ;

        }*/
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.text_row_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element


            if (android.os.Build.VERSION.SDK_INT >= 29) {
                String currentFileNameToShow = words.get(position).getWord().split("-")[1];
                String[] currentFileNameToShowShort = currentFileNameToShow.split("_");
                viewHolder.getTextView().setText(position + " | " + currentFileNameToShowShort[0]+"_"+currentFileNameToShowShort[1]+"..."+currentFileNameToShowShort[currentFileNameToShowShort.length-1]);

            } else {
                String[] currentFileNameToShow = words.get(position).getWord().split("/");
                String[] currentFileNameToShowShort = currentFileNameToShow[currentFileNameToShow.length - 1].split("_");
                viewHolder.getTextView().setText(position + " | " + currentFileNameToShowShort[0]+"_"+currentFileNameToShowShort[1]+"..."+currentFileNameToShowShort[currentFileNameToShowShort.length-1]);
            }

        /* String s = (words.get(position).wordMeaning);
        viewHolder.getDefinitionsTextView().setText(s);*/

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return words.size();
    }
}
