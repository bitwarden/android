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
                .GetValueOrDefault(Constants.OldUserIdKey, null) != null;
        }

        public static async Task<bool> PerformMigrationAsync()
        {
            if(!NeedsMigration())
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

            // Save settings

            await storageService.SaveAsync(Constants.AccessibilityAutofillPersistNotificationKey,
                settingsShim.GetValueOrDefault("setting:persistNotification", false));
            await storageService.SaveAsync(Constants.AccessibilityAutofillPasswordFieldKey,
                settingsShim.GetValueOrDefault("setting:autofillPasswordField", false));
            await storageService.SaveAsync(Constants.DisableAutoTotpCopyKey,
                settingsShim.GetValueOrDefault("setting:disableAutoCopyTotp", false));
            await storageService.SaveAsync(Constants.DisableFaviconKey,
                settingsShim.GetValueOrDefault("setting:disableWebsiteIcons", false));
            await storageService.SaveAsync(Constants.PushInitialPromptShownKey,
                settingsShim.GetValueOrDefault("push:initialPromptShown", false));
            await storageService.SaveAsync(Constants.PushCurrentTokenKey,
                settingsShim.GetValueOrDefault("push:currentToken", null));
            await storageService.SaveAsync(Constants.PushRegisteredTokenKey,
                settingsShim.GetValueOrDefault("push:registeredToken", null));
            await storageService.SaveAsync(Constants.PushLastRegistrationDateKey,
                settingsShim.GetValueOrDefault("push:lastRegistrationDate", null));
            await storageService.SaveAsync("rememberedEmail",
                settingsShim.GetValueOrDefault("other:lastLoginEmail", null));
            await storageService.SaveAsync(Constants.FingerprintUnlockKey,
                settingsShim.GetValueOrDefault("setting:fingerprintUnlockOn", false));


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
                Uppercase = settingsShim.GetValueOrDefault("pwGenerator:uppercase", false),
                Lowercase = settingsShim.GetValueOrDefault("pwGenerator:lowercase", true),
                Number = settingsShim.GetValueOrDefault("pwGenerator:numbers", false),
                MinNumber = settingsShim.GetValueOrDefault("pwGenerator:minNumbers", 0),
                Special = settingsShim.GetValueOrDefault("pwGenerator:special", false),
                MinSpecial = settingsShim.GetValueOrDefault("pwGenerator:minSpecial", 0)
            });

            int? lockOptionsSeconds = settingsShim.GetValueOrDefault("setting:lockSeconds", -2);
            if(lockOptionsSeconds == -2)
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

            // Save pin

            var pinKey = await cryptoService.MakePinKeyAysnc(oldPin, oldEmail, oldKdf, oldKdfIterations);
            var pinProtectedKey = await cryptoService.EncryptAsync(oldKeyBytes, pinKey);
            await storageService.SaveAsync(Constants.PinProtectedKey, pinProtectedKey.EncryptedString);

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

            // Remove "needs migration" flag
            settingsShim.Remove(Constants.OldUserIdKey);
            Migrating = false;
            messagingService.Send("migrated");
            return true;
        }
    }
}
