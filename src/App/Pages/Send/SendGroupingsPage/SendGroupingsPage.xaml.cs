using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public partial class SendGroupingsPage : BaseContentPage
    {
        private readonly IBroadcasterService _broadcasterService;
        private readonly ISyncService _syncService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ISendService _sendService;
        private readonly SendGroupingsPageViewModel _vm;
        private readonly string _pageName;

        private AppOptions _appOptions;

        public SendGroupingsPage(bool mainPage, SendType? type = null, string pageTitle = null,
            AppOptions appOptions = null)
        {
            _pageName = string.Concat(nameof(SendGroupingsPage), "_", DateTime.UtcNow.Ticks);
            InitializeComponent();
            SetActivityIndicator(_mainContent);
            _broadcasterService = ServiceContainer.Resolve<IBroadcasterService>("broadcasterService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _sendService = ServiceContainer.Resolve<ISendService>("sendService");
            _vm = BindingContext as SendGroupingsPageViewModel;
            _vm.Page = this;
            _vm.MainPage = mainPage;
            _vm.Type = type;
            _appOptions = appOptions;
            if (pageTitle != null)
            {
                _vm.PageTitle = pageTitle;
            }

            if (Device.RuntimePlatform == Device.iOS)
            {
                _absLayout.Children.Remove(_fab);
                if (type == null)
                {
                    ToolbarItems.Add(_aboutIconItem);
                }
                ToolbarItems.Add(_addItem);
            }
            else
            {
                ToolbarItems.Add(_syncItem);
                ToolbarItems.Add(_lockItem);
                ToolbarItems.Add(_aboutTextItem);
            }
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();
            await _vm.InitAsync();
            if (_syncService.SyncInProgress)
            {
                IsBusy = true;
            }

            _broadcasterService.Subscribe(_pageName, async (message) =>
            {
                if (message.Command == "syncStarted")
                {
                    Device.BeginInvokeOnMainThread(() => IsBusy = true);
                }
                else if (message.Command == "syncCompleted" || message.Command == "sendUpdated")
                {
                    await Task.Delay(500);
                    Device.BeginInvokeOnMainThread(() =>
                    {
                        IsBusy = false;
                        if (_vm.LoadedOnce)
                        {
                            var task = _vm.LoadAsync();
                        }
                    });
                }
            });

            await LoadOnAppearedAsync(_mainLayout, false, async () =>
            {
                if (!_syncService.SyncInProgress || (await _sendService.GetAllAsync()).Any())
                {
                    try
                    {
                        await _vm.LoadAsync();
                    }
                    catch (Exception e) when (e.Message.Contains("No key."))
                    {
                        await Task.Delay(1000);
                        await _vm.LoadAsync();
                    }
                }
                else
                {
                    await Task.Delay(5000);
                    if (!_vm.Loaded)
                    {
                        await _vm.LoadAsync();
                    }
                }

                AdjustToolbar();
                await CheckAddRequest();
            }, _mainContent);
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            IsBusy = false;
            _broadcasterService.Unsubscribe(_pageName);
            _vm.DisableRefreshing();
        }

        private async Task CheckAddRequest()
        {
            if (_appOptions?.CreateSend != null)
            {
                if (DoOnce())
                {
                    var page = new SendAddEditPage(_appOptions);
                    await Navigation.PushModalAsync(new NavigationPage(page));
                }
            }
        }
        
        private async void RowSelected(object sender, SelectionChangedEventArgs e)
        {
            ((ExtendedCollectionView)sender).SelectedItem = null;
            if (!DoOnce())
            {
                return;
            }
            if (!(e.CurrentSelection?.FirstOrDefault() is SendGroupingsPageListItem item))
            {
                return;
            }

            if (item.Send != null)
            {
                await _vm.SelectSendAsync(item.Send);
            }
            else if (item.Type != null)
            {
                await _vm.SelectTypeAsync(item.Type.Value);
            }
        }

        private async void Search_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                var page = new SendsPage(_vm.Filter, _vm.Type != null);
                await Navigation.PushModalAsync(new NavigationPage(page), false);
            }
        }

        private async void Sync_Clicked(object sender, EventArgs e)
        {
            await _vm.SyncAsync();
        }

        private async void Lock_Clicked(object sender, EventArgs e)
        {
            await _vaultTimeoutService.LockAsync(true, true);
        }
        
        private void About_Clicked(object sender, EventArgs e)
        {
            _vm.ShowAbout();
        }

        private async void AddButton_Clicked(object sender, EventArgs e)
        {
            if (DoOnce())
            {
                var page = new SendAddEditPage(null, null, _vm.Type);
                await Navigation.PushModalAsync(new NavigationPage(page));
            }
        }

        private void AdjustToolbar()
        {
            _addItem.IsEnabled = _vm.SendEnabled;
        }
    }
}
