using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using System;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Migration
{
    public static class MigrationHelpers
    {
        public static bool NeedsMigration()
        {
            return ServiceContainer.Resolve<SettingsShim>("settingsShim")
                .GetValueOrDefault(Constants.OldLastActivityKey, DateTime.MinValue) > DateTime.MinValue;
        }

        public static async Task<bool> PerformMigrationAsync()
        {
            if(!NeedsMigration())
            {
                return false;
            }
            var settingsShim = ServiceContainer.Resolve<SettingsShim>("settingsShim");
            var oldSecureStorageService = ServiceContainer.Resolve<Abstractions.IOldSecureStorageService>(
                "oldSecureStorageService");

            var storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            var secureStorageService = ServiceContainer.Resolve<IStorageService>("secureStorageService");
            var cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            var tokenService = ServiceContainer.Resolve<ITokenService>("tokenService");
            var userService = ServiceContainer.Resolve<IUserService>("userService");

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

            // Save settings



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

            return true;
        }
    }
}
