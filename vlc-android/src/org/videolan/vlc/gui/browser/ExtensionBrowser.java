package org.videolan.vlc.gui.browser;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.view.ContextMenuRecyclerView;
import org.videolan.vlc.gui.view.DividerItemDecoration;
import org.videolan.vlc.gui.view.SwipeRefreshLayout;
import org.videolan.vlc.media.MediaUtils;
import org.videolan.vlc.media.MediaWrapper;
import org.videolan.vlc.plugin.PluginService;
import org.videolan.vlc.plugin.Utils;
import org.videolan.vlc.plugin.api.VLCExtensionItem;

import java.util.ArrayList;

public class ExtensionBrowser extends Fragment implements View.OnClickListener, android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener {

    public static final String KEY_ITEMS_LIST = "key_items_list";
    public static final String KEY_SHOW_FAB = "key_fab";
    public static final String KEY_TITLE = "key_title";

    private String mTitle;
    FloatingActionButton mAddDirectoryFAB;
    ExtensionAdapter mAdapter;
    protected ContextMenuRecyclerView mRecyclerView;
    protected LinearLayoutManager mLayoutManager;
    protected TextView mEmptyView;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    PluginService mPluginService;
    private boolean showSettings = false;

    public void setPluginService(PluginService service) {
        mPluginService = service;
    }

    public ExtensionBrowser() {
        mAdapter = new ExtensionAdapter(this);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null)
            bundle = getArguments();
        if (bundle != null){
            mTitle = bundle.getString(KEY_TITLE);
            showSettings = bundle.getBoolean(KEY_SHOW_FAB);
            mAdapter.addAll(bundle.<VLCExtensionItem>getParcelableArrayList(KEY_ITEMS_LIST));
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.directory_browser, container, false);
        mRecyclerView = (ContextMenuRecyclerView) v.findViewById(R.id.network_list);
        mEmptyView = (TextView) v.findViewById(android.R.id.empty);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(VLCApplication.getAppContext(), DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(mScrollListener);
        registerForContextMenu(mRecyclerView);
        if (showSettings) {
            mAddDirectoryFAB = (FloatingActionButton) v.findViewById(R.id.fab_add_custom_dir);
            mAddDirectoryFAB.setVisibility(View.VISIBLE);
            mAddDirectoryFAB.setOnClickListener(this);
        }

        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeLayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange700);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        final AppCompatActivity activity = (AppCompatActivity)getActivity();
        if (activity != null && activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(mTitle);
            getActivity().supportInvalidateOptionsMenu();
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (mAdapter.getItemCount() > 0) {
            mEmptyView.setVisibility(View.GONE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.GONE);
        }
    }

    public void browseItem(VLCExtensionItem item) {
        mPluginService.browse(item.intId, item.stringId);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == mAddDirectoryFAB.getId()){
            //TODO start plugin activity
        }
    }

    @Override
    public void onRefresh() {
        mPluginService.refresh();
    }

    RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int topRowVerticalPosition =
                    (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
            mSwipeRefreshLayout.setEnabled(topRowVerticalPosition >= 0);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo == null)
            return;
        ContextMenuRecyclerView.RecyclerContextMenuInfo info = (ContextMenuRecyclerView
                .RecyclerContextMenuInfo) menuInfo;
        VLCExtensionItem item = mAdapter.getItem(info.position);
        if (item.type == VLCExtensionItem.TYPE_DIRECTORY)
            return;
        boolean isVideo = item.type == VLCExtensionItem.TYPE_VIDEO;
        getActivity().getMenuInflater().inflate(R.menu.extension_context_menu, menu);
        menu.findItem(R.id.extension_item_view_play_audio).setVisible(isVideo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuRecyclerView.RecyclerContextMenuInfo info = (ContextMenuRecyclerView
                .RecyclerContextMenuInfo) item.getMenuInfo();
        if (info != null && handleContextItemSelected(item, info.position))
            return true;
        return super.onContextItemSelected(item);
    }

    public void openContextMenu(final int position) {
        mRecyclerView.openContextMenu(position);
    }

    protected boolean handleContextItemSelected(MenuItem item, final int position) {
        switch (item.getItemId()) {
            case R.id.extension_item_view_play_all:
                ArrayList<VLCExtensionItem> items = mAdapter.getAll();
                ArrayList<MediaWrapper> medias = new ArrayList<>(items.size());
                for (VLCExtensionItem vlcItem : items) {
                    medias.add(Utils.mediawrapperFromExtension(vlcItem));
                }
                MediaUtils.openList(getActivity(), medias, position);
                return true;
            case R.id.extension_item_view_append:
                MediaUtils.appendMedia(getActivity(), Utils.mediawrapperFromExtension(mAdapter.getItem(position)));
                return true;
            case R.id.extension_item_view_play_audio:
                MediaWrapper mw = Utils.mediawrapperFromExtension(mAdapter.getItem(position));
                mw.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
                MediaUtils.openMedia(getActivity(), mw);
                return true;
            case R.id.extension_item_download:
                //TODO
            default:return false;

        }
    }
}
