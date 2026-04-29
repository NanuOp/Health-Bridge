package com.topstep.fitcloud.sdk.v2.protocol.data.decode;

import com.topstep.fitcloud.sdk.v2.model.production.FcSleepRawItem;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* JADX INFO: loaded from: sdk-fitcloud-v3.0.2.aar:classes.jar:com/topstep/fitcloud/sdk/v2/protocol/data/decode/p.class */
public final class p extends r<FcSleepRawItem> {
    public final int c = 2;

    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public int a() {
        return this.c;
    }

    /* JADX WARN: Type inference incomplete: some casts might be missing */
    @Override // com.topstep.fitcloud.sdk.v2.protocol.data.decode.r
    public void a(long j, @NotNull byte[] bArr) {
        Intrinsics.checkNotNullParameter(bArr, "itemPacket");
        this.b.add((T) new FcSleepRawItem(j, ((bArr[0] & 255) << 8) | (bArr[1] & 255)));
    }
}