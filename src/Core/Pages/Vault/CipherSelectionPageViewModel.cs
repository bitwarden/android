using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Models.View;
using Bit.Core.Resources.Localization;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public abstract class CipherSelectionPageViewModel : BaseViewModel
    {
        protected readonly IPlatformUtilsService _platformUtilsService;
        protected readonly IDeviceActionService _deviceActionService;
        protected readonly IAutofillHandler _autofillHandler;
        protected readonly ICipherService _cipherService;
        protected readonly IStateService _stateService;
        protected readonly IPasswordRepromptService _passwordRepromptService;
        protected readonly IMessagingService _messagingService;
        protected readonly ILogger _logger;

        protected bool _showNoData;
        protected bool _showList;
        protected string _noDataText;
        protected string _addNewItemText;
        protected bool _websiteIconsEnabled;

        public CipherSelectionPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>();
            _cipherService = ServiceContainer.Resolve<ICipherService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            _stateService = ServiceContainer.Resolve<IStateService>();
            _passwordRepromptService = ServiceContainer.Resolve<IPasswordRepromptService>();
            _messagingService = ServiceContainer.Resolve<IMessagingService>();
            _logger = ServiceContainer.Resolve<ILogger>();

            GroupedItems = new ObservableRangeCollection<IGroupingsPageListItem>();
            CipherOptionsCommand = CreateDefaultAsyncRelayCommand<CipherView>(cipher => AppHelpers.CipherListOptions(Page, cipher, _passwordRepromptService),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
            SelectCipherCommand = CreateDefaultAsyncRelayCommand<IGroupingsPageListItem>(SelectCipherAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
            AddFabCipherCommand = CreateDefaultAsyncRelayCommand(AddFabCipherAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
            AddCipherCommand = CreateDefaultAsyncRelayCommand(AddCipherAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            AccountSwitchingOverlayViewModel = new AccountSwitchingOverlayViewModel(_stateService, _messagingService, _logger)
            {
                AllowAddAccountRow = false
            };

            AddNewItemText = AppResources.AddAnItem;
        }

        public string Name { get; set; }
        public bool LoadedOnce { get; set; }
        public ObservableRangeCollection<IGroupingsPageListItem> GroupedItems { get; set; }
        public AccountSwitchingOverlayViewModel AccountSwitchingOverlayViewModel { get; }

        public ICommand CipherOptionsCommand { get; set; }
        public ICommand SelectCipherCommand { get; set; }
        public ICommand AddCipherCommand { get; set; }
        public ICommand AddFabCipherCommand { get; set; }

        public bool ShowNoData
        {
            get => _showNoData;
            set => SetProperty(ref _showNoData, value, additionalPropertyNames: new string[] { nameof(ShowCallout) });
        }

        public bool ShowList
        {
            get => _showList;
            set => SetProperty(ref _showList, value);
        }

        public string NoDataText
        {
            get => _noDataText;
            set => SetProperty(ref _noDataText, value);
        }

        public string AddNewItemText
        {
            get => _addNewItemText;
            set => SetProperty(ref _addNewItemText, value);
        }

        public bool WebsiteIconsEnabled
        {
            get => _websiteIconsEnabled;
            set => SetProperty(ref _websiteIconsEnabled, value);
        }

        public virtual bool ShowCallout => false;

        public abstract void Init(Models.AppOptions options);

        public async Task LoadAsync()
        {
            LoadedOnce = true;
            ShowList = false;
            ShowNoData = false;
            WebsiteIconsEnabled = !(await _stateService.GetDisableFaviconAsync()).GetValueOrDefault();

            var groupedItems = await LoadGroupedItemsAsync();

            // TODO: refactor this
            if (DeviceInfo.Platform == DevicePlatform.Android || GroupedItems.Any())
            {
                var items = new List<IGroupingsPageListItem>();
                foreach (var itemGroup in groupedItems)
                {
                    items.Add(new GroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                    items.AddRange(itemGroup);
                }

                GroupedItems.ReplaceRange(items);
            }
            else
            {
                // HACK: we need this on iOS, so that it doesn't crash when adding coming from an empty list
                var first = true;
                var items = new List<IGroupingsPageListItem>();
                foreach (var itemGroup in groupedItems)
                {
                    if (!first)
                    {
                        items.Add(new GroupingsPageHeaderListItem(itemGroup.Name, itemGroup.ItemCount));
                    }
                    else
                    {
                        first = false;
                    }
                    items.AddRange(itemGroup);
                }

                await MainThread.InvokeOnMainThreadAsync(() =>
                {
                    if (groupedItems.Any())
                    {
                        GroupedItems.ReplaceRange(new List<IGroupingsPageListItem> { new GroupingsPageHeaderListItem(groupedItems[0].Name, groupedItems[0].ItemCount) });
                        GroupedItems.AddRange(items);
                    }
                    else
                    {
                        GroupedItems.Clear();
                    }
                });
            }
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                ShowList = groupedItems.Any();
                ShowNoData = !ShowList;
            });
        }

        protected abstract Task<List<GroupingsPageListGroup>> LoadGroupedItemsAsync();

        protected abstract Task SelectCipherAsync(IGroupingsPageListItem item);

        protected abstract Task AddCipherAsync();
        protected abstract Task AddFabCipherAsync();
    }
}
