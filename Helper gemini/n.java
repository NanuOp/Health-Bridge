package com.topstep.fitcloud.sdk.v2.protocol.data.decode;

import com.topstep.fitcloud.sdk.v2.model.data.FcSleepData;
import com.topstep.fitcloud.sdk.v2.model.data.FcSleepItem;
import com.topstep.fitcloud.sdk.v2.protocol.data.b;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import timber.log.Timber;

/* JADX INFO: loaded from: sdk-fitcloud-v3.0.2.aar:classes.jar:com/topstep/fitcloud/sdk/v2/protocol/data/decode/n.class */
public final class n extends r<FcSleepData> {

    @NotNull
    public static final a m = new a();

    @NotNull
    public static final String n = "Fc#SleepDecoder";
    public final boolean c;
    public final boolean d;
    public final boolean e;
    public final boolean f;

    @NotNull
    public final GregorianCalendar g = new GregorianCalendar();

    @NotNull
    public final SimpleDateFormat h;

    @NotNull
    public final SimpleDateFormat i;

    @NotNull
    public final HashMap<String, ArrayList<b.C0056b>> j;

    @Nullable
    public ArrayList<b.C0056b> k;
    public final int l;

    /* JADX INFO: loaded from: sdk-fitcloud-v3.0.2.aar:classes.jar:com/topstep/fitcloud/sdk/v2/protocol/data/decode/n$a.class */
    public static final class a {
        public /* synthetic */ a(DefaultConstructorMarker $constructor_marker) {
            this();
        }

        public a() {
        }
    }

    public n(boolean newSleepProtocol, boolean isSupportSleepNap, boolean isSupportSleepRem, boolean ignoreTimeLimit) {
        this.c = newSleepProtocol;
        this.d = isSupportSleepNap;
        this.e = isSupportSleepRem;
        this.f = ignoreTimeLimit;
        Locale locale = Locale.US;
        this.h = new SimpleDateFormat("yyyy-MM-dd", locale);
        this.i = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
        this.j = new HashMap<>();
        this.l = newSleepProtocol ? 5 : 1;
    }

    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public int a() {
        return this.l;
    }

    /* JADX WARN: Type inference incomplete: some casts might be missing */
    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    @NotNull
    public List<FcSleepData> d() {
        for (String str : this.j.keySet()) {
            ArrayList<b.C0056b> arrayList = this.j.get(str);
            Intrinsics.checkNotNull(arrayList);
            ArrayList<b.C0056b> arrayList2 = arrayList;
            List<FcSleepItem> listA = this.c ? o.a(arrayList2) : o.b(arrayList2);
            if (listA != null && !listA.isEmpty()) {
                try {
                    ArrayList<T> arrayList3 = this.b;
                    Date date = this.h.parse(str);
                    Intrinsics.checkNotNull(date);
                    arrayList3.add((T) new FcSleepData(date.getTime(), listA, this.d));
                } catch (Exception e) {
                    Timber.Forest.tag(n).w(e);
                }
            }
        }
        return this.b;
    }

    public final String c(b.a header) {
        this.g.setTimeInMillis(header.b);
        if (this.d) {
            if (this.g.get(11) >= 20) {
                GregorianCalendar gregorianCalendar = this.g;
                gregorianCalendar.set(5, gregorianCalendar.get(5) + 1);
            }
        } else if (this.g.get(11) > 12) {
            GregorianCalendar gregorianCalendar2 = this.g;
            gregorianCalendar2.set(5, gregorianCalendar2.get(5) + 1);
        }
        String str = this.h.format(this.g.getTime());
        Intrinsics.checkNotNullExpressionValue(str, "formatDate.format(calendar.time)");
        return str;
    }

    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public void a(@NotNull b.a header) {
        Intrinsics.checkNotNullParameter(header, "header");
        String strC = c(header);
        ArrayList<b.C0056b> arrayList = this.j.get(strC);
        ArrayList<b.C0056b> arrayList2 = arrayList;
        if (arrayList == null) {
            arrayList2 = arrayList;
            ArrayList<b.C0056b> arrayList3 = new ArrayList<>(300);
            this.j.put(strC, arrayList2);
        }
        if (this.c) {
            b.C0056b c0056b = new b.C0056b(header.b, 3);
            arrayList2.add(c0056b);
            Timber.Forest.tag(n).i("%s = NONE", new Object[]{this.i.format(new Date(c0056b.a))});
        }
        this.k = arrayList2;
    }

    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public void a(long itemTimestamp, @NotNull byte[] itemPacket) {
        Intrinsics.checkNotNullParameter(itemPacket, "itemPacket");
        if (this.c) {
            long jB = com.topstep.fitcloud.sdk.v2.protocol.a.b(itemPacket, 0, this.g);
            if (this.f || jB <= this.a) {
                int i = itemPacket[4] & 255;
                if (this.e) {
                    if (i < 0 || i >= 5) {
                        return;
                    }
                } else if (i < 0 || i >= 4) {
                    return;
                }
                ArrayList<b.C0056b> arrayList = this.k;
                if (arrayList != null) {
                    arrayList.add(new b.C0056b(jB, i == 0 ? 3 : i));
                }
                Timber.Forest.tag(n).i("%s = %d", new Object[]{this.i.format(new Date(jB)), Integer.valueOf(i)});
                return;
            }
            return;
        }
        if (this.f || itemTimestamp <= this.a) {
            int i2 = itemPacket[0] & 255;
            if (this.e) {
                if (1 > i2 || i2 >= 5) {
                    return;
                }
            } else if (1 > i2 || i2 >= 4) {
                return;
            }
            ArrayList<b.C0056b> arrayList2 = this.k;
            if (arrayList2 != null) {
                arrayList2.add(new b.C0056b(itemTimestamp, i2));
            }
            Timber.Forest.tag(n).i("%s = %d", new Object[]{this.i.format(new Date(itemTimestamp)), Integer.valueOf(i2)});
        }
    }
}