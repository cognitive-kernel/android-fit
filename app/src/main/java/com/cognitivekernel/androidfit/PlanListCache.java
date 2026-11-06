package com.cognitivekernel.androidfit;

import android.content.Context;
import android.content.pm.PackageInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Persistent, validated snapshot of the Plans & Exercises menu. SQLite remains authoritative. */
final class PlanListCache {
    private static final int CACHE_SCHEMA_VERSION = 1;

    private PlanListCache() {}

    private static int appVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static File cacheFile(Context context) {
        return new File(context.getFilesDir(), "plans_exercises_v" + CACHE_SCHEMA_VERSION
                + "_" + appVersionCode(context) + ".json");
    }

    /** Returns null only when the cache is absent or invalid; an empty plan list is valid. */
    static ArrayList<String[]> read(Context context) {
        File file = cacheFile(context.getApplicationContext());
        if (!file.isFile()) return null;
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            JSONObject root = new JSONObject(output.toString("UTF-8"));
            if (root.getInt("cacheSchemaVersion") != CACHE_SCHEMA_VERSION
                    || root.getInt("appVersionCode") != appVersionCode(context)) return null;
            JSONArray entries = root.getJSONArray("plans");
            if (root.getInt("count") != entries.length()) return null;
            ArrayList<String[]> plans = new ArrayList<>(entries.length());
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                String id = entry.getString("id");
                String name = entry.getString("name");
                if (!id.matches("[1-9][0-9]*") || name.trim().isEmpty()) return null;
                plans.add(new String[]{id, name});
            }
            return plans;
        } catch (Exception invalidCache) {
            return null;
        }
    }

    static void write(Context context, List<String[]> plans) {
        Context app = context.getApplicationContext();
        File target = cacheFile(app);
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        try {
            JSONArray entries = new JSONArray();
            for (String[] plan : plans) {
                if (plan == null || plan.length < 2 || plan[0] == null || plan[1] == null
                        || !plan[0].matches("[1-9][0-9]*") || plan[1].trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid plan cache entry");
                }
                JSONObject entry = new JSONObject();
                entry.put("id", plan[0]);
                entry.put("name", plan[1]);
                entries.put(entry);
            }
            JSONObject root = new JSONObject();
            root.put("cacheSchemaVersion", CACHE_SCHEMA_VERSION);
            root.put("appVersionCode", appVersionCode(app));
            root.put("count", entries.length());
            root.put("plans", entries);
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(root.toString().getBytes("UTF-8"));
                output.flush();
                output.getFD().sync();
            }
            if (target.exists() && !target.delete()) return;
            if (!temporary.renameTo(target)) temporary.delete();
        } catch (Exception ignored) {
            if (temporary.exists()) temporary.delete();
        }
    }

    static void invalidate(Context context) {
        File target = cacheFile(context.getApplicationContext());
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        if (temporary.exists()) temporary.delete();
        if (target.exists()) target.delete();
    }
}
