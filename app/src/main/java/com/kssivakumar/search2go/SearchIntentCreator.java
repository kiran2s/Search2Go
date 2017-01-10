package com.kssivakumar.search2go;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.URLUtil;

import com.google.android.youtube.player.YouTubeIntents;

import java.util.ArrayList;
import java.util.List;

public class SearchIntentCreator
{
    private enum SearchAppLabel {
        APP_NA, APP_SBROWSER, APP_CHROME, APP_YOUTUBE
    }

    public static Intent createSearchIntent(String query,
                                            String dialogText,
                                            Context context,
                                            PackageManager packageManager) {

        Intent searchIntent;
        Intent appChooserIntent;

        if (URLUtil.isValidUrl(query)) {
            searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
            return Intent.createChooser(searchIntent, dialogText);
        }

        searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        searchIntent.putExtra(SearchManager.QUERY, query);
        appChooserIntent = Intent.createChooser(searchIntent, dialogText);

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appInfoList = packageManager.queryIntentActivities(mainIntent, 0);
        List<LabeledIntent> intentList = new ArrayList<>();

        for (int i = 0; i < appInfoList.size(); i++) {
            ResolveInfo appInfo = appInfoList.get(i);
            String appName = appInfo.activityInfo.name;
            String packageName = appInfo.activityInfo.packageName;

            SearchAppLabel searchAppLabel = SearchAppLabel.APP_NA;
            if (packageName.contains("sbrowser"))
                searchAppLabel = SearchAppLabel.APP_SBROWSER;
            else if (packageName.contains("chrome"))
                searchAppLabel = SearchAppLabel.APP_CHROME;
            else if (packageName.contains("youtube"))
                searchAppLabel = SearchAppLabel.APP_YOUTUBE;

            if (searchAppLabel != SearchAppLabel.APP_NA) {
                Intent extraSearchIntent = new Intent();
                switch (searchAppLabel) {
                    case APP_SBROWSER:
                    case APP_CHROME:
                        extraSearchIntent.setAction(Intent.ACTION_VIEW);
                        extraSearchIntent.setData(Uri.parse("https://www.google.com/search?q=" + query));
                        extraSearchIntent.setComponent(new ComponentName(packageName, appName));
                        break;
                    case APP_YOUTUBE:
                        extraSearchIntent = YouTubeIntents.createSearchIntent(
                                context,
                                query
                        );
                        List<ResolveInfo> youtubeAppInfoList = packageManager.queryIntentActivities(
                                extraSearchIntent, 0
                        );
                        extraSearchIntent.setComponent(
                                new ComponentName(
                                        packageName,
                                        youtubeAppInfoList.get(0).activityInfo.name
                                )
                        );
                        break;
                }

                intentList.add(
                        new LabeledIntent(
                                extraSearchIntent,
                                packageName,
                                appInfo.loadLabel(packageManager),
                                appInfo.icon
                        )
                );
            }
        }

        LabeledIntent[] extraSearchIntents = intentList.toArray(
                new LabeledIntent[intentList.size()]
        );
        appChooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraSearchIntents);
        return appChooserIntent;
    }
}
