package com.example.ecommerce;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SponsoredProductAdapter extends RecyclerView.Adapter<SponsoredProductAdapter.ViewHolder> {

    private final List<SponsoredProduct> products;
    private final Context context;

    public SponsoredProductAdapter(Context context, List<SponsoredProduct> products) {
        this.context = context;
        this.products = products;
    }

    @NonNull
    @Override
    public SponsoredProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_sponsored_product, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull SponsoredProductAdapter.ViewHolder holder, int position) {
        SponsoredProduct product = products.get(position);
        holder.productName.setText(product.getName());
        holder.productImage.setImageResource(product.getImageResId());
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
        }
    }
}
