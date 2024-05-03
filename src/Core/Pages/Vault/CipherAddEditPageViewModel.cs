using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Lists.ItemViewModels.CustomFields;
using Bit.App.Models;
using Bit.Core.Resources.Localization;
using Bit.Core;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

using Microsoft.Maui.Controls;
using Microsoft.Maui;
using Bit.App.Utilities;
using CommunityToolkit.Mvvm.Input;
using Bit.Core.Utilities.Fido2;
using Bit.Core.Services;

#nullable enable

namespace Bit.App.Pages
{
    public class CipherAddEditPageViewModel : BaseCipherViewModel
    {
        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly ICollectionService _collectionService;
        private readonly IStateService _stateService;
        private readonly IOrganizationService _organizationService;
        private readonly IMessagingService _messagingService;
        private readonly IEventService _eventService;
        private readonly IPolicyService _policyService;
        private readonly ICustomFieldItemFactory _customFieldItemFactory;
        private readonly IClipboardService _clipboardService;
        private readonly IAutofillHandler _autofillHandler;
        private readonly IWatchDeviceService _watchDeviceService;
        private readonly IAccountsManager _accountsManager;
        private readonly IFido2MakeCredentialConfirmationUserInterface _fido2MakeCredentialConfirmationUserInterface;
        private readonly IUserVerificationMediatorService _userVerificationMediatorService;
        
        private bool _showNotesSeparator;
        private bool _showPassword;
        private bool _showCardNumber;
        private bool _showCardCode;
        private int _typeSelectedIndex;
        private int _cardBrandSelectedIndex;
        private int _cardExpMonthSelectedIndex;
        private int _identityTitleSelectedIndex;
        private int _folderSelectedIndex;
        private int _ownershipSelectedIndex;
        private bool _hasCollections;
        private string _previousCipherId;
        private List<Core.Models.View.CollectionView> _writeableCollections;
        private bool _fromOtp;

        protected override string[] AdditionalPropertiesToRaiseOnCipherChanged => new string[]
        {
            nameof(IsLogin),
            nameof(IsIdentity),
            nameof(IsCard),
            nameof(IsSecureNote),
            nameof(ShowUris),
            nameof(ShowAttachments),
            nameof(ShowCollections),
            nameof(HasTotpValue)
        };

