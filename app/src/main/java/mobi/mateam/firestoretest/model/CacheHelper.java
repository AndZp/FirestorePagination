package mobi.mateam.firestoretest.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yanivsos on 22/02/18.
 * Zemingo LTD.
 */

public class CacheHelper<T extends CacheSupport> {
    private final Map<String, T> mCache;

    public CacheHelper() {
        mCache = new ConcurrentHashMap<>();
    }

    public void put(@NonNull final T item) {
        mCache.put(item.getKeyId(), item);
    }

    public void put(@NonNull final List<T> items) {
        for (T item : items) {
            mCache.put(item.getKeyId(), item);
        }
    }

    public @Nullable
    T get(@Nullable final String keyId) {
        if (TextUtils.isEmpty(keyId)) {
            return null;
        }

        return mCache.get(keyId);
    }

    public @Nullable
    T get(@Nullable final T item) {
        if (item == null || TextUtils.isEmpty(item.getKeyId())) {
            return null;
        }

        return mCache.get(item.getKeyId());
    }

    public void remove(@NonNull final String keyId) {
        mCache.remove(keyId);
    }

    public void remove(@NonNull final T t) {
        mCache.remove(t.getKeyId());
    }

    public void clear() {
        mCache.clear();
    }

    public boolean contains(@NonNull final T t) {
        return get(t) != null;
    }

    public List<T> toList() {
        final List<T> statics = new LinkedList<>();
        Set<Map.Entry<String, T>> entries = mCache.entrySet();

        for (Map.Entry<String, T> entry : entries) {
            statics.add(entry.getValue());
        }
        return statics;
    }
}
