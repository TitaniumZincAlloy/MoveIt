package com.tinakit.moveit.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.tinakit.moveit.R;
import com.tinakit.moveit.db.FitnessDBHelper;
import com.tinakit.moveit.model.Reward;
import com.tinakit.moveit.model.User;
import com.tinakit.moveit.module.CustomApplication;
import com.tinakit.moveit.utility.DateUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by Tina on 1/12/2016.
 */
public class AdminInBox extends Fragment {

    @Inject
    FitnessDBHelper mDatabaseHelper;

    //protected FragmentActivity mFragmentActivity;
    protected View mRootView;
    protected Button mFulfillButton;
    protected List<Reward> mRewardsFulfilledList;
    protected AdminInboxRecyclerAdapter mRecyclerAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        FragmentActivity fragmentActivity = (FragmentActivity)super.getActivity();
        mRootView = inflater.inflate(R.layout.recycler_view, container, false);

        // Dagger 2 injection
        ((CustomApplication)getActivity().getApplication()).getAppComponent().inject(this);

        RecyclerView recyclerView = (RecyclerView)mRootView.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(fragmentActivity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        List<Reward> rewardList = mDatabaseHelper.getUnFulfilledRewards();
        List<User> userList = mDatabaseHelper.getUsers();

        mRecyclerAdapter = new AdminInboxRecyclerAdapter(rewardList, userList);
        recyclerView.setAdapter(mRecyclerAdapter);

        mFulfillButton = (Button)mRootView.findViewById(R.id.fulfill);
        mFulfillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Reward reward = (Reward) v.getTag();
                reward.setDateFulfilled(new Date());
                int rowsEffected = mDatabaseHelper.updateRewardsEarned(mRewardsFulfilledList);

                if (rowsEffected > 0) {

                    Snackbar.make(mRootView.findViewById(R.id.recycler_view_main_layout), getString(R.string.message_update_reward_earned), Snackbar.LENGTH_LONG)
                            .show();

                    // refresh RecyclerView
                    mRecyclerAdapter.notifyDataSetChanged();

                } else {

                    Snackbar.make(mRootView.findViewById(R.id.recycler_view_main_layout), getString(R.string.error_message_update_reward_earned), Snackbar.LENGTH_LONG)
                            .show();
                }


            }
        });

        return mRootView;

    }

    public class AdminInboxRecyclerAdapter extends RecyclerView.Adapter<AdminInboxRecyclerAdapter.CustomViewHolder>{

        private List<Reward> mRewardList;
        private List<User> mUserList;

        public AdminInboxRecyclerAdapter(List<Reward> rewardList, List<User> userList){

            mRewardList = rewardList;
            mUserList = userList;
            mRewardsFulfilledList = new ArrayList<>();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder{

            protected TextView date;
            protected TextView userName;
            protected TextView rewardName;
            protected CheckBox fulfillCheckBox;

            public CustomViewHolder(View itemView) {

                super(itemView);

                date = (TextView)itemView.findViewById(R.id.date);
                userName = (TextView)itemView.findViewById(R.id.userName);
                rewardName = (TextView)itemView.findViewById(R.id.rewardName);
                fulfillCheckBox = (CheckBox)itemView.findViewById(R.id.fulfillCheckBox);

            }
        }

        @Override
        public int getItemCount() {
            return (null != mRewardList ? mRewardList.size() : 0);
        }

        @Override
        public AdminInboxRecyclerAdapter.CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.admin_inbox_list_item, parent, false);
            CustomViewHolder customViewHolder = new CustomViewHolder(view);

            return customViewHolder;
        }

        @Override
        public void onBindViewHolder(AdminInboxRecyclerAdapter.CustomViewHolder holder, int position) {

            Reward reward = mRewardList.get(position);

            holder.date.setText(DateUtility.getDateFormattedRecent(reward.getDateEarned(), 7));

            String userName = "";
            for (User user : mUserList){

                if(user.getUserId() == reward.getUserId())
                    userName = user.getUserName();
            }

            holder.userName.setText(userName);
            holder.rewardName.setText(reward.getName());

            holder.fulfillCheckBox.setTag(reward);
            holder.fulfillCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if(isChecked){

                        Reward reward = (Reward)buttonView.getTag();
                        mRewardsFulfilledList.add(reward);
                    }

                }
            });


        }


    }
}
