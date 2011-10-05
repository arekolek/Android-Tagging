/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, and Michael Novak Jr.                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package com.applicake.android.ui;

import com.applicake.android.R;
import com.applicake.android.widget.OnTagChangeListener;
import com.applicake.android.widget.TagLayout;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Activity for tagging albums, artists and songs
 * 
 * @author Lukasz Wisniewski
 */
public class Tag extends Activity implements OnItemClickListener {
  String mArtist;
  String mTrack;

  ArrayList<String> mOldTags;
  ArrayList<String> mNewTags = new ArrayList<String>(
      Arrays.asList(new String[] { "elo" }));
  ArrayList<String> mSuggestedTags = new ArrayList<String>(Arrays.asList(new String[] {
      "raz", "dwa", "trzy", "elo", "cztery", "pi´ç" }));

  TagListAdapter mAdapter;

  Animation mFadeOutAnimation;
  boolean animate = false;

  // --------------------------------
  // XML LAYOUT start
  // --------------------------------
  EditText mTagEditText;
  Button mTagBackButton;
  Button mTagForwardButton;
  Button mTagButton;
  TagLayout mTagLayout;
  ListView mTagList;

  ProgressDialog mSaveDialog;

  // --------------------------------
  // XML LAYOUT start
  // --------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // loading activity layout
    setContentView(R.layout.tag);

    // binding views to XML-layout
    mTagEditText = (EditText) findViewById(R.id.tag_text_edit);
    mTagButton = (Button) findViewById(R.id.tag_add_button);
    mTagLayout = (TagLayout) findViewById(R.id.TagLayout);
    mTagList = (ListView) findViewById(R.id.TagList);

