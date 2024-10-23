package com.bitwarden.authenticatorbridge.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.manager.util.toPackageName
import com.bitwarden.authenticatorbridge.model.AddTotpLoginItemData
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData
import com.bitwarden.authenticatorbridge.provider.AuthenticatorBridgeCallbackProvider
import com.bitwarden.authenticatorbridge.provider.StubAuthenticatorBridgeCallbackProvider
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider
import com.bitwarden.authenticatorbridge.util.decrypt
import com.bitwarden.authenticatorbridge.util.encrypt
import com.bitwarden.authenticatorbridge.util.isBuildVersionBelow
import com.bitwarden.authenticatorbridge.util.toFingerprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val AUTHENTICATOR_BRIDGE_SERVICE_CLASS =
    "com.x8bit.bitwarden.data.platform.service.AuthenticatorBridgeService"

/**
 * Default implementation of [AuthenticatorBridgeManager].
 *
 * @param context The Context that will be used to bind to AuthenticatorBridgeService.
 * @param connectionType Specifies which build variant to connect to.
 * @param symmetricKeyStorageProvider Provides access to local storage of the symmetric encryption
 * key.
 * @param callbackProvider Provides a way to construct a service callback that can be mocked in
 * tests.
 * @param processLifecycleOwner Lifecycle owner that is used to listen for start/stop
 * lifecycle events.
 */
