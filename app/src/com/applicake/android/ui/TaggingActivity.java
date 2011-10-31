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
import android.content.Intent;
import android.content.res.TypedArray;
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
import java.util.List;

/**
 * Activity for tagging albums, artists and songs
 * 
 * @author Lukasz Wisniewski
 */
public class TaggingActivity extends Activity implements OnItemClickListener {
  public static final String EXTRA_RESULT_TAGS = "tags_result";
  public static final String EXTRA_CURRENT_TAGS = "tags_current";
  public static final String EXTRA_SUGGESTED_TAGS = "tags_suggested";

  ArrayList<String> mTags;
  ArrayList<String> mSuggestedTags;

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
  private Animation mFadeInAnimation;
  private String mAction;
  private boolean mPickOnly;

  // --------------------------------
  // XML LAYOUT start
  // --------------------------------

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // loading activity layout
    setContentView(R.layout.tag);

    final Intent intent = getIntent();
    mAction = intent.getAction();

    mTags = intent.getStringArrayListExtra(EXTRA_CURRENT_TAGS);
    mSuggestedTags = intent.getStringArrayListExtra(EXTRA_SUGGESTED_TAGS);

    mPickOnly = Intent.ACTION_PICK.equals(mAction);

    // TODO some other action?
    //    if (Intent.ACTION_VIEW.equals(mAction)) {
    findViewById(R.id.save).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        save();
      }
    });
    findViewById(R.id.cancel).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        setResult(RESULT_CANCELED);
        finish();
      }
    });
    //    }

    // binding views to XML-layout

    if (mPickOnly) {
      findViewById(R.id.add_new_tag_bar).setVisibility(View.GONE);
    } else {
      mTagEditText = (EditText) findViewById(R.id.tag_text_edit);
      mTagButton = (Button) findViewById(R.id.tag_add_button);
    }
    mTagLayout = (TagLayout) findViewById(R.id.TagLayout);
    mTagList = (ListView) findViewById(R.id.TagList);

    // loading & setting animations
    AnimationListener listener = new AnimationListener() {
      public void onAnimationEnd(Animation animation) {
        mAdapter.notifyDataSetInvalidated();
        animate = false;
      }

      public void onAnimationRepeat(Animation animation) {
      }

      public void onAnimationStart(Animation animation) {
        animate = true;
      }
    };
    mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.tag_row_fadeout);
    mFadeOutAnimation.setAnimationListener(listener);
    mFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.tag_item_fadein);
    mFadeInAnimation.setAnimationListener(listener);
    mTagLayout.setAnimationsEnabled(true);

    // add callback listeners if not in pick only mode
    if (!mPickOnly) {
      mTagEditText.setOnKeyListener(new View.OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
          switch (event.getKeyCode()) {
          case KeyEvent.KEYCODE_ENTER:
            addTagFromInput();
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
          if (mTagEditText != null) {
            addTagFromInput();
          }
        }
      });
    }

    mTagLayout.setTagRemovingListener(new OnTagChangeListener() {
      @Override
      public void onTagAdded(String tag) {
      }

      public void onTagRemoved(String tag) {
        removeTag(tag);
        // TODO convert to notifying all listviews
        mAdapter.onTagRemoved(tag);
      }
    });

    mTagLayout.setAreaHint(mPickOnly ? R.string.picker_hint : R.string.tagarea_hint);

    mTagList.setOnItemClickListener(this);

    mAdapter = new TagListAdapter(this, mSuggestedTags);
    for (int i = 0; i < mTags.size(); i++) {
      mTagLayout.addTag(mTags.get(i));
    }
    mTagList.setAdapter(mAdapter);
  }

  private void save() {
    Intent data = new Intent();
    data.putExtra(EXTRA_RESULT_TAGS, mTags);
    setResult(RESULT_OK, data);
    finish();
  }

  private void addTagFromInput() {
    String tag = mTagEditText.getText().toString();
    addTag(tag);
    // TODO convert to notifying all listviews
    mAdapter.onTagAdded(tag);
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
   * Adds new tag to mTagLayout and mTrackNewTags
   * 
   * @param tag
   * @return true if successful
   */
  private boolean addTag(String tag) {
    if (!isValidTag(tag))
      return false;

    if (mTags.contains(tag)) {
      // tag already exists, abort
      // TODO highlight the tag in tag layout
      Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
      return false;
    }
    mTags.add(tag);
    mTagLayout.addTag(tag);
    if (!mPickOnly)
      mTagEditText.setText("");

    return true;
  }

  /**
   * Removes given tag from mTagLayout, mNewTags and listviews
   * 
   * @param tag
   */
  private void removeTag(String tag) {
    mTags.remove(tag);

    mTagLayout.onTagRemoved(tag);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (!animate) {
      String tag = (String) parent.getItemAtPosition(position);
      View label = view.findViewById(android.R.id.text1);
      if (mTags.contains(tag)) {
        removeTag(tag);
        label.startAnimation(mFadeInAnimation);
      } else {
        if (addTag(tag)) {
          label.startAnimation(mFadeOutAnimation);
        }
      }
    }
  }

  public class TagListAdapter extends BaseAdapter implements OnTagChangeListener {

    private List<String> mList;
    private Activity mContext;
    private int mItem;

    public TagListAdapter(Activity context, List<String> tags) {
      mContext = context;
      mList = tags;
      TypedArray a = getTheme().obtainStyledAttributes(R.styleable.TagTheme);
      mItem = a.getResourceId(R.styleable.TagTheme_tagListItem, R.layout.tag_row);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View row = convertView;

      ViewHolder holder;

      // FIXME caching doesn't work right
      //      if (row == null) {
      LayoutInflater inflater = mContext.getLayoutInflater();
      row = inflater.inflate(mItem, parent, false);

      holder = new ViewHolder();
      holder.label = (TextView) row.findViewById(android.R.id.text1);

      row.setTag(holder);
      //      } else {
      //        holder = (ViewHolder) row.getTag();
      //      }

      // TODO remove hardcoded colors
      if (mTags.contains(getItem(position))) {
        holder.label.setTextColor(0x337a7a7a);
      } else {
        holder.label.setTextColor(mContext.getResources().getColorStateList(
            R.drawable.list_entry_color));
      }

      holder.label.setText(mList.get(position));

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
      return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public void onTagAdded(String tag) {
      if (mList.contains(tag)) {
        notifyDataSetInvalidated();
      }
    }

    @Override
    public void onTagRemoved(String tag) {
      if (mList.contains(tag)) {
        notifyDataSetInvalidated();
      }
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
