package com.topstep.fitcloud.sdk.v2.protocol.data.decode;

import com.topstep.fitcloud.sdk.v2.model.data.FcStepData;
import com.topstep.fitcloud.sdk.v2.protocol.data.b;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

/* JADX INFO: loaded from: sdk-fitcloud-v3.0.2.aar:classes.jar:com/topstep/fitcloud/sdk/v2/protocol/data/decode/q.class */
public final class q extends r<FcStepData> {
    public final boolean c;
    public final boolean d;
    public final int e;
    public int f;

    public q(boolean hasStepExtra, boolean hasSportDuration) {
        this.c = hasStepExtra;
        this.d = hasSportDuration;
        this.e = hasStepExtra ? hasSportDuration ? 8 : 6 : 2;
        this.f = 3000;
    }

    /* JADX WARN: Type inference incomplete: some casts might be missing */
    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public void a(long j, @NotNull byte[] bArr) {
        Intrinsics.checkNotNullParameter(bArr, "itemPacket");
        if (j > this.a) {
            return;
        }
        int i = ((bArr[0] & 255) << 8) | (bArr[1] & 255);
        if (i <= 0 || i > this.f) {
            Timber.Forest.w("step error:" + i + " > " + this.f, new Object[0]);
            return;
        }
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        if (this.c) {
            i2 = ((bArr[2] & 255) << 8) | (bArr[3] & 255);
            i3 = ((bArr[4] & 255) << 8) | (bArr[5] & 255);
            if (this.d) {
                i4 = ((bArr[6] & 255) << 8) | (bArr[7] & 255);
            }
        }
        this.b.add((T) new FcStepData(j, i, i2 / 100000.0f, i3 / 1000.0f, i4));
    }

    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public int a() {
        return this.e;
    }

    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public void a(@NotNull b.a header) {
        Intrinsics.checkNotNullParameter(header, "header");
        this.f = RangesKt.coerceAtLeast(((int) ((header.c / ((long) 1000)) / ((long) 60))) * 600, 3000);
    }
}