internal class AuthenticatorBridgeManagerImpl(
    private val connectionType: AuthenticatorBridgeConnectionType,
    private val symmetricKeyStorageProvider: SymmetricKeyStorageProvider,
    callbackProvider: AuthenticatorBridgeCallbackProvider = StubAuthenticatorBridgeCallbackProvider(),
    context: Context,
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : AuthenticatorBridgeManager {

    private val applicationContext = context.applicationContext

    /**
     * Main AuthenticatorBridgeService access point.
     */
    private var bridgeService: IAuthenticatorBridgeService? = null

    /**
     * Internal state of [accountSyncStateFlow].
     */
    private val mutableSharedAccountsStateFlow: MutableStateFlow<AccountSyncState> =
        MutableStateFlow(
            when {
                isBuildVersionBelow(Build.VERSION_CODES.S) -> AccountSyncState.OsVersionNotSupported
                !isBitwardenAppInstalled() -> AccountSyncState.AppNotInstalled
                else -> AccountSyncState.Loading
            }
        )

    /**
     * Callback registered with AuthenticatorBridgeService.
     */
    private val authenticatorBridgeCallback = callbackProvider.getCallback(::onAccountsSync)

    /**
     * Service connection that listens for connected and disconnected service events.
     */
    private val bridgeServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            onServiceConnected(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            onServiceDisconnected()
        }
    }

    override val accountSyncStateFlow: StateFlow<AccountSyncState> =
        mutableSharedAccountsStateFlow.asStateFlow()

    init {
        // Listen for lifecycle events
        processLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    bindService()
                }

                override fun onStop(owner: LifecycleOwner) {
                    unbindService()
                }
            },
        )
    }

    override fun startAddTotpLoginItemFlow(totpUri: String): Boolean =
        bridgeService
            ?.safeCall {
                // Grab symmetric key data from local storage:
                val symmetricKey = symmetricKeyStorageProvider.symmetricKey ?: return@safeCall false
                // Encrypt the given URI:
                val addTotpData = AddTotpLoginItemData(totpUri).encrypt(symmetricKey).getOrThrow()
                return@safeCall this.startAddTotpLoginItemFlow(addTotpData)
            }
            ?.fold(
                onFailure = { false },
                onSuccess = { true }
            )
            ?: false

    private fun bindService() {
        if (isBuildVersionBelow(Build.VERSION_CODES.S)) {
            mutableSharedAccountsStateFlow.value = AccountSyncState.OsVersionNotSupported
            return
        }
        if (!isBitwardenAppInstalled()) {
            mutableSharedAccountsStateFlow.value = AccountSyncState.AppNotInstalled
            return
        }
        val intent = Intent().apply {
            component = ComponentName(
                connectionType.toPackageName(),
                AUTHENTICATOR_BRIDGE_SERVICE_CLASS,
            )
        }

        val flags = if (isBuildVersionBelow(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            Context.BIND_AUTO_CREATE
        } else {
            Context.BIND_AUTO_CREATE or Context.BIND_ALLOW_ACTIVITY_STARTS
        }

        val isBound = try {
            applicationContext.bindService(
                intent,
                bridgeServiceConnection,
                flags,
            )
        } catch (e: SecurityException) {
            unbindService()
            false
        }

        if (!isBound) {
            mutableSharedAccountsStateFlow.value = AccountSyncState.Error
        }
    }

    private fun onAccountsSync(data: EncryptedSharedAccountData) {
        // Received account sync update. Decrypt with local symmetric key and update StateFlow:
        mutableSharedAccountsStateFlow.value = symmetricKeyStorageProvider.symmetricKey
            ?.let { data.decrypt(it) }
            ?.getOrNull()
            ?.let { AccountSyncState.Success(it.accounts) }
            ?: AccountSyncState.Error
    }

    private fun onServiceConnected(binder: IBinder) {
        val service = IAuthenticatorBridgeService.Stub
            .asInterface(binder)
            .also { bridgeService = it }

        // TODO: Add check for version mismatch between client and server SDKs: BITAU-72

        // Ensure we are using the correct symmetric key:
        val localKeyFingerprint =
            symmetricKeyStorageProvider.symmetricKey?.toFingerprint()?.getOrNull()

        // Query bridge service to see if we have a matching symmetric key:
        val haveCorrectKey = service
            .safeCall { checkSymmetricEncryptionKeyFingerprint(localKeyFingerprint) }
            .fold(
                onSuccess = { it },
                onFailure = { false },
            )

        if (!haveCorrectKey) {
            // If we don't have the correct key, query for key:
            service
                .safeCall { symmetricEncryptionKeyData }
                .fold(
                    onSuccess = {
                        symmetricKeyStorageProvider.symmetricKey = it
                    },
                    onFailure = {
                        mutableSharedAccountsStateFlow.value = AccountSyncState.Error
                        unbindService()
                        return
                    },
                )

        }

        if (symmetricKeyStorageProvider.symmetricKey == null) {
            // This means bridgeService returned a null key, which means we can make
            // no valid operations. We should disconnect form the service and expose to the
            // calling application that authenticator sync is not enabled.
            mutableSharedAccountsStateFlow.value = AccountSyncState.SyncNotEnabled
            unbindService()
            return
        }

        // Register callback:
        service.safeCall { registerBridgeServiceCallback(authenticatorBridgeCallback) }

        // Sync data:
        service.safeCall { syncAccounts() }
    }

    private fun onServiceDisconnected() {
        bridgeService = null
    }

    private fun unbindService() {
        bridgeService?.safeCall { unregisterBridgeServiceCallback(authenticatorBridgeCallback) }
        bridgeService = null
        @Suppress("TooGenericExceptionCaught")
        try {
            applicationContext.unbindService(bridgeServiceConnection)
        } catch (_: Exception) {
            // We want to be super safe when unbinding to assure no crashes.
        }
    }

    private fun isBitwardenAppInstalled(): Boolean =
        // Check to see if correct Bitwarden app is installed:
        try {
            applicationContext.packageManager.getPackageInfo(connectionType.toPackageName(), 0)
            true
        } catch (e: NameNotFoundException) {
            false
        }
}

/**
 * Helper function for wrapping all calls to [IAuthenticatorBridgeService] around try catch.
 *
 * This is important because all calls to [IAuthenticatorBridgeService] can throw
 * DeadObjectExceptions as well as RemoteExceptions.
 */
private fun <T> IAuthenticatorBridgeService.safeCall(
    action: IAuthenticatorBridgeService.() -> T,
): Result<T> =
    runCatching {
        this.let { action.invoke(it) }
    }
