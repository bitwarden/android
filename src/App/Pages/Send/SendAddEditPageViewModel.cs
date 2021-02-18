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
        private readonly IUserService _userService;
        private readonly ISendService _sendService;
        private bool _sendEnabled;
        private bool _canAccessPremium;
        private SendView _send;
        private string _fileName;
        private bool _showOptions;
        private bool _showPassword;
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
            _userService = ServiceContainer.Resolve<IUserService>("userService");
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
        public int SegmentedButtonHeight { get; set; }
        public int SegmentedButtonFontSize { get; set; }
        public Thickness SegmentedButtonMargins { get; set; }
        public bool ShowEditorSeparators { get; set; }
        public Thickness EditorMargins { get; set; }
        public SendType? Type { get; set; }
        public byte[] FileData { get; set; }
        public string NewPassword { get; set; }
        public bool ShareOnSave { get; set; }
        public List<KeyValuePair<string, SendType>> TypeOptions { get; }
        public List<KeyValuePair<string, string>> DeletionTypeOptions { get; }
        public List<KeyValuePair<string, string>> ExpirationTypeOptions { get; }
        public bool SendEnabled
        {
            get => _sendEnabled;
            set => SetProperty(ref _sendEnabled, value);
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
        public bool ShowOptions
        {
            get => _showOptions;
            set => SetProperty(ref _showOptions, value);
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
        public string FileName
        {
            get => _fileName;
            set
            {
                if (SetProperty(ref _fileName, value))
                {
                    Send.File.FileName = _fileName;
                }
            }
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

        public async Task InitAsync()
        {
            PageTitle = EditMode ? AppResources.EditSend : AppResources.AddSend;
            _canAccessPremium = await _userService.CanAccessPremiumAsync();
            SendEnabled = ! await AppHelpers.IsSendDisabledByPolicyAsync();
        }

        public async Task<bool> LoadAsync()
        {
            if (Send == null)
            {
                _isOverridingPickers = true;
                if (EditMode)
                {
                    var send = await _sendService.GetAsync(SendId);
                    if (send == null)
                    {
                        return false;
                    }
                    Send = await send.DecryptAsync();
                    DeletionDate = Send.DeletionDate.ToLocalTime();
                    ExpirationDate = Send.ExpirationDate?.ToLocalTime();
                }
                else
                {
                    var defaultType = _canAccessPremium ? SendType.File : SendType.Text;
                    Send = new SendView
                    {
                        Type = Type.GetValueOrDefault(defaultType),
                    };
                    DeletionDateTypeSelectedIndex = 4;
                    ExpirationDateTypeSelectedIndex = 0;
                }

                MaxAccessCount = Send.MaxAccessCount;
                DeletionTime = DeletionDate.TimeOfDay;
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
            if (Send == null || !SendEnabled)
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
                if (!EditMode)
                {
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
                        await AppHelpers.ShareSendUrlAsync(savedSendView);
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
            await AppHelpers.CopySendUrlAsync(Send);
        }

        public async Task ShareLinkAsync()
        {
            await AppHelpers.ShareSendUrlAsync(Send);
        }

        public async Task<bool> DeleteAsync()
        {
            return await AppHelpers.DeleteSendAsync(SendId);
        }

        public async Task TypeChangedAsync(SendType type)
        {
            if (Send != null)
            {
                if (!EditMode && type == SendType.File && !_canAccessPremium)
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.PremiumRequired);
                    type = SendType.Text;
                }
                Send.Type = type;
                TriggerPropertyChanged(nameof(Send), _additionalSendProperties);
            }
        }

        public void ToggleOptions()
        {
            ShowOptions = !ShowOptions;
        }

        private void DeletionTypeChanged()
        {
            if (Send != null && DeletionDateTypeSelectedIndex > -1)
            {
                _isOverridingPickers = true;
                switch (DeletionDateTypeSelectedIndex)
                {
                    case 0:
                        DeletionDate = DateTimeNow().AddHours(1);
                        break;
                    case 1:
                        DeletionDate = DateTimeNow().AddDays(1);
                        break;
                    case 2:
                        DeletionDate = DateTimeNow().AddDays(2);
                        break;
                    case 3:
                        DeletionDate = DateTimeNow().AddDays(3);
                        break;
                    case 4:
                    case 6:
                        DeletionDate = DateTimeNow().AddDays(7);
                        break;
                    case 5:
                        DeletionDate = DateTimeNow().AddDays(30);
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
                        ExpirationDate = DateTimeNow().AddHours(1);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 2:
                        ExpirationDate = DateTimeNow().AddDays(1);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 3:
                        ExpirationDate = DateTimeNow().AddDays(2);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 4:
                        ExpirationDate = DateTimeNow().AddDays(3);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 5:
                        ExpirationDate = DateTimeNow().AddDays(7);
                        ExpirationTime = ExpirationDate.Value.TimeOfDay;
                        break;
                    case 6:
                        ExpirationDate = DateTimeNow().AddDays(30);
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
                ExpirationTime = DateTimeNow().TimeOfDay;
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

        private DateTime DateTimeNow()
        {
            var dtn = DateTime.Now;
            return new DateTime(
                dtn.Year,
                dtn.Month,
                dtn.Day,
                dtn.Hour,
                dtn.Minute,
                0,
                DateTimeKind.Local
            );
        }
    }
}
