using System;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class BlockAutofillUrisPageViewModel : BaseViewModel
    {
        private const char URI_SEPARARTOR = ',';
        private const string URI_FORMAT = "https://domain.com";

        private readonly IStateService _stateService;
        private readonly IDeviceActionService _deviceActionService;

        public BlockAutofillUrisPageViewModel()
        {
            _stateService = ServiceContainer.Resolve<IStateService>();
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>();

            AddUriCommand = new AsyncCommand(AddUriAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            EditUriCommand = new AsyncCommand<BlockAutofillUriItemViewModel>(EditUriAsync,
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);
        }

        public ObservableRangeCollection<BlockAutofillUriItemViewModel> BlockedUris { get; set; } = new ObservableRangeCollection<BlockAutofillUriItemViewModel>();

        public bool ShowList => BlockedUris.Any();

        public ICommand AddUriCommand { get; }

        public ICommand EditUriCommand { get; }

        public async Task InitAsync()
        {
            var blockedUrisList = await _stateService.GetAutofillBlacklistedUrisAsync();
            if (blockedUrisList?.Any() != true)
            {
                return;
            }
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                BlockedUris.AddRange(blockedUrisList.OrderBy(uri => uri).Select(u => new BlockAutofillUriItemViewModel(u, EditUriCommand)).ToList());
                TriggerPropertyChanged(nameof(ShowList));
            });
        }

        private async Task AddUriAsync()
        {
            var response = await _deviceActionService.DisplayValidatablePromptAsync(new Utilities.Prompts.ValidatablePromptConfig
            {
                Title = AppResources.NewUri,
                Subtitle = AppResources.EnterURI,
                ValueSubInfo = string.Format(AppResources.FormatXSeparateMultipleURIsWithAComma, URI_FORMAT),
                OkButtonText = AppResources.Save,
                ValidateText = text => ValidateUris(text, true)
            });
            if (response?.Text is null)
            {
                return;
            }

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                foreach (var uri in response.Value.Text.Split(URI_SEPARARTOR).Where(s => !string.IsNullOrEmpty(s)))
                {
                    var cleanedUri = uri.Replace(Environment.NewLine, string.Empty).Trim();
                    BlockedUris.Add(new BlockAutofillUriItemViewModel(cleanedUri, EditUriCommand));
                }

                BlockedUris = new ObservableRangeCollection<BlockAutofillUriItemViewModel>(BlockedUris.OrderBy(b => b.Uri));
                TriggerPropertyChanged(nameof(BlockedUris));
                TriggerPropertyChanged(nameof(ShowList));
            });
            await UpdateAutofillBlacklistedUrisAsync();
            _deviceActionService.Toast(AppResources.URISaved);
        }

        private async Task EditUriAsync(BlockAutofillUriItemViewModel uriItemViewModel)
        {
            var response = await _deviceActionService.DisplayValidatablePromptAsync(new Utilities.Prompts.ValidatablePromptConfig
            {
                Title = AppResources.EditURI,
                Subtitle = AppResources.EnterURI,
                Text = uriItemViewModel.Uri,
                ValueSubInfo = string.Format(AppResources.FormatX, URI_FORMAT),
                OkButtonText = AppResources.Save,
                ThirdButtonText = AppResources.Remove,
                ValidateText = text => ValidateUris(text, false)
            });
            if (response is null)
            {
                return;
            }

            if (response.Value.ExecuteThirdAction)
            {
                await MainThread.InvokeOnMainThreadAsync(() =>
                {
                    BlockedUris.Remove(uriItemViewModel);
                    TriggerPropertyChanged(nameof(ShowList));
                });
                await UpdateAutofillBlacklistedUrisAsync();
                _deviceActionService.Toast(AppResources.URIRemoved);
                return;
            }

            var cleanedUri = response.Value.Text.Replace(Environment.NewLine, string.Empty).Trim();
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                BlockedUris.Remove(uriItemViewModel);
                BlockedUris.Add(new BlockAutofillUriItemViewModel(cleanedUri, EditUriCommand));
                BlockedUris = new ObservableRangeCollection<BlockAutofillUriItemViewModel>(BlockedUris.OrderBy(b => b.Uri));
                TriggerPropertyChanged(nameof(BlockedUris));
                TriggerPropertyChanged(nameof(ShowList));
            });
            await UpdateAutofillBlacklistedUrisAsync();
            _deviceActionService.Toast(AppResources.URISaved);
        }

        private string ValidateUris(string uris, bool allowMultipleUris)
        {
            if (string.IsNullOrWhiteSpace(uris))
            {
                return string.Format(AppResources.FormatX, URI_FORMAT);
            }

            if (!allowMultipleUris && uris.Contains(URI_SEPARARTOR))
            {
                return AppResources.CannotEditMultipleURIsAtOnce;
            }

            foreach (var uri in uris.Split(URI_SEPARARTOR).Where(u => !string.IsNullOrWhiteSpace(u)))
            {
                var cleanedUri = uri.Replace(Environment.NewLine, string.Empty).Trim();
                if (!cleanedUri.StartsWith("http://") && !cleanedUri.StartsWith("https://") &&
                    !cleanedUri.StartsWith(Constants.AndroidAppProtocol))
                {
                    return AppResources.InvalidFormatUseHttpsHttpOrAndroidApp;
                }

                if (!Uri.TryCreate(cleanedUri, UriKind.Absolute, out var _))
                {
                    return AppResources.InvalidURI;
                }

                if (BlockedUris.Any(uriItem => uriItem.Uri == cleanedUri))
                {
                    return string.Format(AppResources.TheURIXIsAlreadyBlocked, cleanedUri);
                }
            }

            return null;
        }

        private async Task UpdateAutofillBlacklistedUrisAsync()
        {
            await _stateService.SetAutofillBlacklistedUrisAsync(BlockedUris.Any() ? BlockedUris.Select(bu => bu.Uri).ToList() : null);
        }
    }

    public class BlockAutofillUriItemViewModel : ExtendedViewModel
    {
        public BlockAutofillUriItemViewModel(string uri, ICommand editUriCommand)
        {
            Uri = uri;
            EditUriCommand = new Command(() => editUriCommand.Execute(this));
        }

        public string Uri { get; }

        public ICommand EditUriCommand { get; }
    }
}
