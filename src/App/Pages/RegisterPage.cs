using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Api;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class RegisterPage : ExtendedContentPage
    {
        private ICryptoService _cryptoService;
        private IUserDialogs _userDialogs;
        private IAccountsApiRepository _accountsApiRepository;

        public RegisterPage()
        {
            _cryptoService = Resolver.Resolve<ICryptoService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _accountsApiRepository = Resolver.Resolve<IAccountsApiRepository>();

            Init();
        }

        public FormEntryCell NameCell { get; set; }
        public FormEntryCell EmailCell { get; set; }
        public FormEntryCell PasswordCell { get; set; }
        public FormEntryCell ConfirmPasswordCell { get; set; }
        public FormEntryCell PasswordHintCell { get; set; }

        private void Init()
        {
            PasswordHintCell = new FormEntryCell("Master Password Hint (optional)", useLabelAsPlaceholder: true, imageSource: "lightbulb-o");
            ConfirmPasswordCell = new FormEntryCell("Re-type Master Password", IsPassword: true, nextElement: PasswordHintCell.Entry, useLabelAsPlaceholder: true, imageSource: "lock");
            PasswordCell = new FormEntryCell(AppResources.MasterPassword, IsPassword: true, nextElement: ConfirmPasswordCell.Entry, useLabelAsPlaceholder: true, imageSource: "lock");
            NameCell = new FormEntryCell("Your Name", nextElement: PasswordCell.Entry, useLabelAsPlaceholder: true, imageSource: "user");
            EmailCell = new FormEntryCell(AppResources.EmailAddress, nextElement: NameCell.Entry, entryKeyboard: Keyboard.Email, useLabelAsPlaceholder: true, imageSource: "envelope");

            PasswordHintCell.Entry.ReturnType = Enums.ReturnType.Done;
            PasswordHintCell.Entry.Completed += Entry_Completed;

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = true,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    new TableSection()
                    {
                        EmailCell,
                        NameCell
                    },
                    new TableSection()
                    {
                        PasswordCell,
                        ConfirmPasswordCell,
                        PasswordHintCell
                    }
                }
            };

            var loginToolbarItem = new ToolbarItem("Submit", null, async () =>
            {
                await Register();
            }, ToolbarItemOrder.Default, 0);

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Cancel"));
            }

            ToolbarItems.Add(loginToolbarItem);
            Title = "Create Account";
            Content = table;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            EmailCell.Entry.Focus();
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await Register();
        }

        private async Task Register()
        {
            if(string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress), AppResources.Ok);
                return;
            }

            if(string.IsNullOrWhiteSpace(NameCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, "Your Name"), AppResources.Ok);
                return;
            }

            if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, "Your Name"), AppResources.Ok);
                return;
            }

            if(ConfirmPasswordCell.Entry.Text != PasswordCell.Entry.Text)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, "Password confirmation is not correct.", AppResources.Ok);
                return;
            }

            var key = _cryptoService.MakeKeyFromPassword(PasswordCell.Entry.Text, EmailCell.Entry.Text);
            var request = new RegisterRequest
            {
                Name = NameCell.Entry.Text,
                Email = EmailCell.Entry.Text,
                MasterPasswordHash = _cryptoService.HashPasswordBase64(key, PasswordCell.Entry.Text),
                MasterPasswordHint = !string.IsNullOrWhiteSpace(PasswordHintCell.Entry.Text) ? PasswordHintCell.Entry.Text : null
            };

            var responseTask = _accountsApiRepository.PostRegisterAsync(request);
            _userDialogs.ShowLoading("Creating account...", MaskType.Black);
            var response = await responseTask;
            _userDialogs.HideLoading();
            if(!response.Succeeded)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.FirstOrDefault()?.Message, AppResources.Ok);
                return;
            }

            _userDialogs.SuccessToast("Account Created", "Your new account has been created! You may now log in.");
            await Navigation.PopModalAsync();
        }
    }
}
