using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Pages;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Utilities.Helpers
{
    public class CipherHelper : ICipherHelper
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IEventService _eventService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IClipboardService _clipboardService;
        private readonly IPasswordRepromptService _passwordRepromptService;

        public CipherHelper(IPlatformUtilsService platformUtilsService,
            IEventService eventService,
            IVaultTimeoutService vaultTimeoutService,
            IClipboardService clipboardService,
            IPasswordRepromptService passwordRepromptService)
        {
            _platformUtilsService = platformUtilsService;
            _eventService = eventService;
            _vaultTimeoutService = vaultTimeoutService;
            _clipboardService = clipboardService;
            _passwordRepromptService = passwordRepromptService;
        }

        public async Task<string> ShowCipherOptionsAsync(Page page, CipherView cipher)
        {
            var selection = await page.DisplayActionSheet(cipher.Name, AppResources.Cancel, null, await GetCipherOptionsAsync(cipher));

            if (await _vaultTimeoutService.IsLockedAsync())
            {
                _platformUtilsService.ShowToast("info", null, AppResources.VaultIsLocked);
            }
            else if (selection == AppResources.View)
            {
                await page.Navigation.PushModalAsync(new NavigationPage(new CipherDetailsPage(cipher.Id)));
            }
            else if (selection == AppResources.Edit)
            {
                if (await RepromptPasswordIfNeededAsync(cipher))
                {
                    await page.Navigation.PushModalAsync(new NavigationPage(new CipherAddEditPage(cipher.Id)));
                }
            }
            else if (selection == AppResources.CopyUsername)
            {
                await CopyUsernameAsync(cipher);
            }
            else if (selection == AppResources.CopyPassword)
            {
                await CopyPasswordAsync(cipher);
            }
            else if (selection == AppResources.CopyTotp)
            {
                if (await RepromptPasswordIfNeededAsync(cipher))
                {
                    var totpService = ServiceContainer.Resolve<ITotpService>("totpService");
                    var totp = await totpService.GetCodeAsync(cipher.Login.Totp);
                    if (!string.IsNullOrWhiteSpace(totp))
                    {
                        await _clipboardService.CopyTextAsync(totp);
                        _platformUtilsService.ShowToastForCopiedValue(AppResources.VerificationCodeTotp);
                    }
                }
            }
            else if (selection == AppResources.Launch)
            {
                _platformUtilsService.LaunchUri(cipher.Login.LaunchUri);
            }
            else if (selection == AppResources.CopyNumber)
            {
                await CopyCardNumberAsync(cipher);
            }
            else if (selection == AppResources.CopySecurityCode)
            {
                if (await RepromptPasswordIfNeededAsync(cipher))
                {
                    await _clipboardService.CopyTextAsync(cipher.Card.Code);
                    _platformUtilsService.ShowToastForCopiedValue(AppResources.SecurityCode);
                    _eventService.CollectAsync(EventType.Cipher_ClientCopiedCardCode, cipher.Id).FireAndForget();
                }
            }
            else if (selection == AppResources.CopyNotes)
            {
                await CopyNotesAsync(cipher);
            }
            return selection;
        }

        public async Task CopyUsernameAsync(CipherView cipher)
        {
            await _clipboardService.CopyTextAsync(cipher.Login.Username);
            _platformUtilsService.ShowToastForCopiedValue(AppResources.Username);
        }

        public async Task<bool> CopyPasswordAsync(CipherView cipher)
        {
            if (await RepromptPasswordIfNeededAsync(cipher))
            {
                await _clipboardService.CopyTextAsync(cipher.Login.Password);
                _platformUtilsService.ShowToastForCopiedValue(AppResources.Password);
                _eventService.CollectAsync(EventType.Cipher_ClientCopiedPassword, cipher.Id).FireAndForget();

                return true;
            }
            return false;
        }

        public async Task<bool> CopyCardNumberAsync(CipherView cipher)
        {
            if (await RepromptPasswordIfNeededAsync(cipher))
            {
                await _clipboardService.CopyTextAsync(cipher.Card.Number);
                _platformUtilsService.ShowToastForCopiedValue(AppResources.Number);

                return true;
            }
            return false;
        }

        public async Task CopyNotesAsync(CipherView cipher)
        {
            await _clipboardService.CopyTextAsync(cipher.Notes);
            _platformUtilsService.ShowToastForCopiedValue(AppResources.Notes);
        }

        private async Task<string[]> GetCipherOptionsAsync(CipherView cipher)
        {
            var options = new List<string> { AppResources.View };
            if (!cipher.IsDeleted)
            {
                options.Add(AppResources.Edit);
            }
            if (cipher.Type == CipherType.Login)
            {
                if (!string.IsNullOrWhiteSpace(cipher.Login.Username))
                {
                    options.Add(AppResources.CopyUsername);
                }
                if (!string.IsNullOrWhiteSpace(cipher.Login.Password) && cipher.ViewPassword)
                {
                    options.Add(AppResources.CopyPassword);
                }
                if (!string.IsNullOrWhiteSpace(cipher.Login.Totp))
                {
                    var stateService = ServiceContainer.Resolve<IStateService>("stateService");
                    var canAccessPremium = await stateService.CanAccessPremiumAsync();
                    if (canAccessPremium || cipher.OrganizationUseTotp)
                    {
                        options.Add(AppResources.CopyTotp);
                    }
                }
                if (cipher.Login.CanLaunch)
                {
                    options.Add(AppResources.Launch);
                }
            }
            else if (cipher.Type == CipherType.Card)
            {
                if (!string.IsNullOrWhiteSpace(cipher.Card.Number))
                {
                    options.Add(AppResources.CopyNumber);
                }
                if (!string.IsNullOrWhiteSpace(cipher.Card.Code))
                {
                    options.Add(AppResources.CopySecurityCode);
                }
            }
            else if (cipher.Type == CipherType.SecureNote)
            {
                if (!string.IsNullOrWhiteSpace(cipher.Notes))
                {
                    options.Add(AppResources.CopyNotes);
                }
            }

            return options.ToArray();
        }

        private async Task<bool> RepromptPasswordIfNeededAsync(CipherView cipher)
        {
            return cipher.Reprompt == CipherRepromptType.None || await _passwordRepromptService.ShowPasswordPromptAsync();
        }
    }
}
