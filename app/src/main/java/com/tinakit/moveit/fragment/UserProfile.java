package com.tinakit.moveit.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tinakit.moveit.R;
import com.tinakit.moveit.activity.PickAvatar;
import com.tinakit.moveit.adapter.EditUserRecyclerAdapter;
import com.tinakit.moveit.db.FitnessDBHelper;
import com.tinakit.moveit.model.User;

import java.util.List;

/**
 * Created by Tina on 12/29/2015.
 */
public class UserProfile extends Fragment {

    // constants
    public static final String USER_PROFILE_TAG = "USER_PROFILE_TAG";

    // cache
    protected FragmentActivity mFragmentActivity;
    private View rootView;
    protected EditUserRecyclerAdapter mEditUserRecyclerAdapter;
    private List<User> mUserList;
    private User mUser;

    //database
    FitnessDBHelper mDatabaseHelper;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mFragmentActivity  = (FragmentActivity)super.getActivity();
        rootView = inflater.inflate(R.layout.recycler_view, container, false);

        mFragmentActivity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //get databaseHelper instance
        mDatabaseHelper = FitnessDBHelper.getInstance(mFragmentActivity);

        // fetch directly from the database
        mUserList = mDatabaseHelper.getUsers();


        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true); //child items have fixed dimensions, allows the RecyclerView to optimize better by figuring out the exact height and width of the entire list based on the adapter.

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mFragmentActivity);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mEditUserRecyclerAdapter = new EditUserRecyclerAdapter(getActivity(), mFragmentActivity, mUserList);
        mRecyclerView.setAdapter(mEditUserRecyclerAdapter);


        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PickAvatar.PICK_AVATAR_REQUEST_CODE){
            if (resultCode == mFragmentActivity.RESULT_OK) {
                mUser = data.getParcelableExtra(PickAvatar.PICK_AVATAR_KEY_USER);
                mUserList.set(mUserList.indexOf(mUser), mUser);

                // refresh recyclerview with new user list data
                mEditUserRecyclerAdapter.setList(mUserList);
                mEditUserRecyclerAdapter.notifyDataSetChanged();
            }
        }
    }
}