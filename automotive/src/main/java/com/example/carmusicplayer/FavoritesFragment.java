package com.example.carmusicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Favorites Fragment with Favorites and History tabs
 */
public class FavoritesFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
        
        setupViewPager();
        
        return view;
    }

    private void setupViewPager() {
        viewPager.setAdapter(new FavoritesPagerAdapter(this));
        
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Favorites");
                tab.setIcon(R.drawable.ic_favorite);
            } else {
                tab.setText("History");
                tab.setIcon(R.drawable.ic_history);
            }
        }).attach();
    }

    private static class FavoritesPagerAdapter extends FragmentStateAdapter {

        public FavoritesPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ListFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return 2; // Favorites and History
        }
    }
}
