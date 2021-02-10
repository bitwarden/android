using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.App.Utilities;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SendAddEditPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ISendService _sendService;
        private bool _canAccessPremium;
        private SendView _send;
        private bool _showEditorSeparators;
        private Thickness _editorMargins;
        private bool _showPassword;
        private int _typeSelectedIndex;
        private int _deletionDateTypeSelectedIndex;
        private int _expirationDateTypeSelectedIndex;
        private DateTime _deletionDate;
        private TimeSpan _deletionTime;
        private DateTime? _expirationDate;
        private TimeSpan? _expirationTime;
        private bool _isOverridingPickers;
        private int? _maxAccessCount;
        private string[] _additionalSendProperties = new []
        {
            nameof(IsText),
            nameof(IsFile),
        };

        public SendAddEditPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _sendService = ServiceContainer.Resolve<ISendService>("sendService");
            TogglePasswordCommand = new Command(TogglePassword);

            TypeOptions = new List<KeyValuePair<string, SendType>>
            {
                new KeyValuePair<string, SendType>(AppResources.TypeText, SendType.Text),
                new KeyValuePair<string, SendType>(AppResources.TypeFile, SendType.File),
            };
            DeletionTypeOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>(AppResources.OneHour, AppResources.OneHour),
                new KeyValuePair<string, string>(AppResources.OneDay, AppResources.OneDay),
                new KeyValuePair<string, string>(AppResources.TwoDays, AppResources.TwoDays),
                new KeyValuePair<string, string>(AppResources.ThreeDays, AppResources.ThreeDays),
                new KeyValuePair<string, string>(AppResources.SevenDays, AppResources.SevenDays),
                new KeyValuePair<string, string>(AppResources.ThirtyDays, AppResources.ThirtyDays),
                new KeyValuePair<string, string>(AppResources.Custom, AppResources.Custom),
            };
            ExpirationTypeOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>(AppResources.Never, AppResources.Never),
                new KeyValuePair<string, string>(AppResources.OneHour, AppResources.OneHour),
                new KeyValuePair<string, string>(AppResources.OneDay, AppResources.OneDay),
                new KeyValuePair<string, string>(AppResources.TwoDays, AppResources.TwoDays),
                new KeyValuePair<string, string>(AppResources.ThreeDays, AppResources.ThreeDays),
                new KeyValuePair<string, string>(AppResources.SevenDays, AppResources.SevenDays),
                new KeyValuePair<string, string>(AppResources.ThirtyDays, AppResources.ThirtyDays),
                new KeyValuePair<string, string>(AppResources.Custom, AppResources.Custom),
            };
        }

        public Command TogglePasswordCommand { get; set; }
        public string SendId { get; set; }
        public SendType? Type { get; set; }
        public string FileName { get; set; }
        public byte[] FileData { get; set; }
        public string NewPassword { get; set; }
        public bool ShareOnSave { get; set; }
        public List<KeyValuePair<string, SendType>> TypeOptions { get; }
        public List<KeyValuePair<string, string>> DeletionTypeOptions { get; }
        public List<KeyValuePair<string, string>> ExpirationTypeOptions { get; }
        public int TypeSelectedIndex
        {
            get => _typeSelectedIndex;
            set
            {
                if (SetProperty(ref _typeSelectedIndex, value))
                {
                    TypeChanged();
                }
            }
        }
        public int DeletionDateTypeSelectedIndex
        {
            get => _deletionDateTypeSelectedIndex;
            set
            {
                if (SetProperty(ref _deletionDateTypeSelectedIndex, value))
                {
                    DeletionTypeChanged();
                }
            }
        }
        public DateTime DeletionDate
        {
            get => _deletionDate;
            set => SetProperty(ref _deletionDate, value);
        }
        public TimeSpan DeletionTime
        {
            get => _deletionTime;
            set => SetProperty(ref _deletionTime, value);
        }
        public int ExpirationDateTypeSelectedIndex
        {
            get => _expirationDateTypeSelectedIndex;
            set
            {
                if (SetProperty(ref _expirationDateTypeSelectedIndex, value))
                {
                    ExpirationTypeChanged();
                }
            }
        }
        public DateTime? ExpirationDate
        {
            get => _expirationDate;
            set
            {
                if (SetProperty(ref _expirationDate, value))
                {
                    ExpirationDateChanged();
                }
            }
        }
        public TimeSpan? ExpirationTime
        {
            get => _expirationTime;
            set
            {
                if (SetProperty(ref _expirationTime, value))
                {
                    ExpirationTimeChanged();
                }
            }
        }
        public int? MaxAccessCount
        {
            get => _maxAccessCount;
            set
            {
                if (SetProperty(ref _maxAccessCount, value))
                {
                    MaxAccessCountChanged();
                }
            }
        }
        public SendView Send
        {
            get => _send;
            set => SetProperty(ref _send, value, additionalPropertyNames: _additionalSendProperties);
        }
        public bool ShowEditorSeparators
        {
            get => _showEditorSeparators;
            set => SetProperty(ref _showEditorSeparators, value);
        }
        public Thickness EditorMargins
        {
            get => _editorMargins;
            set => SetProperty(ref _editorMargins, value);
        }
        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new []
                {
                    nameof(ShowPasswordIcon)
                });
        }
        public bool EditMode => !string.IsNullOrWhiteSpace(SendId);
        public bool IsText => Send?.Type == SendType.Text;
        public bool IsFile => Send?.Type == SendType.File;
        public bool ShowDeletionCustomPickers => EditMode || DeletionDateTypeSelectedIndex == 6;
        public bool ShowExpirationCustomPickers => EditMode || ExpirationDateTypeSelectedIndex == 7;
        public string ShowPasswordIcon => ShowPassword ? "" : "";

        public void Init()
        {
            PageTitle = EditMode ? AppResources.EditSend : AppResources.AddSend;
        }

        public async Task<bool> LoadAsync()
        {
            var userService = ServiceContainer.Resolve<IUserService>("userService");
            _canAccessPremium = await userService.CanAccessPremiumAsync();
            // TODO Policy Check
            if (Send == null)
            {
                if (EditMode)
                {
                    var send = await _sendService.GetAsync(SendId);
                    if (send == null)
                    {
                        return false;
                    }
                    Send = await send.DecryptAsync();
                }
                else
                {
                    Send = new SendView
                    {
                        Type = Type.GetValueOrDefault(SendType.Text),
                        DeletionDate = DateTime.Now.AddDays(7),
                    };
                    DeletionDateTypeSelectedIndex = 4;
                    ExpirationDateTypeSelectedIndex = 0;
                }

                TypeSelectedIndex = TypeOptions.FindIndex(k => k.Value == Send.Type);
                MaxAccessCount = Send.MaxAccessCount;
                _isOverridingPickers = true;
                DeletionDate = Send.DeletionDate.ToLocalTime();
                DeletionTime = DeletionDate.TimeOfDay;
                ExpirationDate = Send.ExpirationDate?.ToLocalTime();
                ExpirationTime = ExpirationDate?.TimeOfDay;
                _isOverridingPickers = false;
            }

            return true;
        }

        public async Task ChooseFileAsync()
        {
            await _deviceActionService.SelectFileAsync();
        }

        public void ClearExpirationDate()
        {
            _isOverridingPickers = true;
            ExpirationDate = null;
            ExpirationTime = null;
            _isOverridingPickers = false;
        }

        private void UpdateSendData()
        {
            // filename
            if (Send.File != null && FileName != null)
            {
                Send.File.FileName = FileName;
            }

            // deletion date
            if (ShowDeletionCustomPickers)
            {
                Send.DeletionDate = DeletionDate.Date.Add(DeletionTime).ToUniversalTime();
            }
            else
            {
                Send.DeletionDate = DeletionDate.ToUniversalTime();
            }

            // expiration date
            if (ExpirationDate.HasValue)
            {
                if (ShowExpirationCustomPickers && ExpirationTime.HasValue)
                {
                    Send.ExpirationDate = ExpirationDate.Value.Date.Add(ExpirationTime.Value).ToUniversalTime();
                }
                else
                {
                    Send.ExpirationDate = ExpirationDate.Value.ToUniversalTime();
                }
            }
            else
            {
                Send.ExpirationDate = null;
            }
        }

        public async Task<bool> SubmitAsync()
        {
            if (Send == null)
            {
                return false;
            }
            if (Connectivity.NetworkAccess == NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            if (string.IsNullOrWhiteSpace(Send.Name))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.Name),
                    AppResources.Ok);
                return false;
            }
            if (IsFile)
            {
                if (!_canAccessPremium)
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.PremiumRequired);
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
            }

            UpdateSendData();

            var (send, encryptedFileData) = await _sendService.EncryptAsync(Send, FileData, NewPassword);
            if (send == null)
            {
                return false;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                var sendId = await _sendService.SaveWithServerAsync(send, encryptedFileData);
                await _deviceActionService.HideLoadingAsync();

                _platformUtilsService.ShowToast("success", null,
                    EditMode ? AppResources.SendUpdated : AppResources.NewSendCreated);
                await Page.Navigation.PopModalAsync();

                if (ShareOnSave)
                {
                    var savedSend = await _sendService.GetAsync(sendId);
                    if (savedSend != null)
                    {
                        var savedSendView = await savedSend.DecryptAsync();
                        await AppHelpers.ShareSendUrl(savedSendView);
                    }
                }

                return true;
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
            return false;
        }

        public async Task<bool> RemovePasswordAsync()
        {
            return await AppHelpers.RemoveSendPasswordAsync(SendId);
        }

        public async Task CopyLinkAsync()
        {
            await _platformUtilsService.CopyToClipboardAsync(AppHelpers.GetSendUrl(Send));
            _platformUtilsService.ShowToast("info", null,
                string.Format(AppResources.ValueHasBeenCopied, AppResources.ShareLink));
        }

        public async Task ShareLinkAsync()
        {
            await AppHelpers.ShareSendUrl(Send);
        }

        public async Task<bool> DeleteAsync()
        {
            return await AppHelpers.DeleteSendAsync(SendId);
        }

        private async void TypeChanged()
        {
            if (Send != null && TypeSelectedIndex > -1)
            {
                if (!EditMode && TypeOptions[TypeSelectedIndex].Value == SendType.File && !_canAccessPremium)
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.PremiumRequired);
                    TypeSelectedIndex = 0;
                }
                Send.Type = TypeOptions[TypeSelectedIndex].Value;
                TriggerPropertyChanged(nameof(Send), _additionalSendProperties);
            }
        }

        private void DeletionTypeChanged()
        {
            if (Send != null && DeletionDateTypeSelectedIndex > -1)
            {
                _isOverridingPickers = true;
                switch (DeletionDateTypeSelectedIndex)
                {
                    case 0:
                        DeletionDate = DateTime.Now.AddHours(1);
                        break;
                    case 1:
                        DeletionDate = DateTime.Now.AddDays(1);
                        break;
                    case 2:
                        DeletionDate = DateTime.Now.AddDays(2);
                        break;
                    case 3:
                        DeletionDate = DateTime.Now.AddDays(3);
                        break;
                    case 4:
                    case 6:
                        DeletionDate = DateTime.Now.AddDays(7);
                        break;
                    case 5:
                        DeletionDate = DateTime.Now.AddDays(30);
                        break;
                }
                DeletionTime = DeletionDate.TimeOfDay;
                _isOverridingPickers = false;
                TriggerPropertyChanged(nameof(ShowDeletionCustomPickers));
            }
        }

        private void ExpirationTypeChanged()
        {
            if (Send != null && ExpirationDateTypeSelectedIndex > -1)
            {
                _isOverridingPickers = true;
                switch (ExpirationDateTypeSelectedIndex)
                {
                    case 0:
                        ClearExpirationDate();
                        break;
                    case 1:
                        ExpirationDate = DateTime.Now.AddHours(1);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 2:
                        ExpirationDate = DateTime.Now.AddDays(1);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 3:
                        ExpirationDate = DateTime.Now.AddDays(2);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 4:
                        ExpirationDate = DateTime.Now.AddDays(3);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 5:
                        ExpirationDate = DateTime.Now.AddDays(7);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 6:
                        ExpirationDate = DateTime.Now.AddDays(30);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 7:
                        ClearExpirationDate();
                        break;
                }
                _isOverridingPickers = false;
                TriggerPropertyChanged(nameof(ShowExpirationCustomPickers));
            }
        }

        private void ExpirationDateChanged()
        {
            if (!_isOverridingPickers && !ExpirationTime.HasValue)
            {
                // auto-set time to current time upon setting date
                ExpirationTime = DateTime.Now.TimeOfDay;
            }
        }

        private void ExpirationTimeChanged()
        {
            if (!_isOverridingPickers && !ExpirationDate.HasValue)
            {
                // auto-set date to current date upon setting time
                ExpirationDate = DateTime.Today;
            }
        }

        private void MaxAccessCountChanged()
        {
            Send.MaxAccessCount = _maxAccessCount;
        }

        private void TogglePassword()
        {
            ShowPassword = !ShowPassword;
        }
    }
}
