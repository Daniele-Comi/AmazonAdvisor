package com.dev.amazonadvisor;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private ArrayList<AmazonProduct> dataset;
    private Activity activity;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public ViewHolder(LinearLayout layout) {
            super(layout);
            this.layout = layout;
        }
    }

    public ListAdapter(ArrayList<AmazonProduct> dataset, Activity activity) {
        this.dataset = dataset;
        this.activity =  activity;
    }

    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout layout = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.product_card, parent, false);
        ViewHolder vh = new ViewHolder(layout);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        ((ImageView)holder.layout.findViewById(R.id.product_image)).setImageDrawable(dataset.get(position).image);
        ((TextView)holder.layout.findViewById(R.id.product_title)).setText(dataset.get(position).title);
        ((TextView)holder.layout.findViewById(R.id.product_description)).setText(dataset.get(position).description);
        ((TextView)holder.layout.findViewById(R.id.product_price)).setText(dataset.get(position).price);
        ((CardView)holder.layout.findViewById(R.id.card_view)).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, ProductActivity.class);

                View sharedView = ((CardView)holder.layout.findViewById(R.id.card_view));
                String transitionName = activity.getString(R.string.product_transition);

                ActivityOptions transitionActivityOptions = ActivityOptions.makeSceneTransitionAnimation(activity,
                                                                                                         sharedView,
                                                                                                         transitionName);
                intent.putExtra("Title", dataset.get(position).title);
                intent.putExtra("Description", dataset.get(position).description);
                intent.putExtra("Price", dataset.get(position).price);
                intent.putExtra("ImageID", R.drawable.product_demo);
                activity.startActivity(intent, transitionActivityOptions.toBundle());
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public ArrayList<AmazonProduct> getDataset()
    {
        return dataset;
    }
}