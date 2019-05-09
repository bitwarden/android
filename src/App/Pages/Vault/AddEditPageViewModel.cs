using Bit.App.Abstractions;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class AddEditPageViewModel : BaseViewModel
    {
        private readonly IDeviceActionService _deviceActionService;
        private readonly ICipherService _cipherService;
        private readonly IFolderService _folderService;
        private readonly IUserService _userService;
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly IAuditService _auditService;
        private readonly IMessagingService _messagingService;
        private CipherView _cipher;
        private bool _showPassword;
        private bool _showCardCode;
        private int _typeSelectedIndex;
        private int _cardBrandSelectedIndex;
        private int _cardExpMonthSelectedIndex;
        private int _identityTitleSelectedIndex;
        private int _folderSelectedIndex;
        private string[] _additionalCipherProperties = new string[]
        {
            nameof(IsLogin),
            nameof(IsIdentity),
            nameof(IsCard),
            nameof(IsSecureNote),
            nameof(ShowUris),
            nameof(ShowAttachments),
            nameof(ShowIdentityAddress),
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
        private List<KeyValuePair<FieldType, string>> _fieldTypeOptions =
            new List<KeyValuePair<FieldType, string>>
            {
                new KeyValuePair<FieldType, string>(FieldType.Text, AppResources.FieldTypeText),
                new KeyValuePair<FieldType, string>(FieldType.Hidden, AppResources.FieldTypeHidden),
                new KeyValuePair<FieldType, string>(FieldType.Boolean, AppResources.FieldTypeBoolean)
            };

        public AddEditPageViewModel()
        {
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _cipherService = ServiceContainer.Resolve<ICipherService>("cipherService");
            _folderService = ServiceContainer.Resolve<IFolderService>("folderService");
            _userService = ServiceContainer.Resolve<IUserService>("userService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _auditService = ServiceContainer.Resolve<IAuditService>("auditService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            GeneratePasswordCommand = new Command(GeneratePassword);
            TogglePasswordCommand = new Command(TogglePassword);
            ToggleCardCodeCommand = new Command(ToggleCardCode);
            CheckPasswordCommand = new Command(CheckPasswordAsync);
            UriOptionsCommand = new Command<LoginUriView>(UriOptions);
            FieldOptionsCommand = new Command<AddEditPageFieldViewModel>(FieldOptions);
            Uris = new ExtendedObservableCollection<LoginUriView>();
            Fields = new ExtendedObservableCollection<AddEditPageFieldViewModel>();

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
                new KeyValuePair<string, string>(AppResources.Dr, AppResources.Dr),
            };
            FolderOptions = new List<KeyValuePair<string, string>>();
        }

        public Command GeneratePasswordCommand { get; set; }
        public Command TogglePasswordCommand { get; set; }
        public Command ToggleCardCodeCommand { get; set; }
        public Command CheckPasswordCommand { get; set; }
        public Command UriOptionsCommand { get; set; }
        public Command FieldOptionsCommand { get; set; }
        public string CipherId { get; set; }
        public string OrganizationId { get; set; }
        public string FolderId { get; set; }
        public CipherType? Type { get; set; }
        public List<string> CollectionIds { get; set; }
        public List<KeyValuePair<string, CipherType>> TypeOptions { get; set; }
        public List<KeyValuePair<string, string>> CardBrandOptions { get; set; }
        public List<KeyValuePair<string, string>> CardExpMonthOptions { get; set; }
        public List<KeyValuePair<string, string>> IdentityTitleOptions { get; set; }
        public List<KeyValuePair<string, string>> FolderOptions { get; set; }
        public ExtendedObservableCollection<LoginUriView> Uris { get; set; }
        public ExtendedObservableCollection<AddEditPageFieldViewModel> Fields { get; set; }
        public int TypeSelectedIndex
        {
            get => _typeSelectedIndex;
            set
            {
                SetProperty(ref _typeSelectedIndex, value);
                TypeChanged();
            }
        }
        public int CardBrandSelectedIndex
        {
            get => _cardBrandSelectedIndex;
            set
            {
                SetProperty(ref _cardBrandSelectedIndex, value);
                CardBrandChanged();
            }
        }
        public int CardExpMonthSelectedIndex
        {
            get => _cardExpMonthSelectedIndex;
            set
            {
                SetProperty(ref _cardExpMonthSelectedIndex, value);
                CardExpMonthChanged();
            }
        }
        public int IdentityTitleSelectedIndex
        {
            get => _identityTitleSelectedIndex;
            set
            {
                SetProperty(ref _identityTitleSelectedIndex, value);
                IdentityTitleChanged();
            }
        }
        public int FolderSelectedIndex
        {
            get => _folderSelectedIndex;
            set
            {
                SetProperty(ref _folderSelectedIndex, value);
                FolderChanged();
            }
        }
        public CipherView Cipher
        {
            get => _cipher;
            set => SetProperty(ref _cipher, value, additionalPropertyNames: _additionalCipherProperties);
        }
        public bool ShowPassword
        {
            get => _showPassword;
            set => SetProperty(ref _showPassword, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowPasswordIcon)
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
        public bool EditMode => !string.IsNullOrWhiteSpace(CipherId);
        public bool IsLogin => Cipher?.Type == CipherType.Login;
        public bool IsIdentity => Cipher?.Type == CipherType.Identity;
        public bool IsCard => Cipher?.Type == CipherType.Card;
        public bool IsSecureNote => Cipher?.Type == CipherType.SecureNote;
        public bool ShowUris => IsLogin && Cipher.Login.HasUris;
        public bool ShowIdentityAddress => IsIdentity && (
            !string.IsNullOrWhiteSpace(Cipher.Identity.Address1) ||
            !string.IsNullOrWhiteSpace(Cipher.Identity.City) ||
            !string.IsNullOrWhiteSpace(Cipher.Identity.Country));
        public bool ShowAttachments => Cipher.HasAttachments;
        public string ShowPasswordIcon => ShowPassword ? "" : "";
        public string ShowCardCodeIcon => ShowCardCode ? "" : "";

        public void Init()
        {
            PageTitle = EditMode ? AppResources.EditItem : AppResources.AddItem;
        }

        public async Task LoadAsync()
        {
            // TODO: load collections
            var folders = await _folderService.GetAllDecryptedAsync();
            FolderOptions = folders.Select(f => new KeyValuePair<string, string>(f.Name, f.Id)).ToList();

            if(Cipher == null)
            {
                if(EditMode)
                {
                    var cipher = await _cipherService.GetAsync(CipherId);
                    Cipher = await cipher.DecryptAsync();

                    FolderSelectedIndex = string.IsNullOrWhiteSpace(Cipher.FolderId) ? FolderOptions.Count - 1 :
                        FolderOptions.FindIndex(k => k.Value == Cipher.FolderId); ;
                    if(Cipher.Card != null)
                    {
                        CardBrandSelectedIndex = string.IsNullOrWhiteSpace(Cipher.Card.Brand) ? 0 :
                            CardBrandOptions.FindIndex(k => k.Value == Cipher.Card.Brand);
                        CardExpMonthSelectedIndex = string.IsNullOrWhiteSpace(Cipher.Card.ExpMonth) ? 0 :
                            CardExpMonthOptions.FindIndex(k => k.Value == Cipher.Card.ExpMonth);
                    }
                    if(Cipher.Identity != null)
                    {
                        IdentityTitleSelectedIndex = string.IsNullOrWhiteSpace(Cipher.Identity.Title) ? 0 :
                            IdentityTitleOptions.FindIndex(k => k.Value == Cipher.Identity.Title);
                    }
                }
                else
                {
                    Cipher = new CipherView
                    {
                        OrganizationId = OrganizationId,
                        FolderId = FolderId,
                        Type = Type.GetValueOrDefault(CipherType.Login),
                        Login = new LoginView(),
                        Card = new CardView(),
                        Identity = new IdentityView(),
                        SecureNote = new SecureNoteView()
                    };
                    Cipher.Login.Uris = new List<LoginUriView> { new LoginUriView() };
                    Cipher.SecureNote.Type = SecureNoteType.Generic;

                    TypeSelectedIndex = TypeOptions.FindIndex(k => k.Value == Cipher.Type);
                    CardBrandSelectedIndex = 0;
                    CardExpMonthSelectedIndex = 0;
                    IdentityTitleSelectedIndex = 0;
                    FolderSelectedIndex = FolderOptions.Count - 1;
                    // TODO: org/collection stuff
                }

                if(Cipher.Login.Uris != null)
                {
                    Uris.ResetWithRange(Cipher.Login.Uris);
                }
                if(Cipher.Fields != null)
                {
                    Fields.ResetWithRange(Cipher.Fields?.Select(f => new AddEditPageFieldViewModel(f)));
                }
            }
        }

        public async Task<bool> SubmitAsync()
        {
            if(string.IsNullOrWhiteSpace(Cipher.Name))
            {
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred,
                    string.Format(AppResources.ValidationFieldRequired, AppResources.Name),
                    AppResources.Ok);
                return false;
            }

            Cipher.Fields = Fields.Any() ? Fields.Select(f => f.Field).ToList() : null;
            Cipher.Login.Uris = Uris.ToList();
            if(!EditMode && Cipher.Type == CipherType.Login && (Cipher.Login.Uris?.Count ?? 0) == 1 &&
                string.IsNullOrWhiteSpace(Cipher.Login.Uris.First().Uri))
            {
                Cipher.Login.Uris = null;
            }

            if(!EditMode && Cipher.OrganizationId != null)
            {
                // TODO: filter cipher collection ids
            }

            var cipher = await _cipherService.EncryptAsync(Cipher);
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Saving);
                await _cipherService.SaveWithServerAsync(cipher);
                Cipher.Id = cipher.Id;
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null,
                    EditMode ? AppResources.ItemUpdated : AppResources.NewItemCreated);
                _messagingService.Send(EditMode ? "editedCipher" : "addedCipher");
                await Page.Navigation.PopModalAsync();
                return true;
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
            }
            return false;
        }

        public async Task<bool> DeleteAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.DoYouReallyWantToDelete,
                null, AppResources.Yes, AppResources.No);
            if(!confirmed)
            {
                return false;
            }
            try
            {
                await _deviceActionService.ShowLoadingAsync(AppResources.Deleting);
                await _cipherService.DeleteWithServerAsync(Cipher.Id);
                await _deviceActionService.HideLoadingAsync();
                _platformUtilsService.ShowToast("success", null, AppResources.ItemDeleted);
                _messagingService.Send("deletedCipher");
                return true;
            }
            catch(ApiException e)
            {
                await _deviceActionService.HideLoadingAsync();
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, e.Error.GetSingleMessage(), AppResources.Ok);
            }
            return false;
        }

        public void GeneratePassword()
        {
            // TODO: push modal for generate page
        }

        public async void UriOptions(LoginUriView uri)
        {
            if(!(Page as AddEditPage).DoOnce())
            {
                return;
            }
            var selection = await Page.DisplayActionSheet(AppResources.Options, AppResources.Cancel, null,
                AppResources.MatchDetection, AppResources.Remove);
            if(selection == AppResources.Remove)
            {
                Uris.Remove(uri);
            }
            else if(selection == AppResources.MatchDetection)
            {
                var options = _matchDetectionOptions.Select(o => o.Key == uri.Match ? $"✓ {o.Value}" : o.Value);
                var matchSelection = await Page.DisplayActionSheet(AppResources.URIMatchDetection,
                    AppResources.Cancel, null, options.ToArray());
                if(matchSelection != null && matchSelection != AppResources.Cancel)
                {
                    var matchSelectionClean = matchSelection.Replace("✓ ", string.Empty);
                    uri.Match = _matchDetectionOptions.FirstOrDefault(o => o.Value == matchSelectionClean).Key;
                }
            }
        }

        public void AddUri()
        {
            if(Cipher.Type != CipherType.Login)
            {
                return;
            }
            if(Uris == null)
            {
                Uris = new ExtendedObservableCollection<LoginUriView>();
            }
            Uris.Add(new LoginUriView());
        }

        public async void FieldOptions(AddEditPageFieldViewModel field)
        {
            if(!(Page as AddEditPage).DoOnce())
            {
                return;
            }
            var selection = await Page.DisplayActionSheet(AppResources.Options, AppResources.Cancel, null,
                AppResources.Edit, AppResources.MoveUp, AppResources.MoveDown, AppResources.Remove);
            if(selection == AppResources.Remove)
            {
                Fields.Remove(field);
            }
            else if(selection == AppResources.Edit)
            {
                var name = await _deviceActionService.DisplayPromptAync(AppResources.CustomFieldName,
                    null, field.Field.Name);
                field.Field.Name = name ?? field.Field.Name;
                field.TriggerFieldChanged();
            }
            else if(selection == AppResources.MoveUp)
            {
                var currentIndex = Fields.IndexOf(field);
                if(currentIndex > 0)
                {
                    Fields.Move(currentIndex, currentIndex - 1);
                }
            }
            else if(selection == AppResources.MoveDown)
            {
                var currentIndex = Fields.IndexOf(field);
                if(currentIndex < Fields.Count - 1)
                {
                    Fields.Move(currentIndex, currentIndex + 1);
                }
            }
        }

        public async void AddField()
        {
            var typeSelection = await Page.DisplayActionSheet(AppResources.SelectTypeField, AppResources.Cancel, null,
                _fieldTypeOptions.Select(f => f.Value).ToArray());
            if(typeSelection != null && typeSelection != AppResources.Cancel)
            {
                var name = await _deviceActionService.DisplayPromptAync(AppResources.CustomFieldName);
                if(name == null)
                {
                    return;
                }
                if(Fields == null)
                {
                    Fields = new ExtendedObservableCollection<AddEditPageFieldViewModel>();
                }
                var type = _fieldTypeOptions.FirstOrDefault(f => f.Value == typeSelection).Key;
                Fields.Add(new AddEditPageFieldViewModel(new FieldView
                {
                    Type = type,
                    Name = string.IsNullOrWhiteSpace(name) ? null : name
                }));
            }
        }

        public void TogglePassword()
        {
            ShowPassword = !ShowPassword;
        }

        public void ToggleCardCode()
        {
            ShowCardCode = !ShowCardCode;
        }

        private void TypeChanged()
        {
            if(Cipher != null && TypeSelectedIndex > -1)
            {
                Cipher.Type = TypeOptions[TypeSelectedIndex].Value;
                TriggerCipherChanged();
            }
        }

        private void CardBrandChanged()
        {
            if(Cipher?.Card != null && CardBrandSelectedIndex > -1)
            {
                Cipher.Card.Brand = CardBrandOptions[CardBrandSelectedIndex].Value;
            }
        }

        private void CardExpMonthChanged()
        {
            if(Cipher?.Card != null && CardExpMonthSelectedIndex > -1)
            {
                Cipher.Card.ExpMonth = CardExpMonthOptions[CardExpMonthSelectedIndex].Value;
            }
        }

        private void IdentityTitleChanged()
        {
            if(Cipher?.Identity != null && IdentityTitleSelectedIndex > -1)
            {
                Cipher.Identity.Title = IdentityTitleOptions[IdentityTitleSelectedIndex].Value;
            }
        }

        private void FolderChanged()
        {
            if(Cipher != null && FolderSelectedIndex > -1)
            {
                Cipher.FolderId = FolderOptions[FolderSelectedIndex].Value;
            }
        }

        private void TriggerCipherChanged()
        {
            TriggerPropertyChanged(nameof(Cipher), _additionalCipherProperties);
        }

        private async void CheckPasswordAsync()
        {
            if(!(Page as BaseContentPage).DoOnce())
            {
                return;
            }
            if(string.IsNullOrWhiteSpace(Cipher.Login?.Password))
            {
                return;
            }
            await _deviceActionService.ShowLoadingAsync(AppResources.CheckingPassword);
            var matches = await _auditService.PasswordLeakedAsync(Cipher.Login.Password);
            await _deviceActionService.HideLoadingAsync();
            if(matches > 0)
            {
                await _platformUtilsService.ShowDialogAsync(string.Format(AppResources.PasswordExposed,
                    matches.ToString("N0")));
            }
            else
            {
                await _platformUtilsService.ShowDialogAsync(AppResources.PasswordSafe);
            }
        }
    }

    public class AddEditPageFieldViewModel : BaseViewModel
    {
        private FieldView _field;
        private bool _showHiddenValue;
        private bool _booleanValue;
        private string[] _additionalFieldProperties = new string[]
        {
            nameof(IsBooleanType),
            nameof(IsHiddenType),
            nameof(IsTextType),
        };

        public AddEditPageFieldViewModel(FieldView field)
        {
            Field = field;
            ToggleHiddenValueCommand = new Command(ToggleHiddenValue);
            BooleanValue = IsBooleanType && field.Value == "true";
        }

        public FieldView Field
        {
            get => _field;
            set => SetProperty(ref _field, value, additionalPropertyNames: _additionalFieldProperties);
        }

        public bool ShowHiddenValue
        {
            get => _showHiddenValue;
            set => SetProperty(ref _showHiddenValue, value,
                additionalPropertyNames: new string[]
                {
                    nameof(ShowHiddenValueIcon)
                });
        }

        public bool BooleanValue
        {
            get => _booleanValue;
            set
            {
                SetProperty(ref _booleanValue, value);
                if(IsBooleanType)
                {
                    Field.Value = value ? "true" : "false";
                }
            }
        }

        public Command ToggleHiddenValueCommand { get; set; }

        public string ShowHiddenValueIcon => _showHiddenValue ? "" : "";
        public bool IsTextType => _field.Type == FieldType.Text;
        public bool IsBooleanType => _field.Type == FieldType.Boolean;
        public bool IsHiddenType => _field.Type == FieldType.Hidden;

        public void ToggleHiddenValue()
        {
            ShowHiddenValue = !ShowHiddenValue;
        }

        public void TriggerFieldChanged()
        {
            TriggerPropertyChanged(nameof(Field), _additionalFieldProperties);
        }
    }
}