        private List<KeyValuePair<UriMatchType?, string>> _matchDetectionOptions =
            new List<KeyValuePair<UriMatchType?, string>>
            {
                new KeyValuePair<UriMatchType?, string>(null, AppResources.Default),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Domain, AppResources.BaseDomain),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Host, AppResources.Host),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.StartsWith, AppResources.StartsWith),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.RegularExpression, AppResources.RegEx),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Exact, AppResources.Exact),
                new KeyValuePair<UriMatchType?, string>(UriMatchType.Never, AppResources.Never)
            };

        public CipherAddEditPageViewModel()
        {
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _organizationService = ServiceContainer.Resolve<IOrganizationService>("organizationService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _collectionService = ServiceContainer.Resolve<ICollectionService>("collectionService");
            _eventService = ServiceContainer.Resolve<IEventService>("eventService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _customFieldItemFactory = ServiceContainer.Resolve<ICustomFieldItemFactory>("customFieldItemFactory");
            _clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            _watchDeviceService = ServiceContainer.Resolve<IWatchDeviceService>();
            _accountsManager = ServiceContainer.Resolve<IAccountsManager>();
            if (ServiceContainer.TryResolve<IFido2MakeCredentialConfirmationUserInterface>(out var fido2MakeService))
            {
                _fido2MakeCredentialConfirmationUserInterface = fido2MakeService;
            }
            _userVerificationMediatorService = ServiceContainer.Resolve<IUserVerificationMediatorService>(); 

            GeneratePasswordCommand = new Command(GeneratePassword);
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleCardNumberCommand = new Command(ToggleCardNumber);
            ToggleCardCodeCommand = new Command(ToggleCardCode);
            UriOptionsCommand = new Command<LoginUriView>(UriOptions);
            FieldOptionsCommand = new Command<ICustomFieldItemViewModel>(FieldOptions);
            PasswordPromptHelpCommand = new Command(PasswordPromptHelp);
            CopyCommand = CreateDefaultAsyncRelayCommand(CopyTotpClipboardAsync, onException: ex => _logger.Exception(ex), allowsMultipleExecutions: false);
            GenerateUsernameCommand = CreateDefaultAsyncRelayCommand(GenerateUsernameAsync, onException: ex => OnGenerateUsernameException(ex), allowsMultipleExecutions: false);
            Uris = new ExtendedObservableCollection<LoginUriView>();
            Fields = new ExtendedObservableCollection<ICustomFieldItemViewModel>();
            Collections = new ExtendedObservableCollection<CollectionViewModel>();
            AllowPersonal = true;

            TypeOptions = new List<KeyValuePair<string, CipherType>>
            {
                new KeyValuePair<string, CipherType>(AppResources.TypeLogin, CipherType.Login),
                new KeyValuePair<string, CipherType>(AppResources.TypeCard, CipherType.Card),
                new KeyValuePair<string, CipherType>(AppResources.TypeIdentity, CipherType.Identity),
                new KeyValuePair<string, CipherType>(AppResources.TypeSecureNote, CipherType.SecureNote),
            };
            CardBrandOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>($"-- {AppResources.Select} --", null),
                new KeyValuePair<string, string>("Visa", "Visa"),
                new KeyValuePair<string, string>("Mastercard", "Mastercard"),
                new KeyValuePair<string, string>("American Express", "Amex"),
                new KeyValuePair<string, string>("Discover", "Discover"),
                new KeyValuePair<string, string>("Diners Club", "Diners Club"),
                new KeyValuePair<string, string>("JCB", "JCB"),
                new KeyValuePair<string, string>("Maestro", "Maestro"),
                new KeyValuePair<string, string>("UnionPay", "UnionPay"),
                new KeyValuePair<string, string>("RuPay", "RuPay"),
                new KeyValuePair<string, string>(AppResources.Other, "Other")
            };
            CardExpMonthOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>($"-- {AppResources.Select} --", null),
                new KeyValuePair<string, string>($"01 - {AppResources.January}", "1"),
                new KeyValuePair<string, string>($"02 - {AppResources.February}", "2"),
                new KeyValuePair<string, string>($"03 - {AppResources.March}", "3"),
                new KeyValuePair<string, string>($"04 - {AppResources.April}", "4"),
                new KeyValuePair<string, string>($"05 - {AppResources.May}", "5"),
                new KeyValuePair<string, string>($"06 - {AppResources.June}", "6"),
                new KeyValuePair<string, string>($"07 - {AppResources.July}", "7"),
                new KeyValuePair<string, string>($"08 - {AppResources.August}", "8"),
                new KeyValuePair<string, string>($"09 - {AppResources.September}", "9"),
                new KeyValuePair<string, string>($"10 - {AppResources.October}", "10"),
                new KeyValuePair<string, string>($"11 - {AppResources.November}", "11"),
                new KeyValuePair<string, string>($"12 - {AppResources.December}", "12")
            };
            IdentityTitleOptions = new List<KeyValuePair<string, string>>
            {
                new KeyValuePair<string, string>($"-- {AppResources.Select} --", null),
                new KeyValuePair<string, string>(AppResources.Mr, AppResources.Mr),
                new KeyValuePair<string, string>(AppResources.Mrs, AppResources.Mrs),
                new KeyValuePair<string, string>(AppResources.Ms, AppResources.Ms),
                new KeyValuePair<string, string>(AppResources.Mx, AppResources.Mx),
                new KeyValuePair<string, string>(AppResources.Dr, AppResources.Dr),
            };
            FolderOptions = new List<KeyValuePair<string, string>>();
            OwnershipOptions = new List<KeyValuePair<string, string>>();
        }

        public Command GeneratePasswordCommand { get; set; }
        public Command TogglePasswordCommand { get; set; }
        public Command ToggleCardNumberCommand { get; set; }
        public Command ToggleCardCodeCommand { get; set; }
        public Command UriOptionsCommand { get; set; }
        public Command FieldOptionsCommand { get; set; }
        public Command PasswordPromptHelpCommand { get; set; }
        public AsyncRelayCommand CopyCommand { get; set; }
        public AsyncRelayCommand GenerateUsernameCommand { get; set; }
        public string CipherId { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public CipherType? Type { get; set; }
        public HashSet<string> CollectionIds { get; set; }
        public string DefaultName { get; set; }
        public string DefaultUri { get; set; }
        public List<KeyValuePair<string, CipherType>> TypeOptions { get; set; }
        public List<KeyValuePair<string, string>> CardBrandOptions { get; set; }
        public List<KeyValuePair<string, string>> CardExpMonthOptions { get; set; }
        public List<KeyValuePair<string, string>> IdentityTitleOptions { get; set; }
        public List<KeyValuePair<string, string>> FolderOptions { get; set; }
        public List<KeyValuePair<string, string>> OwnershipOptions { get; set; }
        public ExtendedObservableCollection<LoginUriView> Uris { get; set; }
        public ExtendedObservableCollection<ICustomFieldItemViewModel> Fields { get; set; }
        public ExtendedObservableCollection<CollectionViewModel> Collections { get; set; }

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
        public int CardBrandSelectedIndex
        {
            get => _cardBrandSelectedIndex;
            set
            {
                if (SetProperty(ref _cardBrandSelectedIndex, value))
                {
                    CardBrandChanged();
                }
            }
        }
        public int CardExpMonthSelectedIndex
        {
            get => _cardExpMonthSelectedIndex;
            set
            {
                if (SetProperty(ref _cardExpMonthSelectedIndex, value))
                {
                    CardExpMonthChanged();
                }
            }
        }
        public int IdentityTitleSelectedIndex
        {
            get => _identityTitleSelectedIndex;
            set
            {
                if (SetProperty(ref _identityTitleSelectedIndex, value))
                {
                    IdentityTitleChanged();
                }
            }
        }
        public int FolderSelectedIndex
        {
            get => _folderSelectedIndex;
            set
            {
                if (SetProperty(ref _folderSelectedIndex, value))
                {
                    FolderChanged();
                }
            }
        }
        public int OwnershipSelectedIndex
        {
            get => _ownershipSelectedIndex;
            set
            {
                if (SetProperty(ref _ownershipSelectedIndex, value))
                {
                    OrganizationChanged();
                }
            }
        }
        public bool ShowNotesSeparator
        {
            get => _showNotesSeparator;
            set => SetProperty(ref _showNotesSeparator, value);
        }
        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon),
                    nameof(PasswordVisibilityAccessibilityText)
                });
        }
        public bool ShowCardNumber
        {
            get => _showCardNumber;
            set => SetProperty(ref _showCardNumber, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowCardNumberIcon)
                });
        }
        public bool ShowCardCode
        {
            get => _showCardCode;
            set => SetProperty(ref _showCardCode, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowCardCodeIcon)
                });
        }
        public bool HasCollections
        {
            get => _hasCollections;
            set => SetProperty(ref _hasCollections, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowCollections)
                });
        }
        public bool ShowCollections => (!EditMode || CloneMode) && Cipher?.OrganizationId != null;
        public bool IsFromFido2Framework { get; set; }
        public bool EditMode => !string.IsNullOrWhiteSpace(CipherId);
        public bool TypeEditMode => !string.IsNullOrWhiteSpace(CipherId) || IsFromFido2Framework;
        public bool ShowOwnershipOptions => !EditMode || CloneMode;
        public bool OwnershipPolicyInEffect => ShowOwnershipOptions && !AllowPersonal;
        public bool CloneMode { get; set; }
        public CipherDetailsPage CipherDetailsPage { get; set; }
        public bool IsLogin => Cipher?.Type == CipherType.Login;
        public bool IsIdentity => Cipher?.Type == CipherType.Identity;
        public bool IsCard => Cipher?.Type == CipherType.Card;
        public bool IsSecureNote => Cipher?.Type == CipherType.SecureNote;
        public bool ShowUris => IsLogin && Cipher != null && Cipher.Login.HasUris;
        public bool ShowAttachments => Cipher != null && Cipher.HasAttachments;
        public string ShowPasswordIcon => ShowPassword ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public string ShowCardNumberIcon => ShowCardNumber ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public string ShowCardCodeIcon => ShowCardCode ? BitwardenIcons.EyeSlash : BitwardenIcons.Eye;
        public int PasswordFieldColSpan => Cipher != null && Cipher.ViewPassword ? 1 : 4;
        public int TotpColumnSpan => Cipher != null && Cipher.ViewPassword ? 1 : 2;
        public bool AllowPersonal { get; set; }
        public bool PasswordPrompt => Cipher != null && Cipher.Reprompt != CipherRepromptType.None;
        public string PasswordVisibilityAccessibilityText => ShowPassword ? AppResources.PasswordIsVisibleTapToHide : AppResources.PasswordIsNotVisibleTapToShow;
        public bool HasTotpValue => IsLogin && !string.IsNullOrEmpty(Cipher?.Login?.Totp);
        public bool AllowTotpCopy => HasTotpValue && Cipher.ViewPassword;
        public string SetupTotpText => $"{BitwardenIcons.Camera} {AppResources.SetupTotp}";
        public bool ShowPasskeyInfo => Cipher?.HasFido2Credential == true && !CloneMode;

        public void Init()
        {
            PageTitle = EditMode && !CloneMode ? AppResources.EditItem : AppResources.AddItem;
        }

        public async Task<bool> LoadAsync(AppOptions appOptions = null)
        {
            _fromOtp = appOptions?.OtpData != null;
            IsFromFido2Framework = _fido2MakeCredentialConfirmationUserInterface?.IsConfirmingNewCredential == true;

            var myEmail = await _stateService.GetEmailAsync();
            OwnershipOptions.Add(new KeyValuePair<string, string>(myEmail, null));
            var orgs = await _organizationService.GetAllAsync();
            foreach (var org in orgs.OrderBy(o => o.Name))
            {
                if (org.Enabled && org.Status == OrganizationUserStatusType.Confirmed)
                {
                    OwnershipOptions.Add(new KeyValuePair<string, string>(org.Name, org.Id));
                }
            }

            var personalOwnershipPolicyApplies = await _policyService.PolicyAppliesToUser(PolicyType.PersonalOwnership);
            if (personalOwnershipPolicyApplies && (!EditMode || CloneMode))
            {
                AllowPersonal = false;
                // Remove personal ownership
                OwnershipOptions.RemoveAt(0);
            }

            var allCollections = await _collectionService.GetAllDecryptedAsync();
            _writeableCollections = allCollections.Where(c => !c.ReadOnly).ToList();
            if (CollectionIds?.Any() ?? false)
            {
                var colId = CollectionIds.First();
                var collection = _writeableCollections.FirstOrDefault(c => c.Id == colId);
                OrganizationId = collection?.OrganizationId;
            }
            var folders = await _folderService.GetAllDecryptedAsync();
            FolderOptions = folders.Select(f => new KeyValuePair<string, string>(f.Name, f.Id)).ToList();

            if (Cipher == null)
            {
                if (EditMode)
                {
                    var cipher = await _cipherService.GetAsync(CipherId);
                    if (cipher == null)
                    {
                        return false;
                    }
                    Cipher = await cipher.DecryptAsync();
                    if (CloneMode)
                    {
                        Cipher.Name += " - " + AppResources.Clone;
                        // If not allowing personal ownership, update cipher's org Id to prompt downstream changes
                        if (Cipher.OrganizationId == null && !AllowPersonal)
                        {
                            Cipher.OrganizationId = OrganizationId;
                        }
                        if (Cipher.Type == CipherType.Login)
                        {
                            // passkeys can't be cloned
                            Cipher.Login.Fido2Credentials = null;
                        }
                    }
                    if (appOptions?.OtpData != null && Cipher.Type == CipherType.Login)
                    {
                        Cipher.Login.Totp = appOptions.OtpData.Value.Uri;
                    }
                }
                else
                {
                    Cipher = new CipherView
                    {
                        Name = DefaultName,
                        OrganizationId = OrganizationId,
                        FolderId = FolderId,
                        Type = Type.GetValueOrDefault(CipherType.Login),
                        Login = new LoginView(),
                        Card = new CardView(),
                        Identity = new IdentityView(),
                        SecureNote = new SecureNoteView()
                    };
                    Cipher.Login.Uris = new List<LoginUriView> { new LoginUriView { Uri = DefaultUri } };
                    Cipher.SecureNote.Type = SecureNoteType.Generic;

                    if (appOptions != null)
                    {
                        Cipher.Type = appOptions.SaveType.GetValueOrDefault(Cipher.Type);
                        Cipher.Login.Username = appOptions.SaveUsername;
                        Cipher.Login.Password = appOptions.SavePassword;
                        Cipher.Login.Totp = appOptions.OtpData?.Uri;
                        Cipher.Card.Code = appOptions.SaveCardCode;
                        if (int.TryParse(appOptions.SaveCardExpMonth, out int month) && month <= 12 && month >= 1)
                        {
                            Cipher.Card.ExpMonth = month.ToString();
                        }
                        Cipher.Card.ExpYear = appOptions.SaveCardExpYear;
                        Cipher.Card.CardholderName = appOptions.SaveCardName;
                        Cipher.Card.Number = appOptions.SaveCardNumber;
                    }
                }

                TypeSelectedIndex = TypeOptions.FindIndex(k => k.Value == Cipher.Type);
                FolderSelectedIndex = string.IsNullOrWhiteSpace(Cipher.FolderId) ? FolderOptions.Count - 1 :
                    FolderOptions.FindIndex(k => k.Value == Cipher.FolderId);
                CardBrandSelectedIndex = string.IsNullOrWhiteSpace(Cipher.Card?.Brand) ? 0 :
                    CardBrandOptions.FindIndex(k => k.Value == Cipher.Card.Brand);
                CardExpMonthSelectedIndex = string.IsNullOrWhiteSpace(Cipher.Card?.ExpMonth) ? 0 :
                    CardExpMonthOptions.FindIndex(k => k.Value == Cipher.Card.ExpMonth);
                IdentityTitleSelectedIndex = string.IsNullOrWhiteSpace(Cipher.Identity?.Title) ? 0 :
                    IdentityTitleOptions.FindIndex(k => k.Value == Cipher.Identity.Title);
                OwnershipSelectedIndex = string.IsNullOrWhiteSpace(Cipher.OrganizationId) ? 0 :
                    OwnershipOptions.FindIndex(k => k.Value == Cipher.OrganizationId);

                // If the selected organization is on Index 0 and we've removed the personal option, force refresh
                if (!AllowPersonal && OwnershipSelectedIndex == 0)
                {
                    OrganizationChanged();
                }

                if ((!EditMode || CloneMode) && (CollectionIds?.Any() ?? false))
                {
                    foreach (var col in Collections)
                    {
                        col.Checked = CollectionIds.Contains(col.Collection.Id);
                    }
                }
                if (Cipher.Login?.Uris != null)
                {
                    Uris.ResetWithRange(Cipher.Login.Uris);
                }
                if (Cipher.Fields != null)
                {
                    Fields.ResetWithRange(Cipher.Fields?.Select(f => _customFieldItemFactory.CreateCustomFieldItem(f, true, Cipher, null, null, FieldOptionsCommand)));
                }

                if (appOptions?.OtpData != null)
                {
                    _platformUtilsService.ShowToast(null, AppResources.AuthenticatorKey, AppResources.AuthenticatorKeyAdded);
                }
            }

            if (EditMode && _previousCipherId != CipherId)
            {
                var task = _eventService.CollectAsync(EventType.Cipher_ClientViewed, CipherId);
            }
            _previousCipherId = CipherId;

            MainThread.BeginInvokeOnMainThread(() =>
            {
                TriggerPropertyChanged(nameof(OwnershipOptions));
                TriggerPropertyChanged(nameof(OwnershipSelectedIndex));
            });

            return true;
        }

        public async Task<bool> SubmitAsync()
        {
            if (Cipher == null)
            {
                return false;
            }
            if (Microsoft.Maui.Networking.Connectivity.NetworkAccess == Microsoft.Maui.Networking.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            if (string.IsNullOrWhiteSpace(Cipher.Name))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.Name),
                    AppResources.Ok);
                return false;
            }

            if ((!EditMode || CloneMode) && !AllowPersonal && string.IsNullOrWhiteSpace(Cipher.OrganizationId))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    AppResources.PersonalOwnershipSubmitError, AppResources.Ok);
                return false;
            }

            Cipher.Fields = Fields != null && Fields.Any() ?
                Fields.Where(f => f != null).Select(f => f.Field).ToList() : null;
            if (Cipher.Login != null)
            {
                Cipher.Login.Uris = Uris?.ToList();
                if ((!EditMode || CloneMode) && Cipher.Type == CipherType.Login && Cipher.Login.Uris != null &&
                   Cipher.Login.Uris.Count == 1 && string.IsNullOrWhiteSpace(Cipher.Login.Uris[0].Uri))
                {
                    Cipher.Login.Uris = null;
                }
            }

            if ((!EditMode || CloneMode) && Cipher.OrganizationId != null)
            {
                if (Collections == null || !Collections.Any(c => c != null && c.Checked))
                {
                    await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.SelectOneCollection,
                        AppResources.Ok);
                    return false;
                }

                Cipher.CollectionIds = Collections.Any() ?
                    new HashSet<string>(Collections.Where(c => c != null && c.Checked && c.Collection?.Id != null)
                        .Select(c => c.Collection.Id)) : null;
            }

            if (CloneMode)
            {
                Cipher.Id = null;
            }
            var cipher = await _cipherService.EncryptAsync(Cipher);
            if (cipher == null)
            {
                return false;
            }
            try
            {
                bool isFido2UserVerified = false;
                if (IsFromFido2Framework)
                {
                    // Verify the user and prevent saving cipher if enforcing is needed and it's not verified.
                    var userVerification = await VerifyUserAsync();
                    if (userVerification.IsCancelled)
                    {
                        return false;
                    }
                    isFido2UserVerified = userVerification.Result;

                    var options = _fido2MakeCredentialConfirmationUserInterface.GetCurrentUserVerificationOptions();

                    if (!isFido2UserVerified && await _userVerificationMediatorService.ShouldEnforceFido2RequiredUserVerificationAsync(options.Value))
                    {
                        await _platformUtilsService.ShowDialogAsync(AppResources.ErrorCreatingPasskey, AppResources.SavePasskey);
                        return false;
                    }
                }

                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);

                await _cipherService.SaveWithServerAsync(cipher);
                Cipher.Id = cipher.Id;

                await _deviceActionService.HideLoadingAsync();

                _platformUtilsService.ShowToast("success", null,
                    EditMode && !CloneMode ? AppResources.ItemUpdated : AppResources.NewItemCreated);
                _messagingService.Send(EditMode && !CloneMode ? "editedCipher" : "addedCipher", Cipher.Id);

                _watchDeviceService.SyncDataToWatchAsync().FireAndForget();

                if (Page is CipherAddEditPage page && page.FromAutofillFramework)
                {
                    // Close and go back to app
                    _autofillHandler.CloseAutofill();
                }
                else if (IsFromFido2Framework)
                {
                    _fido2MakeCredentialConfirmationUserInterface.Confirm(cipher.Id, isFido2UserVerified);
                    return true;
                }
                else if (_fromOtp)
                {
                    await _accountsManager.StartDefaultNavigationFlowAsync(op => op.OtpData = null);
                }
                else
                {
                    if (CloneMode)
                    {
                        CipherDetailsPage?.UpdateCipherId(this.Cipher.Id);
                    }
                    // if the app is tombstoned then PopModalAsync would throw index out of bounds
                    if (Page.Navigation?.ModalStack?.Count > 0)
                    {
                        await Page.Navigation.PopModalAsync();
                    }
                }
                return true;
            }
            catch (ApiException apiEx)
            {
                await _deviceActionService.HideLoadingAsync();
                if (apiEx?.Error != null)
                {
                    await _platformUtilsService.ShowDialogAsync(apiEx.Error.GetSingleMessage(),
                        AppResources.AnErrorHasOccurred);
                }
            }
            catch (Exception genex)
            {
                _logger.Exception(genex);
                await _deviceActionService.HideLoadingAsync();
            }
            return false;
        }

        private async Task<CancellableResult<bool>> VerifyUserAsync()
        {
            try
            {
                var options = _fido2MakeCredentialConfirmationUserInterface.GetCurrentUserVerificationOptions();
                ArgumentNullException.ThrowIfNull(options);

                if (options.Value.UserVerificationPreference == Fido2UserVerificationPreference.Discouraged)
                {
                    return new CancellableResult<bool>(false);
                }

                return await _userVerificationMediatorService.VerifyUserForFido2Async(options.Value);
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
                return new CancellableResult<bool>(false);
            }
        }

        public async Task<bool> DeleteAsync()
        {
            if (Microsoft.Maui.Networking.Connectivity.NetworkAccess == Microsoft.Maui.Networking.NetworkAccess.None)
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.InternetConnectionRequiredMessage,
                    AppResources.InternetConnectionRequiredTitle);
                return false;
            }
            var confirmed = await _platformUtilsService.ShowDialogAsync(
                AppResources.DoYouReallyWantToSoftDeleteCipher,
                null, AppResources.Yes, AppResources.Cancel);
            if (!confirmed)
            {
                return false;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.SoftDeleting);
                await _cipherService.SoftDeleteWithServerAsync(Cipher.Id);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.ItemSoftDeleted);
                _messagingService.Send("softDeletedCipher", Cipher);
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

        public async void GeneratePassword()
        {
            if (!string.IsNullOrWhiteSpace(Cipher?.Login?.Password))
            {
                var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.PasswordOverrideAlert,
                    null, AppResources.Yes, AppResources.No);
                if (!confirmed)
                {
                    return;
                }
            }
            var page = new GeneratorPage(false, async (password) =>
            {
                Cipher.Login.Password = password;
                TriggerCipherChanged();
                await Page.Navigation.PopModalAsync();
            });
            await Page.Navigation.PushModalAsync(new NavigationPage(page));
        }

        public async Task GenerateUsernameAsync()
        {
            if (!string.IsNullOrWhiteSpace(Cipher?.Login?.Username)
                && !await _platformUtilsService.ShowDialogAsync(AppResources.AreYouSureYouWantToOverwriteTheCurrentUsername, null, AppResources.Yes, AppResources.No))
            {
                return;
            }

            var website = Cipher?.Login?.Uris?.FirstOrDefault()?.Host;

            var page = new GeneratorPage(false, async (username) =>
            {
                try
                {
                    Cipher.Login.Username = username;
                    TriggerCipherChanged();
                    await Page.Navigation.PopModalAsync();
                }
                catch (Exception ex)
                {
                    OnGenerateUsernameException(ex);
                }
            }, isUsernameGenerator: true, emailWebsite: website, editMode: true);
            await Page.Navigation.PushModalAsync(new NavigationPage(page));
        }

        public async void UriOptions(LoginUriView uri)
        {
            if (!(Page as CipherAddEditPage).DoOnce())
            {
                return;
            }
            var selection = await Page.DisplayActionSheet(AppResources.Options, AppResources.Cancel, null,
                AppResources.MatchDetection, AppResources.Remove);
            if (selection == AppResources.Remove)
            {
                Uris.Remove(uri);
            }
            else if (selection == AppResources.MatchDetection)
            {
                var options = _matchDetectionOptions.Select(o => o.Key == uri.Match ? $"✓ {o.Value}" : o.Value);
                var matchSelection = await Page.DisplayActionSheet(AppResources.URIMatchDetection,
                    AppResources.Cancel, null, options.ToArray());
                if (matchSelection != null && matchSelection != AppResources.Cancel)
                {
                    var matchSelectionClean = matchSelection.Replace("✓ ", string.Empty);
                    uri.Match = _matchDetectionOptions.FirstOrDefault(o => o.Value == matchSelectionClean).Key;
                }
            }
        }

        public void AddUri()
        {
            if (Cipher.Type != CipherType.Login)
            {
                return;
            }
            if (Uris == null)
            {
                Uris = new ExtendedObservableCollection<LoginUriView>();
            }
            Uris.Add(new LoginUriView());
        }

        public async void FieldOptions(ICustomFieldItemViewModel field)
        {
            if (!(Page as CipherAddEditPage).DoOnce())
            {
                return;
            }
            var selection = await Page.DisplayActionSheet(AppResources.Options, AppResources.Cancel, null,
                AppResources.Edit, AppResources.MoveUp, AppResources.MoveDown, AppResources.Remove);
            if (selection == AppResources.Remove)
            {
                Fields.Remove(field);
            }
            else if (selection == AppResources.Edit)
            {
                var name = await _deviceActionService.DisplayPromptAync(AppResources.CustomFieldName,
                    null, field.Field.Name);
                field.Field.Name = name ?? field.Field.Name;
                field.TriggerFieldChanged();
            }
            else if (selection == AppResources.MoveUp)
            {
                var currentIndex = Fields.IndexOf(field);
                if (currentIndex > 0)
                {
                    Fields.Move(currentIndex, currentIndex - 1);
                }
            }
            else if (selection == AppResources.MoveDown)
            {
                var currentIndex = Fields.IndexOf(field);
                if (currentIndex < Fields.Count - 1)
                {
                    Fields.Move(currentIndex, currentIndex + 1);
                }
            }
        }

        public async void AddField()
        {
            var fieldTypeOptions = new List<KeyValuePair<FieldType, string>>
            {
                new KeyValuePair<FieldType, string>(FieldType.Text, AppResources.FieldTypeText),
                new KeyValuePair<FieldType, string>(FieldType.Hidden, AppResources.FieldTypeHidden),
                new KeyValuePair<FieldType, string>(FieldType.Boolean, AppResources.FieldTypeBoolean),
            };

            if (Cipher.LinkedFieldOptions != null)
            {
                fieldTypeOptions.Add(new KeyValuePair<FieldType, string>(FieldType.Linked, AppResources.FieldTypeLinked));
            }

            var typeSelection = await Page.DisplayActionSheet(AppResources.SelectTypeField, AppResources.Cancel, null,
                fieldTypeOptions.Select(f => f.Value).ToArray());
            if (typeSelection != null && typeSelection != AppResources.Cancel)
            {
                var name = await _deviceActionService.DisplayPromptAync(AppResources.CustomFieldName);
                if (name == null)
                {
                    return;
                }
                if (Fields == null)
                {
                    Fields = new ExtendedObservableCollection<ICustomFieldItemViewModel>();
                }
                var type = fieldTypeOptions.FirstOrDefault(f => f.Value == typeSelection).Key;
                Fields.Add(_customFieldItemFactory.CreateCustomFieldItem(new FieldView
                {
                    Type = type,
                    Name = string.IsNullOrWhiteSpace(name) ? null : name,
                    NewField = true,
                }, true, Cipher, null, null, FieldOptionsCommand));
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
            if (EditMode && ShowPassword)
            {
                var task = _eventService.CollectAsync(EventType.Cipher_ClientToggledPasswordVisible, CipherId);
            }
        }

        public void ToggleCardNumber()
        {
            ShowCardNumber = !ShowCardNumber;
            if (EditMode && ShowCardNumber)
            {
                var task = _eventService.CollectAsync(
                    Core.Enums.EventType.Cipher_ClientToggledCardNumberVisible, CipherId);
            }
        }

        public void ToggleCardCode()
        {
            ShowCardCode = !ShowCardCode;
            if (EditMode && ShowCardCode)
            {
                var task = _eventService.CollectAsync(EventType.Cipher_ClientToggledCardCodeVisible, CipherId);
            }
        }

        public async Task UpdateTotpKeyAsync(string key)
        {
            if (Cipher?.Login != null)
            {
                if (!string.IsNullOrWhiteSpace(key))
                {
                    Cipher.Login.Totp = key;
                    TriggerCipherChanged();
                    _platformUtilsService.ShowToast("info", null, AppResources.AuthenticatorKeyAdded);
                }
                else
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.AuthenticatorKeyReadError);
                }
            }
        }

        public void PasswordPromptHelp()
        {
            _platformUtilsService.LaunchUri("https://bitwarden.com/help/managing-items/#protect-individual-items");
        }

        private void TypeChanged()
        {
            if (Cipher != null && TypeSelectedIndex > -1)
            {
                Cipher.Type = TypeOptions[TypeSelectedIndex].Value;
                TriggerCipherChanged();

                // Linked Custom Fields only apply to a specific item type
                foreach (var field in Fields.OfType<LinkedCustomFieldItemViewModel>().ToList())
                {
                    Fields.Remove(field);
                }
            }
        }

        private void CardBrandChanged()
        {
            if (Cipher?.Card != null && CardBrandSelectedIndex > -1)
            {
                Cipher.Card.Brand = CardBrandOptions[CardBrandSelectedIndex].Value;
            }
        }

        private void CardExpMonthChanged()
        {
            if (Cipher?.Card != null && CardExpMonthSelectedIndex > -1)
            {
                Cipher.Card.ExpMonth = CardExpMonthOptions[CardExpMonthSelectedIndex].Value;
            }
        }

        private void IdentityTitleChanged()
        {
            if (Cipher?.Identity != null && IdentityTitleSelectedIndex > -1)
            {
                Cipher.Identity.Title = IdentityTitleOptions[IdentityTitleSelectedIndex].Value;
            }
        }

        private void FolderChanged()
        {
            if (Cipher != null && FolderSelectedIndex > -1)
            {
                Cipher.FolderId = FolderOptions[FolderSelectedIndex].Value;
            }
        }

        private void OrganizationChanged()
        {
            if (Cipher != null && OwnershipSelectedIndex > -1)
            {
                Cipher.OrganizationId = OwnershipOptions[OwnershipSelectedIndex].Value;
                TriggerCipherChanged();
            }
            if (Cipher?.OrganizationId != null)
            {
                var cols = _writeableCollections.Where(c => c.OrganizationId == Cipher.OrganizationId)
                    .Select(c => new CollectionViewModel { Collection = c }).ToList();
                Collections.ResetWithRange(cols);
            }
            else
            {
                Collections.ResetWithRange(new List<CollectionViewModel>());
            }
            HasCollections = Collections.Any();
        }

        private void TriggerCipherChanged()
        {
            MainThread.BeginInvokeOnMainThread(() => TriggerPropertyChanged(nameof(Cipher), AdditionalPropertiesToRaiseOnCipherChanged));
        }

        private async Task CopyTotpClipboardAsync()
        {
            try
            {
                await _clipboardService.CopyTextAsync(Cipher.Login.Totp);
                _platformUtilsService.ShowToast("info", null, string.Format(AppResources.ValueHasBeenCopied, AppResources.AuthenticatorKeyScanner));
            }
            catch (Exception ex)
            {
                _logger.Exception(ex);
            }
        }

        private async void OnGenerateUsernameException(Exception ex)
        {
            _logger.Exception(ex);
            await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok);
        }
    }
}
