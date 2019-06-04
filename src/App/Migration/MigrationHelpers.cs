using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System;
using System.Text;
using System.Threading.Tasks;

namespace Bit.App.Migration
{
    public static class MigrationHelpers
    {
        public static bool Migrating = false;

        public static bool NeedsMigration()
        {
            return ServiceContainer.Resolve<SettingsShim>("settingsShim")
                .GetValueOrDefault(Constants.OldUserIdKey, null) != null; ;
        }

        public static async Task<bool> PerformMigrationAsync()
        {
            if(!NeedsMigration() || Migrating)
            {
                return false;
            }

            Migrating = true;
            var settingsShim = ServiceContainer.Resolve<SettingsShim>("settingsShim");
            var oldSecureStorageService = ServiceContainer.Resolve<Abstractions.IOldSecureStorageService>(
                "oldSecureStorageService");

            var messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            var cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            var tokenService = ServiceContainer.Resolve<ITokenService>("tokenService");
            var userService = ServiceContainer.Resolve<IUserService>("userService");
            var environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            var passwordGenerationService = ServiceContainer.Resolve<IPasswordGenerationService>(
                "passwordGenerationService");
            var syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            var lockService = ServiceContainer.Resolve<ILockService>("lockService");

            // Get old data

            var oldTokenBytes = oldSecureStorageService.Retrieve("accessToken");
            var oldToken = oldTokenBytes == null ? null : Encoding.UTF8.GetString(
                oldTokenBytes, 0, oldTokenBytes.Length);
            var oldKeyBytes = oldSecureStorageService.Retrieve("key");
            var oldKey = oldKeyBytes == null ? null : new Models.SymmetricCryptoKey(oldKeyBytes);
            var oldUserId = settingsShim.GetValueOrDefault("userId", null);

            var isAuthenticated = oldKey != null && !string.IsNullOrWhiteSpace(oldToken) &&
                !string.IsNullOrWhiteSpace(oldUserId);
            if(!isAuthenticated)
            {
                Migrating = false;
                return false;
            }

            var oldRefreshTokenBytes = oldSecureStorageService.Retrieve("refreshToken");
            var oldRefreshToken = oldRefreshTokenBytes == null ? null : Encoding.UTF8.GetString(
                oldRefreshTokenBytes, 0, oldRefreshTokenBytes.Length);
            var oldPinBytes = oldSecureStorageService.Retrieve("pin");
            var oldPin = oldPinBytes == null ? null : Encoding.UTF8.GetString(
                oldPinBytes, 0, oldPinBytes.Length);

            var oldEncKey = settingsShim.GetValueOrDefault("encKey", null);
            var oldEncPrivateKey = settingsShim.GetValueOrDefault("encPrivateKey", null);
            var oldEmail = settingsShim.GetValueOrDefault("email", null);
            var oldKdf = (KdfType)settingsShim.GetValueOrDefault("kdf", (int)KdfType.PBKDF2_SHA256);
            var oldKdfIterations = settingsShim.GetValueOrDefault("kdfIterations", 5000);

            var oldTwoFactorTokenBytes = oldSecureStorageService.Retrieve(
                string.Format("twoFactorToken_{0}", Convert.ToBase64String(Encoding.UTF8.GetBytes(oldEmail))));
            var oldTwoFactorToken = oldTwoFactorTokenBytes == null ? null : Encoding.UTF8.GetString(
                oldTwoFactorTokenBytes, 0, oldTwoFactorTokenBytes.Length);

            var oldAppIdBytes = oldSecureStorageService.Retrieve("appId");
            var oldAppId = oldAppIdBytes == null ? null : new Guid(oldAppIdBytes).ToString();
            var oldAnonAppIdBytes = oldSecureStorageService.Retrieve("anonymousAppId");
            var oldAnonAppId = oldAnonAppIdBytes == null ? null : new Guid(oldAnonAppIdBytes).ToString();
            var oldFingerprint = settingsShim.GetValueOrDefault("setting:fingerprintUnlockOn", false);

            // Save settings

            await storageService.SaveAsync(Constants.AccessibilityAutofillPersistNotificationKey,
                settingsShim.GetValueOrDefault("setting:persistNotification", false));
            await storageService.SaveAsync(Constants.AccessibilityAutofillPasswordFieldKey,
                settingsShim.GetValueOrDefault("setting:autofillPasswordField", false));
            await storageService.SaveAsync(Constants.DisableAutoTotpCopyKey,
                settingsShim.GetValueOrDefault("setting:disableAutoCopyTotp", false));
            await storageService.SaveAsync(Constants.DisableFaviconKey,
                settingsShim.GetValueOrDefault("setting:disableWebsiteIcons", false));
            await storageService.SaveAsync(Constants.AddSitePromptShownKey,
                settingsShim.GetValueOrDefault("addedSiteAlert", false));
            await storageService.SaveAsync(Constants.PushInitialPromptShownKey,
                settingsShim.GetValueOrDefault("push:initialPromptShown", false));
            await storageService.SaveAsync(Constants.PushCurrentTokenKey,
                settingsShim.GetValueOrDefault("push:currentToken", null));
            await storageService.SaveAsync(Constants.PushRegisteredTokenKey,
                settingsShim.GetValueOrDefault("push:registeredToken", null));
            // For some reason "push:lastRegistrationDate" isn't getting pulled from settingsShim correctly.
            // We don't really need it anyways.
            // var lastReg = settingsShim.GetValueOrDefault("push:lastRegistrationDate", DateTime.MinValue);
            // await storageService.SaveAsync(Constants.PushLastRegistrationDateKey, lastReg);
            await storageService.SaveAsync("rememberedEmail",
                settingsShim.GetValueOrDefault("other:lastLoginEmail", null));

            await environmentService.SetUrlsAsync(new Core.Models.Data.EnvironmentUrlData
            {
                Base = settingsShim.GetValueOrDefault("other:baseUrl", null),
                Api = settingsShim.GetValueOrDefault("other:apiUrl", null),
                WebVault = settingsShim.GetValueOrDefault("other:webVaultUrl", null),
                Identity = settingsShim.GetValueOrDefault("other:identityUrl", null),
                Icons = settingsShim.GetValueOrDefault("other:iconsUrl", null)
            });

            await passwordGenerationService.SaveOptionsAsync(new Core.Models.Domain.PasswordGenerationOptions
            {
                Ambiguous = settingsShim.GetValueOrDefault("pwGenerator:ambiguous", false),
                Length = settingsShim.GetValueOrDefault("pwGenerator:length", 15),
                Uppercase = settingsShim.GetValueOrDefault("pwGenerator:uppercase", true),
                Lowercase = settingsShim.GetValueOrDefault("pwGenerator:lowercase", true),
                Number = settingsShim.GetValueOrDefault("pwGenerator:numbers", true),
                MinNumber = settingsShim.GetValueOrDefault("pwGenerator:minNumbers", 0),
                Special = settingsShim.GetValueOrDefault("pwGenerator:special", true),
                MinSpecial = settingsShim.GetValueOrDefault("pwGenerator:minSpecial", 0),
                WordSeparator = "-",
                NumWords = 3
            });

            // Save lock options

            int? lockOptionsSeconds = settingsShim.GetValueOrDefault("setting:lockSeconds", -10);
            if(lockOptionsSeconds == -10)
            {
                lockOptionsSeconds = 60 * 15;
            }
            else if(lockOptionsSeconds == -1)
            {
                lockOptionsSeconds = null;
            }
            await storageService.SaveAsync(Constants.LockOptionKey,
                lockOptionsSeconds == null ? (int?)null : lockOptionsSeconds.Value / 60);

            // Save app ids

            await storageService.SaveAsync("appId", oldAppId);
            await storageService.SaveAsync("anonymousAppId", oldAnonAppId);

            // Save new authed data

            await tokenService.SetTwoFactorTokenAsync(oldTwoFactorToken, oldEmail);
            await tokenService.SetTokensAsync(oldToken, oldRefreshToken);
            await userService.SetInformationAsync(oldUserId, oldEmail, oldKdf, oldKdfIterations);

            var newKey = new Core.Models.Domain.SymmetricCryptoKey(oldKey.Key);
            await cryptoService.SetKeyAsync(newKey);
            // Key hash is unavailable in old version, store old key until we can move it to key hash
            await secureStorageService.SaveAsync("oldKey", newKey.KeyB64);
            await cryptoService.SetEncKeyAsync(oldEncKey);
            await cryptoService.SetEncPrivateKeyAsync(oldEncPrivateKey);

            // Save fingerprint/pin

            if(oldFingerprint)
            {
                await storageService.SaveAsync(Constants.FingerprintUnlockKey, true);
            }
            else if(!string.IsNullOrWhiteSpace(oldPin))
            {
                var pinKey = await cryptoService.MakePinKeyAysnc(oldPin, oldEmail, oldKdf, oldKdfIterations);
                var pinProtectedKey = await cryptoService.EncryptAsync(oldKeyBytes, pinKey);
                await storageService.SaveAsync(Constants.PinProtectedKey, pinProtectedKey.EncryptedString);
            }

            // Post migration tasks
            await cryptoService.ToggleKeyAsync();
            await storageService.SaveAsync(Constants.LastActiveKey, DateTime.UtcNow.AddYears(-1));
            await lockService.CheckLockAsync();

            // Remove "needs migration" flag
            settingsShim.Remove(Constants.OldUserIdKey);
            await storageService.SaveAsync(Constants.MigratedFromV1, true);
            Migrating = false;
            messagingService.Send("migrated");
            if(Xamarin.Essentials.Connectivity.NetworkAccess != Xamarin.Essentials.NetworkAccess.None)
            {
                var task = Task.Run(() => syncService.FullSyncAsync(true));
            }
            return true;
        }
    }
}
