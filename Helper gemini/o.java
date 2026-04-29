package com.topstep.fitcloud.sdk.v2.protocol.data.decode;

import androidx.annotation.Nullable;
import com.topstep.fitcloud.sdk.v2.model.data.FcSleepItem;
import com.topstep.fitcloud.sdk.v2.protocol.data.b;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import timber.log.Timber;

/* JADX INFO: loaded from: sdk-fitcloud-v3.0.2.aar:classes.jar:com/topstep/fitcloud/sdk/v2/protocol/data/decode/o.class */
public class o {
    public static final long a = 300000;

    @Nullable
    public static List<FcSleepItem> b(List<b.C0056b> list) {
        if (list == null || list.size() <= 0) {
            return null;
        }
        Collections.sort(list, (o1, o2) -> {
            return (int) (o1.a - o2.a);
        });
        while (list.size() > 0 && list.get(0).b == 3) {
            list.remove(0);
        }
        for (int size = list.size() - 1; size >= 0 && list.get(size).b == 3; size--) {
            list.remove(size);
        }
        if (list.size() <= 0) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        FcSleepItem fcSleepItem = null;
        for (int i = 0; i < list.size(); i++) {
            FcSleepItem fcSleepItem2 = fcSleepItem;
            b.C0056b c0056b = list.get(i);
            if (fcSleepItem2 != null) {
                if (fcSleepItem.getStatus() == c0056b.b) {
                    fcSleepItem.setEndTime(c0056b.a);
                } else {
                    arrayList.add(fcSleepItem);
                    fcSleepItem = null;
                }
            }
            if (fcSleepItem == null) {
                long endTime = c0056b.a - a;
                if (arrayList.size() > 0) {
                    FcSleepItem fcSleepItem3 = (FcSleepItem) arrayList.get(arrayList.size() - 1);
                    if (endTime < fcSleepItem3.getEndTime()) {
                        endTime = fcSleepItem3.getEndTime();
                    } else if (endTime - fcSleepItem3.getEndTime() <= 900000 || fcSleepItem3.getStatus() == 3) {
                        fcSleepItem3.setEndTime(endTime);
                    } else if (c0056b.b == 3) {
                        endTime = fcSleepItem3.getEndTime();
                    } else {
                        arrayList.add(new FcSleepItem(3, fcSleepItem3.getEndTime(), endTime));
                    }
                }
                fcSleepItem = fcSleepItem;
                FcSleepItem fcSleepItem4 = new FcSleepItem(c0056b.b, endTime, c0056b.a);
            }
            if (i == list.size() - 1) {
                arrayList.add(fcSleepItem);
            }
        }
        return arrayList;
    }

    @Nullable
    public static List<FcSleepItem> a(List<b.C0056b> list) {
        if (list == null || list.size() <= 1) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        FcSleepItem fcSleepItem = null;
        int i = 1;
        while (i < list.size()) {
            b.C0056b c0056b = list.get(i);
            b.C0056b c0056b2 = list.get(i - 1);
            long j = c0056b.a;
            long j2 = c0056b2.a;
            if (j < j2) {
                Timber.w("parserNewOneSleepData wrong sleep timestamp:%d and remove it", new Object[]{Long.valueOf(j)});
                list.remove(i);
                i--;
            } else if (j == j2) {
                Timber.w("parserNewOneSleepData wrong sleep timestamp:%d and skip it", new Object[]{Long.valueOf(j)});
            } else {
                if (fcSleepItem != null) {
                    if (fcSleepItem.getStatus() == c0056b.b) {
                        fcSleepItem.setEndTime(c0056b.a);
                    } else {
                        fcSleepItem = null;
                    }
                }
                if (fcSleepItem == null) {
                    fcSleepItem = fcSleepItem;
                    FcSleepItem fcSleepItem2 = new FcSleepItem(c0056b.b, c0056b2.a, c0056b.a);
                    arrayList.add(fcSleepItem);
                }
            }
            i++;
        }
        return arrayList;
    }
}