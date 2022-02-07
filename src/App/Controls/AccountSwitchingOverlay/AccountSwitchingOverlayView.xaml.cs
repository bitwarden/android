using System;
using System.Threading.Tasks;
using System.Windows.Input;
#if !FDROID
using Microsoft.AppCenter.Crashes;
#endif
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public partial class AccountSwitchingOverlayView : ContentView
    {
        public static readonly BindableProperty MainFabProperty = BindableProperty.Create(
            nameof(MainFab),
            typeof(View),
            typeof(AccountSwitchingOverlayView),
            defaultBindingMode: BindingMode.OneWay);

        public View MainFab
        {
            get => (View)GetValue(MainFabProperty);
            set => SetValue(MainFabProperty, value);
        }

        public AccountSwitchingOverlayView()
        {
            InitializeComponent();

            ToggleVisibililtyCommand = new AsyncCommand(ToggleVisibilityAsync,
#if !FDROID
                onException: ex => Crashes.TrackError(ex),
#endif
                allowsMultipleExecutions: false);
        }

        public AccountSwitchingOverlayViewModel ViewModel => BindingContext as AccountSwitchingOverlayViewModel;

        public ICommand ToggleVisibililtyCommand { get; }

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
            await ViewModel?.RefreshAccountViewsAsync();

            await Device.InvokeOnMainThreadAsync(async () =>
            {
                // start listView in default (off-screen) position
                await _accountListContainer.TranslateTo(0, _accountListContainer.Height * -1, 0);

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
#if !FDROID
                Crashes.TrackError(ex);
#endif
            }
        }

        async void AccountRow_Selected(object sender, SelectedItemChangedEventArgs e)
        {
            try
            {
                if (!(e.SelectedItem is AccountViewCellViewModel item))
                {
                    return;
                }

                ((ListView)sender).SelectedItem = null;
                await Task.Delay(100);
                await HideAsync();

                ViewModel?.SelectAccountCommand?.Execute(item);
            }
            catch (Exception ex)
            {
#if !FDROID
                Crashes.TrackError(ex);
#endif
            }
        }
    }
}
