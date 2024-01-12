using System;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.App.Styles;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public partial class GeneratorPage : BaseContentPage, IThemeDirtablePage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly ILogger _logger;

        private GeneratorPageViewModel _vm;
        private readonly bool _fromTabPage;
        private readonly Action<string> _selectAction;
        private readonly TabsPage _tabsPage;

        public GeneratorPage(bool fromTabPage, Action<string> selectAction = null, TabsPage tabsPage = null, bool isUsernameGenerator = false, string emailWebsite = null, bool editMode = false, AppOptions appOptions = null)
        {
            _tabsPage = tabsPage;
            InitializeComponent();
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>();
            _logger = ServiceContainer.Resolve<ILogger>();

            _vm = BindingContext as GeneratorPageViewModel;
            _vm.Page = this;
            _fromTabPage = fromTabPage;
            _selectAction = selectAction;
            _vm.ShowTypePicker = fromTabPage;
            _vm.IsUsername = isUsernameGenerator;
            _vm.EmailWebsite = emailWebsite;
            _vm.EditMode = editMode;
            _vm.IosExtension = appOptions?.IosExtension ?? false;

            if (selectAction != null)
            {
#if IOS
                ToolbarItems.Add(_closeItem);
#endif
                ToolbarItems.Add(_selectItem);
            }
            else
            {
#if IOS
                ToolbarItems.Add(_moreItem);
#else
                ToolbarItems.Add(_historyItem);
#endif
            }
            _typePicker.On<Microsoft.Maui.Controls.PlatformConfiguration.iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            _passwordTypePicker.On<Microsoft.Maui.Controls.PlatformConfiguration.iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            _usernameTypePicker.On<Microsoft.Maui.Controls.PlatformConfiguration.iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            _serviceTypePicker.On<Microsoft.Maui.Controls.PlatformConfiguration.iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            _plusAddressedEmailTypePicker.On<Microsoft.Maui.Controls.PlatformConfiguration.iOS>().SetUpdateMode(UpdateMode.WhenFinished);
            _catchallEmailTypePicker.On<Microsoft.Maui.Controls.PlatformConfiguration.iOS>().SetUpdateMode(UpdateMode.WhenFinished);
        }

        public async Task InitAsync()
        {
            await _vm.InitAsync();
        }

        protected async override void OnAppearing()
        {
            try
            {
                base.OnAppearing();

                lblPassword.IsVisible = true;

                if (!_fromTabPage)
                {
                    await InitAsync();
                }

                _broadcasterService.Subscribe(nameof(GeneratorPage), (message) =>
                {
                    if (message.Command is ThemeManager.UPDATED_THEME_MESSAGE_KEY)
                    {
                        MainThread.BeginInvokeOnMainThread(() => _vm.RedrawPassword());
                    }
                });
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
                throw;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            lblPassword.IsVisible = false;

            _broadcasterService.Unsubscribe(nameof(GeneratorPage));
        }

        protected override bool OnBackButtonPressed()
        {
#if ANDROID
            if (_tabsPage != null)
            {
                _tabsPage.ResetToVaultPage();
                return true;
            }
#endif
            return base.OnBackButtonPressed();
        }

        private async void More_Clicked(object sender, EventArgs e)
        {
            try
            {
                if (!DoOnce())
                {
                    return;
                }
                var selection = await DisplayActionSheet(AppResources.Options, AppResources.Cancel,
                    null, AppResources.PasswordHistory);
                if (selection == AppResources.PasswordHistory)
                {
                    var page = new GeneratorHistoryPage();
                    await Navigation.PushModalAsync(new Microsoft.Maui.Controls.NavigationPage(page));
                }
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private void Select_Clicked(object sender, EventArgs e)
        {
            _selectAction?.Invoke(_vm.IsUsername ? _vm.Username : _vm.Password);
        }

        private async void History_Clicked(object sender, EventArgs e)
        {
            var page = new GeneratorHistoryPage();
            await Navigation.PushModalAsync(new Microsoft.Maui.Controls.NavigationPage(page));
        }

        private async void LengthSlider_DragCompleted(object sender, EventArgs e)
        {
            await _vm.SliderChangedAsync();
        }

        public override async Task UpdateOnThemeChanged()
        {
            await base.UpdateOnThemeChanged();

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                if (_vm != null)
                {
                    if (_vm.IsUsername)
                    {
                        _vm.RedrawUsername();
                    }
                    else
                    {
                        _vm.RedrawPassword();
                    }
                }
            });
        }
    }
}
