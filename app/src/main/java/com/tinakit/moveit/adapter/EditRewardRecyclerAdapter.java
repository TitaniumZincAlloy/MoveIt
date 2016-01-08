package com.tinakit.moveit.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.tinakit.moveit.R;
import com.tinakit.moveit.db.FitnessDBHelper;
import com.tinakit.moveit.model.Reward;
import com.tinakit.moveit.module.CustomApplication;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by Tina on 1/7/2016.
 */
public class EditRewardRecyclerAdapter extends RecyclerView.Adapter<EditRewardRecyclerAdapter.CustomViewHolder>  {

    @Inject
    FitnessDBHelper mDatabaseHelper;
    private Context mContext;
    private List<Reward> mRewardList;
    Reward mReward;


    public EditRewardRecyclerAdapter(Context context, Activity activity) {
        mContext = context;

        // inject FitnessDBHelper
        ((CustomApplication)activity.getApplication()).getStorageComponent().inject(this);

        // Get singleton instance of database
        //mDatabaseHelper = FitnessDBHelper.getInstance(context);

        // Get Activity Types
        mRewardList = mDatabaseHelper.getAllRewards();
    }

    @Override
    public EditRewardRecyclerAdapter.CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.edit_reward_list_item, viewGroup, false);

        CustomViewHolder viewHolder = new CustomViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final EditRewardRecyclerAdapter.CustomViewHolder customViewHolder, int i) {

        Reward reward = mRewardList.get(i);

        // Populate data from Reward data object
        int numPoints = reward.getPoints();

        customViewHolder.points.setText(String.valueOf(numPoints));
        customViewHolder.name.setText(reward.getName());
        customViewHolder.itemView.setTag(reward);

        customViewHolder.updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Reward reward = (Reward)customViewHolder.itemView.getTag();

                //TODO: call updateReward to DB
                mDatabaseHelper.updateReward(reward.getRewardId(), customViewHolder.name.getText().toString(), Integer.parseInt(customViewHolder.points.getText().toString()));

                //TODO: refresh recyclerview, is there another way to do this besides calling intent on EditReward
                ((Activity)mContext).finish();
                Intent intent = new Intent(mContext, com.tinakit.moveit.fragment.EditRewardFragment.class);
                mContext.startActivity(intent);
            }
        });

        customViewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mReward = (Reward)v.getTag();

                AlertDialog alertDialog = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this reward?")
                                //.setIcon(R.drawable.delete)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                //your deleting code

                                mDatabaseHelper.deleteReward(mReward.getRewardId());
                                dialog.dismiss();
                            }

                        })

                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                            }
                        })
                        .create();

                alertDialog.show();

            }
        });


    }

    @Override
    public int getItemCount() {
        return (null != mRewardList ? mRewardList.size() : 0);
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        EditText points;
        EditText name;
        Button updateButton;
        ImageView deleteButton;

        public CustomViewHolder(View view) {
            super(view);

            this.points = (EditText) view.findViewById(R.id.points);
            this.name = (EditText) view.findViewById(R.id.name);
            this.updateButton = (Button) view.findViewById(R.id.updateButton);
            this.deleteButton = (ImageView) view.findViewById(R.id.delete);
        }
    }

}