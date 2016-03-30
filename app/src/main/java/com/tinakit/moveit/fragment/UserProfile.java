package com.tinakit.moveit.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tinakit.moveit.R;
import com.tinakit.moveit.adapter.UserProfileRecyclerAdapter;
import com.tinakit.moveit.db.DBController;
import com.tinakit.moveit.model.User;
import com.tinakit.moveit.module.CustomApplication;

import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by Tina on 12/29/2015.
 */
public class UserProfile extends Fragment {

    // CONSTANTS
    public static final String USER_PROFILE_TAG = "USER_PROFILE_TAG";

    //@Inject
    DBController mDBController;
    //FitnessDBHelper mDatabaseHelper;


    // UI
    protected FragmentActivity mFragmentActivity;
    private View rootView;
    protected FloatingActionButton mAddUserButton;

    //make these public to enable saving changes from Toolbar
    public UserProfileRecyclerAdapter mUserProfileRecyclerAdapter;
    private RecyclerView mRecyclerView;
    public List<User> mUserList;
    private User mUser;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mFragmentActivity = (FragmentActivity) super.getActivity();
        rootView = inflater.inflate(R.layout.user_profile, container, false);

        mFragmentActivity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Dagger 2 injection
        ((CustomApplication) getActivity().getApplication()).getAppComponent().inject(this);

        // fetch directly from the database
        //mUserList = mDatabaseHelper.getUsers();
        getUsers();

        initializeUI();
        setActionListeners();
        //TODO: not sure if we still need this if not calling onBackStack
        getActivity().getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    public void onBackStackChanged() {
                        // Update your UI here.
                        //mUserList = mDatabaseHelper.getUsers();
                        getUsers();
                        mUserProfileRecyclerAdapter = new UserProfileRecyclerAdapter(getActivity(), mFragmentActivity, mUserList);
                        mRecyclerView.setAdapter(mUserProfileRecyclerAdapter);
                    }
                });
        return rootView;
    }

    private void getUsers(){
        mDBController = new DBController();
        mDBController.getUsers(getActivity())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<User>>() {
                    @Override
                    public void call(List<User> users) {
                        mUserList = users;
                    }
                });
    }

    private void initializeUI(){

        mAddUserButton = (FloatingActionButton)rootView.findViewById(R.id.addUserButton);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true); //child items have fixed dimensions, allows the RecyclerView to optimize better by figuring out the exact height and width of the entire list based on the adapter.

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mFragmentActivity);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mUserProfileRecyclerAdapter = new UserProfileRecyclerAdapter(getActivity(), mFragmentActivity, mUserList);
        mRecyclerView.setAdapter(mUserProfileRecyclerAdapter);

    }

    private void setActionListeners(){

        mAddUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getActivity().getString(R.string.app_bar_header_admin) + " : " + getActivity().getString(R.string.app_bar_header_new_user));

                // check whether UserProfile is already visible
                EditUser editUser = (EditUser)mFragmentActivity.getSupportFragmentManager().findFragmentByTag(EditUser.EDIT_USER_TAG);
                if (editUser == null) {

                    editUser = new EditUser();
                    mFragmentActivity.getSupportFragmentManager()
                            .beginTransaction()
                            //add() instead of replace() so BackButton can track via fragment back stack
                            .add(R.id.fragmentContainer, editUser, EditUser.EDIT_USER_TAG)
                            .addToBackStack(getString(R.string.app_bar_header_edit_user))
                            .commit();
                }
            }
        });
    }

}
