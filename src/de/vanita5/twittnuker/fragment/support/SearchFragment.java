package de.vanita5.twittnuker.fragment.support;

import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import de.vanita5.twittnuker.R;
import de.vanita5.twittnuker.activity.support.LinkHandlerActivity;
import de.vanita5.twittnuker.adapter.support.SupportTabsAdapter;
import de.vanita5.twittnuker.fragment.iface.RefreshScrollTopInterface;
import de.vanita5.twittnuker.fragment.iface.SupportFragmentCallback;
import de.vanita5.twittnuker.graphic.DropShadowDrawable;
import de.vanita5.twittnuker.model.Panes;
import de.vanita5.twittnuker.provider.RecentSearchProvider;
import de.vanita5.twittnuker.util.AsyncTwitterWrapper;
import de.vanita5.twittnuker.util.ThemeUtils;
import de.vanita5.twittnuker.view.ExtendedViewPager;
import de.vanita5.twittnuker.view.SquareImageView;

public class SearchFragment extends BaseSupportFragment implements Panes.Left, OnPageChangeListener,
		RefreshScrollTopInterface, SupportFragmentCallback {

	private ExtendedViewPager mViewPager;
	private LinearLayout mIndicator;

	private SupportTabsAdapter mAdapter;

	private int mThemeColor;
	private Fragment mCurrentVisibleFragment;

	@Override
	public Fragment getCurrentVisibleFragment() {
		return mCurrentVisibleFragment;
	}

	public void hideIndicator() {
		if (mIndicator.getVisibility() == View.GONE) return;
		mIndicator.setVisibility(View.GONE);
		mIndicator.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		final Bundle args = getArguments();
		mThemeColor = ThemeUtils.getUserThemeColor(getActivity());
		mAdapter = new SupportTabsAdapter(getActivity(), getChildFragmentManager(), null);
		mAdapter.addTab(SearchStatusesFragment.class, args, getString(R.string.statuses), R.drawable.ic_tab_twitter, 0);
		mAdapter.addTab(SearchUsersFragment.class, args, getString(R.string.users), R.drawable.ic_tab_person, 1);
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(this);
		mViewPager.setOffscreenPageLimit(2);
		final int current = mViewPager.getCurrentItem();
		for (int i = 0, count = mAdapter.getCount(); i < count; i++) {
			final ImageView v = new SquareImageView(getActivity());
			v.setScaleType(ScaleType.CENTER_INSIDE);
			final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
			lp.weight = 0;
			mIndicator.addView(v, lp);
			final Drawable icon = mAdapter.getPageIcon(i);
			v.setImageDrawable(new DropShadowDrawable(getResources(), icon, 3, Color.BLACK, Color.WHITE));
			if (i == current) {
				v.setColorFilter(mThemeColor, Mode.MULTIPLY);
			} else {
				v.clearColorFilter();
			}
		}
		if (savedInstanceState == null && args != null && args.containsKey(EXTRA_QUERY)) {
			final String query = args.getString(EXTRA_QUERY);
			final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
					RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);
			suggestions.saveRecentQuery(query, null);
			final FragmentActivity activity = getActivity();
			if (activity instanceof LinkHandlerActivity) {
				final ActionBar ab = activity.getActionBar();
				if (ab != null) {
					ab.setSubtitle(query);
				}
			}
		}
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search, container, false);
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_search, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SAVE: {
			final AsyncTwitterWrapper twitter = getTwitterWrapper();
			final Bundle args = getArguments();
			if (twitter != null && args != null) {
				final long accountId = args.getLong(EXTRA_ACCOUNT_ID, -1);
				final String query = args.getString(EXTRA_QUERY);
				twitter.createSavedSearchAsync(accountId, query);
			}
			return true;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDetachFragment(final Fragment fragment) {

	}

	@Override
	public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
	}

	@Override
	public void onPageScrollStateChanged(final int state) {
		if (state == ViewPager.SCROLL_STATE_DRAGGING) {
			showIndicator();
		}
	}

	@Override
	public void onPageSelected(final int position) {
		final int count = mAdapter.getCount();
		if (count != mIndicator.getChildCount()) return;
		for (int i = 0; i < count; i++) {
			final ImageView v = (ImageView) mIndicator.getChildAt(i);
			if (i == position) {
				v.setColorFilter(mThemeColor, Mode.SRC_ATOP);
			} else {
				v.clearColorFilter();
			}
		}
	}

	@Override
	public void onSetUserVisibleHint(final Fragment fragment, final boolean isVisibleToUser) {
		if (isVisibleToUser) {
			mCurrentVisibleFragment = fragment;
		}
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mViewPager = (ExtendedViewPager) view.findViewById(R.id.search_pager);
		mIndicator = (LinearLayout) view.findViewById(R.id.search_pager_indicator);
	}

	@Override
	public boolean scrollToStart() {
		if (!(mCurrentVisibleFragment instanceof RefreshScrollTopInterface)) return false;
		((RefreshScrollTopInterface) mCurrentVisibleFragment).scrollToStart();
		return true;
	}

	public void showIndicator() {
		if (mIndicator.getVisibility() == View.VISIBLE) return;
		mIndicator.setVisibility(View.VISIBLE);
		mIndicator.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
	}

	@Override
	public boolean triggerRefresh() {
		if (!(mCurrentVisibleFragment instanceof RefreshScrollTopInterface)) return false;
		((RefreshScrollTopInterface) mCurrentVisibleFragment).triggerRefresh();
		return true;
	}

	@Override
	public boolean triggerRefresh(final int position) {
		return false;
	}

}