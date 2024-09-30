package com.bitwarden.authenticatorbridge.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import com.bitwarden.authenticatorbridge.manager.model.AuthenticatorBridgeConnectionType
import com.bitwarden.authenticatorbridge.manager.util.toPackageName
import com.bitwarden.authenticatorbridge.model.EncryptedSharedAccountData
import com.bitwarden.authenticatorbridge.provider.AuthenticatorBridgeCallbackProvider
import com.bitwarden.authenticatorbridge.provider.StubAuthenticatorBridgeCallbackProvider
import com.bitwarden.authenticatorbridge.provider.SymmetricKeyStorageProvider
import com.bitwarden.authenticatorbridge.util.decrypt
import com.bitwarden.authenticatorbridge.util.toFingerprint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val AUTHENTICATOR_BRIDGE_SERVICE_CLASS =
    "com.x8bit.bitwarden.data.platform.service.AuthenticatorBridgeService"

/**
 * Default implementation of [AuthenticatorBridgeManager].
 *
 * @param applicationContext The Context that will be used to bind to AuthenticatorBridgeService. Should be an
 * application context.
 * @param connectionType Specifies which build variant to connect to.
 * @param symmetricKeyStorageProvider Provides access to local storage of the symmetric encryption
 * key.
 * @param callbackProvider Provides a way to construct a service callback that can be mocked in
 * tests.
 * @param processLifecycleOwner Lifecycle owner that is used to listen for start/stop
 * lifecycle events.
 */
internal class AuthenticatorBridgeManagerImpl(
    private val applicationContext: Context,
    private val connectionType: AuthenticatorBridgeConnectionType,
    private val symmetricKeyStorageProvider: SymmetricKeyStorageProvider,
    callbackProvider: AuthenticatorBridgeCallbackProvider = StubAuthenticatorBridgeCallbackProvider(),
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : AuthenticatorBridgeManager {

    /**
     * Main AuthenticatorBridgeService access point.
     */
    private var bridgeService: IAuthenticatorBridgeService? = null

    /**
     * Internal state of [accountSyncStateFlow].
     */
    private val mutableSharedAccountsStateFlow =
        MutableStateFlow<AccountSyncState>(AccountSyncState.Loading)

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

    private fun bindService() {
        val intent = Intent().apply {
            component = ComponentName(
                connectionType.toPackageName(),
                AUTHENTICATOR_BRIDGE_SERVICE_CLASS,
            )
        }

        val isBound = try {
            applicationContext.bindService(
                intent,
                bridgeServiceConnection,
                Context.BIND_AUTO_CREATE,
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

    private fun onServiceConnected(service: IBinder) {
        bridgeService = IAuthenticatorBridgeService.Stub.asInterface(service)

        // TODO: Add check for version mismatch between client and server SDKs: BITAU-72

        // Ensure we are using the correct symmetric key:
        val localKeyFingerprint =
            symmetricKeyStorageProvider.symmetricKey?.toFingerprint()?.getOrNull()

        // Query bridge service to see if we have a matching symmetric key:
        val haveCorrectKey = bridgeService
            .safeCall { checkSymmetricEncryptionKeyFingerprint(localKeyFingerprint) }
            .fold(
                onSuccess = { it },
                onFailure = { false },
            )
            ?: false

        if (!haveCorrectKey) {
            // If we don't have the correct key, query for key:
            symmetricKeyStorageProvider.symmetricKey =
                bridgeService.safeCall { symmetricEncryptionKeyData }.getOrNull()
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
        bridgeService.safeCall { registerBridgeServiceCallback(authenticatorBridgeCallback) }

        // Sync data:
        bridgeService.safeCall { syncAccounts() }
    }

    private fun onServiceDisconnected() {
        bridgeService = null
    }

    private fun unbindService() {
        bridgeService.safeCall { unregisterBridgeServiceCallback(authenticatorBridgeCallback) }
        bridgeService = null
        @Suppress("TooGenericExceptionCaught")
        try {
            applicationContext.unbindService(bridgeServiceConnection)
        } catch (_: Exception) {
            // We want to be super safe when unbinding to assure no crashes.
        }
    }
}

/**
 * Helper function for wrapping all calls to [IAuthenticatorBridgeService] around try catch.
 *
 * This is important because all calls to [IAuthenticatorBridgeService] can throw
 * DeadObjectExceptions as well as RemoteExceptions.
 */
fun <T> IAuthenticatorBridgeService?.safeCall(action: IAuthenticatorBridgeService.() -> T): Result<T?> =
    runCatching {
        this?.let { action.invoke(it) }
    }
