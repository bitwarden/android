using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Input;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class AttachmentsPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IFileService _fileService;
        private readonly ICipherService _cipherService;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ILogger _logger;
        private CipherView _cipher;
        private Cipher _cipherDomain;
        private bool _hasAttachments;
        private bool _hasUpdatedKey;
        private bool _canAccessAttachments;
        private string _fileName;

        public AttachmentsPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _fileService = ServiceContainer.Resolve<IFileService>();
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _logger = ServiceContainer.Resolve<ILogger>();
            Attachments = new ExtendedObservableCollection<AttachmentView>();
            DeleteAttachmentCommand = new Command<AttachmentView>(DeleteAsync);
            SubmitAsyncCommand = new AsyncCommand(SubmitAsync, allowsMultipleExecutions: false);
            PageTitle = AppResources.Attachments;
        }

        public string CipherId { get; set; }
        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value);
        }
        public ExtendedObservableCollection<AttachmentView> Attachments { get; set; }
        public bool HasAttachments
        {
            get => _hasAttachments;
            set => SetProperty(ref _hasAttachments, value);
        }
        public string FileName
        {
            get => _fileName;
            set => SetProperty(ref _fileName, value);
        }
        public byte[] FileData { get; set; }
        public Command DeleteAttachmentCommand { get; set; }
        public ICommand SubmitAsyncCommand { get; }

        public async Task InitAsync()
        {
            _cipherDomain = await _cipherService.GetAsync(CipherId);
            Cipher = await _cipherDomain.DecryptAsync();
            LoadAttachments();
            _hasUpdatedKey = await _cryptoService.HasUserKeyAsync();
            var canAccessPremium = await _stateService.CanAccessPremiumAsync();
            _canAccessAttachments = canAccessPremium || Cipher.OrganizationId != null;
            if (!_canAccessAttachments)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.PremiumRequired);
            }
            else if (!_hasUpdatedKey)
            {
                var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.UpdateKey,
                    AppResources.FeatureUnavailable, AppResources.LearnMore, AppResources.Cancel);
                if (confirmed)
                {
                    _platformUtilsService.LaunchUri("https://bitwarden.com/help/account-encryption-key/#rotate-your-encryption-key");
                }
            }
        }

        public async Task<bool> SubmitAsync()
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            if (!_hasUpdatedKey)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.UpdateKey,
                    AppResources.AnErrorHasOccurred);
                return false;
            }
            if (FileData == null)
            {
                await _platformUtilsService.ShowDialogAsync(
                    string.Format(AppResources.ValidationFieldRequired, AppResources.File),
                    AppResources.AnErrorHasOccurred);
                return false;
            }
            if (FileData.Length > 104857600) // 100 MB
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.MaxFileSize,
                    AppResources.AnErrorHasOccurred);
                return false;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                _cipherDomain = await _cipherService.SaveAttachmentRawWithServerAsync(
                    _cipherDomain, Cipher, FileName, FileData);
                Cipher = await _cipherDomain.DecryptAsync();
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.AttachementAdded);
                LoadAttachments();
                FileData = null;
                FileName = null;
                return true;
            }
            catch (ApiException e)
            {
                _logger.Exception(e);
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            catch (Exception e)
            {
                _logger.Exception(e);
                await _deviceActionService.HideLoadingAsync();
                await _platformUtilsService.ShowDialogAsync(AppResources.GenericErrorMessage, AppResources.AnErrorHasOccurred);
            }
            return false;
        }

        public async Task ChooseFileAsync()
        {
            // Prevent Android from locking if vault timeout set to "immediate"
            if (Device.RuntimePlatform == Device.Android)
            {
                _vaultTimeoutService.DelayLockAndLogoutMs = 60000;
            }
            await _fileService.SelectFileAsync();
        }

        private async void DeleteAsync(AttachmentView attachment)
        {
            if (Xamarin.Essentials.Connectivity.NetworkAccess == Xamarin.Essentials.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return;
            }
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.DoYouReallyWantToDelete,
                null, AppResources.Yes, AppResources.No);
            if (!confirmed)
            {
                return;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Deleting);
                await _cipherService.DeleteAttachmentWithServerAsync(Cipher.Id, attachment.Id);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.AttachmentDeleted);
                var attachmentToRemove = Cipher.Attachments.FirstOrDefault(a => a.Id == attachment.Id);
                if (attachmentToRemove != null)
                {
                    Cipher.Attachments.Remove(attachmentToRemove);
                    LoadAttachments();
                }
            }
            catch (ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                if (e?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(e.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
        }

        private void LoadAttachments()
        {
            Attachments.ResetWithRange(Cipher.Attachments ?? new List<AttachmentView>());
            HasAttachments = Cipher.HasAttachments;
        }
    }
}