    // loading & setting animations
    mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.tag_row_fadeout);
    mTagLayout.setAnimationsEnabled(true);

    // add callback listeners
    mTagEditText.setOnKeyListener(new View.OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_ENTER:
          addTag(mTagEditText.getText().toString());
          return true;
        default:
          return false;
        }
      }
    });

    mTagEditText.setOnTouchListener(new View.OnTouchListener() {
      public boolean onTouch(View v, MotionEvent event) {
        mTagEditText.requestFocusFromTouch();
        return false;
      }
    });

    mTagButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        if (mTagEditText != null)
          addTag(mTagEditText.getText().toString());
      }

    });

    mTagLayout.setTagRemovingListener(new OnTagChangeListener() {
      @Override
      public void onTagAdded(String tag) {
      }

      public void onTagRemoved(String tag) {
        removeTag(tag);
      }
    });
    mTagLayout.setAreaHint(R.string.tagarea_hint);

    mTagList.setOnItemClickListener(this);

    mAdapter = new TagListAdapter(this);
    fillData();

  }

  /**
   * Fills mTopTagListAdapter, mUserTagListListAdapter and mTagLayout with data
   * (mTopTags, mUserTags & mTrackNewTags)
   */
  private void fillData() {
    mAdapter.setSource(mSuggestedTags, mNewTags);
    for (int i = 0; i < mNewTags.size(); i++) {
      mTagLayout.addTag(mNewTags.get(i));
    }
    mTagList.setAdapter(mAdapter);
  }

  /**
   * Adds new tag to mTagLayout and mTrackNewTags
   * 
   * @param tag
   * @return true if successful
   */
  private boolean addTag(String tag) {
    if (!isValidTag(tag))
      return false;

    if (mNewTags.contains(tag)) {
      // tag already exists, abort
      // TODO highlight the tag in tag layout
      Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
      return false;
    }
    mNewTags.add(tag);
    mTagLayout.addTag(tag);
    mTagEditText.setText("");

    // TODO convert to notifying all listviews
    mAdapter.onTagAdded(tag);
    return true;
  }

  /**
   * Validates a tag
   * 
   * @param tag
   * @return true if tag is valid
   */
  private boolean isValidTag(String tag) {
    if (tag == null || tag.trim().length() == 0) {
      return false;
    }
    return true;
  }

  /**
   * Removes given tag from mTagLayout, mNewTags and listviews
   * 
   * @param tag
   */
  private void removeTag(String tag) {
    mNewTags.remove(tag);

    // TODO notify mTagLayout

    // TODO convert to notifying all listviews
    mAdapter.onTagRemoved(tag);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (!animate) {
      String tag = (String) parent.getItemAtPosition(position);
      if (addTag(tag)) {
        mFadeOutAnimation.setAnimationListener(new AnimationListener() {

          public void onAnimationEnd(Animation animation) {
            animate = false;
          }

          public void onAnimationRepeat(Animation animation) {
          }

          public void onAnimationStart(Animation animation) {
            animate = true;
          }

        });
        view.findViewById(R.id.row_label).startAnimation(mFadeOutAnimation);
      }
    }
  }

  public class TagListAdapter extends BaseAdapter implements OnTagChangeListener {

    /**
     * Internal class representing single row entry
     * 
     * @author Lukasz Wisniewski
     */
    private class Entry {
      /**
       * Text to be displayed
       */
      String text;
      /**
       * Indicates whether the entry has already been added meaning whether it
       * should be grayed out
       */
      boolean added;

      public Entry(String text, boolean added) {
        this.text = text;
        this.added = added;
      }
    }

    private ArrayList<Entry> mList;
    private Activity mContext;

    public TagListAdapter(Activity context) {
      mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView;

      ViewHolder holder;

      // FIXME caching doesn't work right
      //      if (row == null) {
      LayoutInflater inflater = mContext.getLayoutInflater();
      row = inflater.inflate(R.layout.tag_row, null);

      holder = new ViewHolder();
      holder.label = (TextView) row.findViewById(R.id.row_label);

      row.setTag(holder);
      //      } else {
      //        holder = (ViewHolder) row.getTag();
      //      }

      // TODO remove hardcoded colors
      if (mList.get(position).added) {
        holder.label.setTextColor(0x337a7a7a);
      } else {
        holder.label.setTextColor(mContext.getResources().getColorStateList(
            R.drawable.list_entry_color));
      }

      holder.label.setText(mList.get(position).text);

      return row;
    }

    @Override
    public int getCount() {
      if (mList != null)
        return mList.size();
      else
        return 0;
    }

    @Override
    public Object getItem(int position) {
      return mList.get(position).text;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public boolean isEnabled(int position) {
      return !mList.get(position).added;
    }

    /**
     * Sets data source (ArrayList of tags) for this adapter
     * 
     * @param tags
     *          ArrayList of tags
     */
    public void setSource(ArrayList<String> tags) {
      setSource(tags, null);
    }

    /**
     * Sets data source (ArrayList of tags) for this adapter, additionally
     * allows to disable so called present tags which could have been previously
     * added by the user and we want them to be grayed out
     * 
     * @param tags
     *          ArrayList of tags
     * @param presentTags
     *          ArrayList of already added tags or null
     */
    public void setSource(ArrayList<String> tags, ArrayList<String> presentTags) {
      mList = new ArrayList<Entry>();
      for (int i = 0; i < tags.size(); i++) {
        Entry entry = new Entry(tags.get(i), false);
        if (presentTags != null) {
          entry.added = presentTags.contains(tags.get(i));
        }
        mList.add(entry);
      }
      notifyDataSetChanged();
    }

    @Override
    public void onTagAdded(String tag) {
      updateTag(tag, true);
    }

    @Override
    public void onTagRemoved(String tag) {
      updateTag(tag, false);
    }

    private int updateTag(String tag, boolean added) {
      for (int i = 0; i < mList.size(); i++) {
        if (mList.get(i).text.equals(tag)) {
          mList.get(i).added = added;
          notifyDataSetChanged();
        }
      }
      return -1;
    }

  }

  /**
   * Holder pattern implementation, performance boost
   * 
   * @author Lukasz Wisniewski
   */
  static class ViewHolder {
    TextView label;
  }

}
