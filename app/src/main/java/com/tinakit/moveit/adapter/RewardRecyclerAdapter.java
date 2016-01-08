package com.tinakit.moveit.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tinakit.moveit.R;
import com.tinakit.moveit.activity.MainActivity;
import com.tinakit.moveit.db.FitnessDBHelper;
import com.tinakit.moveit.activity.RewardView;
import com.tinakit.moveit.model.Reward;
import com.tinakit.moveit.model.RewardStatusType;
import com.tinakit.moveit.model.User;
import com.tinakit.moveit.module.CustomApplication;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by Tina on 9/25/2015.
 */
public class RewardRecyclerAdapter extends RecyclerView.Adapter<RewardRecyclerAdapter.CustomViewHolder> {

    private FragmentActivity mFragmentActivity;
    private List<Reward> mRewardList;

    @Inject
    FitnessDBHelper mDatabaseHelper;

    private User mUser;

    public RewardRecyclerAdapter(User user, FragmentActivity fragmentActivity) {
        mUser = user;
        mFragmentActivity = fragmentActivity;

        // Dagger 2 injection
        ((CustomApplication)fragmentActivity.getApplication()).getAppComponent().inject(this);

        // Get singleton instance of database
        //FitnessDBHelper databaseHelper = FitnessDBHelper.getInstance(context);

        // Get Reward list
        mRewardList = mDatabaseHelper.getUserRewards(mUser.getUserId());
    }

    @Override
    public RewardRecyclerAdapter.CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.stat_list_item_child, viewGroup, false);

            CustomViewHolder viewHolder = new CustomViewHolder(view);
            return viewHolder;
    }

    @Override
    public void onBindViewHolder(RewardRecyclerAdapter.CustomViewHolder customViewHolder, int position) {

        Reward reward = mRewardList.get(position);

        // Populate data from Reward data object
        int numPoints = reward.getPoints();

        customViewHolder.rewardPoints.setText(String.valueOf(numPoints));
        customViewHolder.name.setText(reward.getName());
        //customViewHolder.description.setText(" " + reward.getDescription());
        customViewHolder.itemView.setTag(reward);

        customViewHolder.statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Button button = (Button)v;
                if (button.getText().equals("Get It")){
                    Reward reward = (Reward)v.getTag();
                    mUser.setPoints(mUser.getPoints() - reward.getPoints());

                    //update user's points
                    mDatabaseHelper.updateUser(mUser);

                    //update the reward status
                    mDatabaseHelper.setRewardStatus(mUser.getUserId(), reward.getRewardId(), RewardStatusType.PENDING);

                }
                else if (button.getText().equals("Cancel")){

                    Reward reward = (Reward)v.getTag();
                    mUser.setPoints(mUser.getPoints() + reward.getPoints());

                    //update user's points
                    mDatabaseHelper.updateUser(mUser);

                    //update the reward status
                    mDatabaseHelper.setRewardStatus(mUser.getUserId(), reward.getRewardId(), RewardStatusType.AVAILABLE);

                }

                RewardView rewardView = (RewardView) mFragmentActivity.getSupportFragmentManager().findFragmentByTag(RewardView.REWARD_VIEW_TAG);
                if (rewardView == null){

                    rewardView= new RewardView();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(RewardView.REWARD_VIEW_USER, mUser);
                    rewardView.setArguments(bundle);
                    mFragmentActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, rewardView).commit();
                }

            }
        });

        //if user has enough points, enable this button
        if (reward.getPoints() <= mUser.getPoints() && reward.getRewardStatusType() == RewardStatusType.AVAILABLE) {
            customViewHolder.statusButton.setText("Get It");
            customViewHolder.statusButton.setTag(reward);
            customViewHolder.statusButton.setVisibility(View.VISIBLE);


        } else if (reward.getRewardStatusType() == RewardStatusType.PENDING) {

            customViewHolder.statusButton.setText("Cancel");
            customViewHolder.statusButton.setTag(reward);
            customViewHolder.statusButton.setVisibility(View.VISIBLE);
            customViewHolder.status.setText("in progress");

        } else if (reward.getRewardStatusType() == RewardStatusType.DENIED) {

            customViewHolder.statusButton.setEnabled(false);
            customViewHolder.statusButton.setTag(reward);
            customViewHolder.status.setText("Mommy said no to this.");
        }

    }

    @Override
    public int getItemCount() {
            return (null != mRewardList ? mRewardList.size() : 0);
            }

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        TextView rewardPoints;
        TextView name;
        TextView description;
        Button statusButton;
        TextView status;

        public CustomViewHolder(View view) {
            super(view);

            this.rewardPoints = (TextView) view.findViewById(R.id.rewardPoints);
            this.name = (TextView) view.findViewById(R.id.name);
            //this.description = (TextView) view.findViewById(R.id.description);
            this.statusButton = (Button) view.findViewById(R.id.statusButton);
            this.status = (TextView) view.findViewById(R.id.status);


        }
    }

}


