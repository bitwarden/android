using System;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class AccountSwitchingOverlayView : ContentView
    {
        public static readonly BindableProperty MainPageProperty = BindableProperty.Create(
            nameof(MainPage),
            typeof(ContentPage),
            typeof(AccountSwitchingOverlayView),
            defaultBindingMode: BindingMode.OneWay);

        public static readonly BindableProperty MainFabProperty = BindableProperty.Create(
            nameof(MainFab),
            typeof(View),
            typeof(AccountSwitchingOverlayView),
            defaultBindingMode: BindingMode.OneWay);

        public ContentPage MainPage
        {
            get => (ContentPage)GetValue(MainPageProperty);
            set => SetValue(MainPageProperty, value);
        }

        public View MainFab
        {
            get => (View)GetValue(MainFabProperty);
            set => SetValue(MainFabProperty, value);
        }

        readonly LazyResolve<ILogger> _logger = new LazyResolve<ILogger>("logger");

        public AccountSwitchingOverlayView()
        {
            InitializeComponent();

            ToggleVisibilityCommand = new AsyncCommand(ToggleVisibilityAsync,
                onException: ex => _logger.Value.Exception(ex),
                allowsMultipleExecutions: false);

            SelectAccountCommand = new AsyncCommand<AccountViewCellViewModel>(SelectAccountAsync,
                onException: ex => _logger.Value.Exception(ex),
                allowsMultipleExecutions: false);

            LongPressAccountCommand = new AsyncCommand<AccountViewCellViewModel>(LongPressAccountAsync,
                onException: ex => _logger.Value.Exception(ex),
                allowsMultipleExecutions: false);
        }

        public AccountSwitchingOverlayViewModel ViewModel => BindingContext as AccountSwitchingOverlayViewModel;

        public ICommand ToggleVisibilityCommand { get; }

        public ICommand SelectAccountCommand { get; }

        public ICommand LongPressAccountCommand { get; }

        public int AccountListRowHeight => Device.RuntimePlatform == Device.Android ? 74 : 70;

        public bool LongPressAccountEnabled { get; set; } = true;

        public Action AfterHide { get; set; }

        public async Task ToggleVisibilityAsync()
        {
            if (IsVisible)
            {
                await HideAsync();
            }
            else
            {
                await ShowAsync();
            }
        }

        public async Task ShowAsync()
        {
            if (ViewModel == null)
            {
                return;
            }

            await ViewModel.RefreshAccountViewsAsync();

            await Device.InvokeOnMainThreadAsync(async () =>
            {
                // start listView in default (off-screen) position
                await _accountListContainer.TranslateTo(0, _accountListContainer.Height * -1, 0);

                // re-measure in case accounts have been removed without changing screens
                if (ViewModel.AccountViews != null)
                {
                    _accountListView.HeightRequest = AccountListRowHeight * ViewModel.AccountViews.Count;
                }

                // set overlay opacity to zero before making visible and start fade-in
                Opacity = 0;
                IsVisible = true;
                this.FadeTo(1, 100);

                if (Device.RuntimePlatform == Device.Android && MainFab != null)
                {
                    // start fab fade-out
                    MainFab.FadeTo(0, 200);
                }

                // slide account list into view
                await _accountListContainer.TranslateTo(0, 0, 200, Easing.SinOut);
            });
        }

        public async Task HideAsync()
        {
            if (!IsVisible)
            {
                // already hidden, don't animate again
                return;
            }
            // Not all animations are awaited. This is intentional to allow multiple simultaneous animations.
            await Device.InvokeOnMainThreadAsync(async () =>
            {
                // start overlay fade-out
                this.FadeTo(0, 200);

                if (Device.RuntimePlatform == Device.Android && MainFab != null)
                {
                    // start fab fade-in
                    MainFab.FadeTo(1, 200);
                }

                // slide account list out of view
                await _accountListContainer.TranslateTo(0, _accountListContainer.Height * -1, 200, Easing.SinIn);

                // remove overlay
                IsVisible = false;

                AfterHide?.Invoke();
            });
        }

        private async void FreeSpaceOverlay_Tapped(object sender, EventArgs e)
        {
            try
            {
                await HideAsync();
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }

        private async Task SelectAccountAsync(AccountViewCellViewModel item)
        {
            try
            {
                await Task.Delay(100);
                await HideAsync();

                ViewModel?.SelectAccountCommand?.Execute(item);
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }

        private async Task LongPressAccountAsync(AccountViewCellViewModel item)
        {
            if (!LongPressAccountEnabled || !item.IsAccount)
            {
                return;
            }
            try
            {
                await Task.Delay(100);
                await HideAsync();

                ViewModel?.LongPressAccountCommand?.Execute(
                    new Tuple<ContentPage, AccountViewCellViewModel>(MainPage, item));
            }
            catch (Exception ex)
            {
                _logger.Value.Exception(ex);
            }
        }
    }
}
