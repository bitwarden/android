using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Effects;
using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.App.Utilities;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Bit.Core.Services;

namespace Bit.App.Pages
{
    public class TabsPage : TabbedPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly IMessagingService _messagingService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly IStateService _stateService;
        private readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        private NavigationPage _groupingsPage;
        private NavigationPage _sendGroupingsPage;
        private NavigationPage _generatorPage;

        public TabsPage(AppOptions appOptions = null, PreviousPageInfo previousPage = null)
        {
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _keyConnectorService = ServiceContainer.Resolve<IKeyConnectorService>("keyConnectorService");
            _stateService = ServiceContainer.Resolve<IStateService>();

            _groupingsPage = new NavigationPage(new GroupingsPage(true, previousPage: previousPage, appOptions: appOptions))
            {
                Title = AppResources.MyVault,
                IconImageSource = "lock.png"
            };
            Children.Add(_groupingsPage);

            _sendGroupingsPage = new NavigationPage(new SendGroupingsPage(true, null, null, appOptions))
            {
                Title = AppResources.Send,
                IconImageSource = "send.png",
            };
            Children.Add(_sendGroupingsPage);

            _generatorPage = new NavigationPage(new GeneratorPage(true, null, this))
            {
                Title = AppResources.Generator,
                IconImageSource = "generate.png"
            };
            Children.Add(_generatorPage);

            var settingsPage = new NavigationPage(new SettingsPage(this))
            {
                Title = AppResources.Settings,
                IconImageSource = "cog_settings.png"
            };
            Children.Add(settingsPage);

            Unloaded += OnUnloaded;

            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific.TabbedPage.SetToolbarPlacement(this,
                Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific.ToolbarPlacement.Bottom);
                Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific.TabbedPage.SetIsSwipePagingEnabled(this, false);
                Microsoft.Maui.Controls.PlatformConfiguration.AndroidSpecific.TabbedPage.SetIsSmoothScrollEnabled(this, false);
            }

            if (appOptions?.GeneratorTile ?? false)
            {
                appOptions.GeneratorTile = false;
                ResetToGeneratorPage();
            }
            else if (appOptions?.MyVaultTile ?? false)
            {
                appOptions.MyVaultTile = false;
            }
            else if (appOptions?.CreateSend != null)
            {
                ResetToSendPage();
            }
        }

        protected override async void OnAppearing()
        {
            try
            {
                base.OnAppearing();
                _broadcasterService.Subscribe(nameof(TabsPage), async (message) =>
                {
                    if (message.Command == "syncCompleted")
                    {
                        MainThread.BeginInvokeOnMainThread(async () => await UpdateVaultButtonTitleAsync());
                        try
                        {
                            await ForcePasswordResetIfNeededAsync();
                        }
                        catch (Exception ex)
                        {
                            _logger.Value.Exception(ex);
                        }
                    }
                });
                await UpdateVaultButtonTitleAsync();
                if (await _keyConnectorService.UserNeedsMigrationAsync())
                {
                    _messagingService.Send("convertAccountToKeyConnector");
                }

                await ForcePasswordResetIfNeededAsync();
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        private async Task ForcePasswordResetIfNeededAsync()
        {
            var forcePasswordResetReason = await _stateService.GetForcePasswordResetReasonAsync();
            switch (forcePasswordResetReason)
            {
                case ForcePasswordResetReason.TdeUserWithoutPasswordHasPasswordResetPermission:
                    // TDE users should only have one org
                    var userOrgs = await _stateService.GetOrganizationsAsync();
                    if (userOrgs != null && userOrgs.Any())
                    {
                        _messagingService.Send(Constants.ForceSetPassword, userOrgs.First().Value.Identifier);
                        return;
                    }
                    _logger.Value.Error("TDE user needs to set password but has no organizations.");

                    var rememberedOrg = _stateService.GetRememberedOrgIdentifierAsync();
                    if (rememberedOrg == null)
                    {
                        _logger.Value.Error("TDE user needs to set password but has no organizations or remembered org identifier.");
                        return;
                    }
                    _messagingService.Send(Constants.ForceSetPassword, rememberedOrg);
                    return;
                case ForcePasswordResetReason.AdminForcePasswordReset:
                case ForcePasswordResetReason.WeakMasterPasswordOnLogin:
                    _messagingService.Send(Constants.ForceUpdatePassword);
                    break;
                default:
                    return;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _broadcasterService.Unsubscribe(nameof(TabsPage));
        }

        private void OnUnloaded(object sender, EventArgs e)
        {
            try
            {
                Handler?.DisconnectHandler();
            }
            catch (Exception ex)
            {
                //Workaround: Currently the Disconnect Handler needs to be manually called from the App: https://github.com/dotnet/maui/issues/3604
                // In some specific edges cases the MauiContext can be gone when we call this. (for example filling a field using Accessibility)
                // In those scenarios the app should just be "closing" anyway, so we just want to avoid the exception.
                System.Diagnostics.Debug.WriteLine(ex.Message);
            }
        }

        public void ResetToVaultPage()
        {
            CurrentPage = _groupingsPage;
        }

        public void ResetToGeneratorPage()
        {
            CurrentPage = _generatorPage;
        }

        public void ResetToSendPage()
        {
            CurrentPage = _sendGroupingsPage;
        }

        protected override async void OnCurrentPageChanged()
        {
            try
            {
                if (CurrentPage is NavigationPage navPage)
                {
                    if (_groupingsPage?.RootPage is GroupingsPage groupingsPage)
                    {
                        await groupingsPage.HideAccountSwitchingOverlayAsync();
                    }

                    _messagingService.Send(ThemeManager.UPDATED_THEME_MESSAGE_KEY);
                    if (navPage.RootPage is GroupingsPage)
                    {
                        // Load something?
                    }
                    else if (navPage.RootPage is GeneratorPage genPage)
                    {
                        await genPage.InitAsync();
                    }
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                throw;
            }
        }

        public void OnPageReselected()
        {
            if (_groupingsPage?.RootPage is GroupingsPage groupingsPage)
            {
                groupingsPage.HideAccountSwitchingOverlayAsync().FireAndForget();
            }
        }

        private async Task UpdateVaultButtonTitleAsync()
        {
            try
            {
                var policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
                var isShowingVaultFilter = await policyService.ShouldShowVaultFilterAsync();
                _groupingsPage.Title = isShowingVaultFilter ? AppResources.Vaults : AppResources.MyVault;
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }
    }
}
