package com.x8bit.bitwarden.data.autofill.credential.model

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.provider.Action
import com.x8bit.bitwarden.MainActivity
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag
import kotlin.random.Random

fun getCredentialResponseAction(
    context: Context,
) = Action(
    title = context.getString(R.string.open_bitwarden),
    pendingIntent = PendingIntent.getActivity(
        context,
        Random.nextInt(),
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
    ),
)
