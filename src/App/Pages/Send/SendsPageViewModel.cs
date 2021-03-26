using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SendsPageViewModel : BaseViewModel
    {
        private readonly ISearchService _searchService;

        private CancellationTokenSource _searchCancellationTokenSource;
        private bool _sendEnabled;
        private bool _showNoData;
        private bool _showList;

        public SendsPageViewModel()
        {
            _searchService = ServiceContainer.Resolve<ISearchService>("searchService");
            Sends = new ExtendedObservableCollection<SendView>();
            SendOptionsCommand = new Command<SendView>(SendOptionsAsync);
        }

        public Command SendOptionsCommand { get; set; }
        public ExtendedObservableCollection<SendView> Sends { get; set; }
        public Func<SendView, bool> Filter { get; set; }

        public bool SendEnabled
        {
            get => _sendEnabled;
            set => SetProperty(ref _sendEnabled, value);
        }
        
        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value, additionalPropertyNames: new []
            {
                nameof(ShowSearchDirection)
            });
        }

        public bool ShowList
        {
            get => _showList;
            set => SetProperty(ref _showList, value, additionalPropertyNames: new []
            {
                nameof(ShowSearchDirection)
            });
        }

        public bool ShowSearchDirection => !ShowList && !ShowNoData;

        public async Task InitAsync()
        {
            SendEnabled = ! await AppHelpers.IsSendDisabledByPolicyAsync();
            if (!string.IsNullOrWhiteSpace((Page as SendsPage).SearchBar.Text))
            {
                Search((Page as SendsPage).SearchBar.Text, 200);
            }
        }

        public void Search(string searchText, int? timeout = null)
        {
            var previousCts = _searchCancellationTokenSource;
            var cts = new CancellationTokenSource();
            Task.Run(async () =>
            {
                List<SendView> sends = null;
                var searchable = !string.IsNullOrWhiteSpace(searchText) && searchText.Length > 1;
                if (searchable)
                {
                    if (timeout != null)
                    {
                        await Task.Delay(timeout.Value);
                    }
                    if (searchText != (Page as SendsPage).SearchBar.Text)
                    {
                        return;
                    }
                    else
                    {
                        previousCts?.Cancel();
                    }
                    try
                    {
                        sends = await _searchService.SearchSendsAsync(searchText, Filter, null, cts.Token);
                        cts.Token.ThrowIfCancellationRequested();
                    }
                    catch (OperationCanceledException)
                    {
                        return;
                    }
                }
                if (sends == null)
                {
                    sends = new List<SendView>();
                }
                Device.BeginInvokeOnMainThread(() =>
                {
                    Sends.ResetWithRange(sends);
                    ShowNoData = searchable && Sends.Count == 0;
                    ShowList = searchable && !ShowNoData;
                });
            }, cts.Token);
            _searchCancellationTokenSource = cts;
        }

        public async Task SelectSendAsync(SendView send)
        {
            var page = new SendAddEditPage(null, send.Id);
            await Page.Navigation.PushModalAsync(new NavigationPage(page));
        }

        private async void SendOptionsAsync(SendView send)
        {
            if ((Page as BaseContentPage).DoOnce())
            {
                await AppHelpers.SendListOptions(Page, send);
            }
        }
    }
}
