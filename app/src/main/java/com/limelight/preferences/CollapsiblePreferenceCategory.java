package com.limelight.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;
import com.limelight.R;

public class CollapsiblePreferenceCategory extends PreferenceCategory {
    private boolean isExpanded = false;

    public CollapsiblePreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_category_collapsible);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        
        TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        ImageView arrow = (ImageView) holder.findViewById(R.id.arrow);
        
        arrow.setRotation(isExpanded ? 0 : -90);
        
        holder.itemView.setOnClickListener(v -> {
            isExpanded = !isExpanded;
            updateExpandState();
            arrow.setRotation(isExpanded ? 0 : -90);
        });
        
        updateExpandState();
    }

    private void updateExpandState() {
        for (int i = 0; i < getPreferenceCount(); i++) {
            getPreference(i).setVisible(isExpanded);
        }
    }
} 