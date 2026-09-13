package com.example.fooddeliveryapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.fooddeliveryapp.fragments.AdminAuthFragment;
import com.example.fooddeliveryapp.fragments.DeliveryBoyAuthFragment;
import com.example.fooddeliveryapp.fragments.UserAuthFragment;

/**
 * AuthPagerAdapter — 3 tabs: User / Admin / Delivery Partner
 */
public class AuthPagerAdapter extends FragmentStateAdapter {

    private static final int TAB_USER         = 0;
    private static final int TAB_ADMIN        = 1;
    private static final int TAB_DELIVERY_BOY = 2;
    private static final int TAB_COUNT        = 3;

    public AuthPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_ADMIN:        return new AdminAuthFragment();
            case TAB_DELIVERY_BOY: return new DeliveryBoyAuthFragment();
            default:               return new UserAuthFragment();
        }
    }

    @Override
    public int getItemCount() { return TAB_COUNT; }
}