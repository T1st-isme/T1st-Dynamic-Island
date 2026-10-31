package com.akira.island.island

import com.akira.island.core.CallInfo

/** Rich notification payload, but stable phone-state identity and authoritative call state. */
internal fun reconcileCall(phone: CallInfo?, notification: CallInfo?): CallInfo? = when {
    notification == null -> phone
    phone == null -> notification
    else -> notification.copy(
        key = phone.key,
        pkg = phone.pkg,
        incoming = phone.incoming,
        base = if (!phone.incoming && notification.incoming) phone.base else notification.base,
        avatar = notification.avatar ?: phone.avatar,
    )
}
