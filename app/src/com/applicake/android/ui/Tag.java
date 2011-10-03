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
import com.applicake.android.widget.TagLayout;
import com.applicake.android.widget.TagLayoutListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Activity for tagging albums, artists and songs
 * 
 * @author Lukasz Wisniewski
 */
public class Tag extends Activity {
  String mArtist;
  String mTrack;

  ArrayList<String> mOldTags;
  ArrayList<String> mNewTags = new ArrayList<String>(
      Arrays.asList(new String[] { "elo" }));
  ArrayList<String> mSuggestedTags = new ArrayList<String>(Arrays.asList(new String[] {
      "raz", "dwa", "trzy", "elo" }));

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

    mAdapter = new TagListAdapter(this);
    mAdapter.setSource(mSuggestedTags, mNewTags);
    mTagList.setAdapter(mAdapter);
    
    // loading & setting animations
    mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.tag_row_fadeout);
    mTagLayout.setAnimationsEnabled(true);

    // add callback listeners
    mTagEditText.setOnKeyListener(new View.OnKeyListener() {

      public boolean onKey(View v, int keyCode, KeyEvent event) {
        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_ENTER:
          mTagButton.performClick();
          mTagEditText.setText("");
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

    mTagLayout.setTagLayoutListener(new TagLayoutListener() {

      public void tagRemoved(String tag) {
        removeTag(tag);
      }

    });
    mTagLayout.setAreaHint(R.string.tagarea_hint);

    mTagList.setOnItemClickListener(new OnItemClickListener() {

      public void onItemClick(final AdapterView<?> parent, final View view,
          final int position, long time) {
        if (!animate) {
          String tag = (String) parent.getItemAtPosition(position);
          if (addTag(tag)) {
            mFadeOutAnimation.setAnimationListener(new AnimationListener() {

              public void onAnimationEnd(Animation animation) {
                ((TagListAdapter) parent.getAdapter()).tagAdded(position);
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

    });

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

    for (int i = 0; i < mNewTags.size(); i++) {
      if (mNewTags.get(i).equals(tag)) {
        // tag already exists, abort
        return false;
      }
    }
    mNewTags.add(tag);
    mTagLayout.addTag(tag);
    return true;
  }

  /**
   * Validates a tag
   * 
   * @param tag
   * @return true if tag is valid
   */
  private boolean isValidTag(String tag) {
    if (tag == null || tag.trim().length() == 0)
      return false;

    return true;
  }

  /**
   * Removes given tag from mTagLayout and mTrackNewTags
   * 
   * @param tag
   */
  private void removeTag(String tag) {
    for (int i = mNewTags.size() - 1; mNewTags.size() > 0 && i >= 0; i--) {
      if (mNewTags.get(i).equals(tag)) {
        mNewTags.remove(i);
      }
    }
    mAdapter.tagUnadded(tag);
  }

}
