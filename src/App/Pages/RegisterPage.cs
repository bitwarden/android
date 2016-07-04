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

        public FormEntryCell EmailCell { get; set; }
        public FormEntryCell PasswordCell { get; set; }
        public FormEntryCell ConfirmPasswordCell { get; set; }
        public FormEntryCell PasswordHintCell { get; set; }

        private void Init()
        {
            var padding = new Thickness(15, 20);

            PasswordHintCell = new FormEntryCell("Master Password Hint (optional)", useLabelAsPlaceholder: true,
                imageSource: "lightbulb-o", containerPadding: padding);
            ConfirmPasswordCell = new FormEntryCell("Re-type Master Password", IsPassword: true,
                nextElement: PasswordHintCell.Entry, useLabelAsPlaceholder: true, imageSource: "lock", containerPadding: padding);
            PasswordCell = new FormEntryCell(AppResources.MasterPassword, IsPassword: true,
                nextElement: ConfirmPasswordCell.Entry, useLabelAsPlaceholder: true, imageSource: "lock", containerPadding: padding);
            EmailCell = new FormEntryCell(AppResources.EmailAddress, nextElement: PasswordCell.Entry,
                entryKeyboard: Keyboard.Email, useLabelAsPlaceholder: true, imageSource: "envelope", containerPadding: padding);

            PasswordHintCell.Entry.ReturnType = Enums.ReturnType.Done;
            PasswordHintCell.Entry.Completed += Entry_Completed;

            var table = new FormTableView
            {
                Root = new TableRoot
                {
                    new TableSection
                    {
                        EmailCell,
                        PasswordCell
                    }
                }
            };

            var passwordLabel = new Label
            {
                Text = "The master password is the password you use to access your vault. It is very important that you do not forget your master password. There is no way to recover the password in the event that you forget it.",
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            var table2 = new FormTableView
            {
                Root = new TableRoot
                {
                    new TableSection
                    {
                        ConfirmPasswordCell,
                        PasswordHintCell
                    }
                }
            };

            var hintLabel = new Label
            {
                Text = "A master password hint can help you remember your password if you forget it.",
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            var layout = new RelativeLayout
            {
                Padding = new Thickness(0, 0, 0, 35)
            };
            layout.Children.Add(
                table,
                Constraint.Constant(0),
                Constraint.Constant(0),
                Constraint.RelativeToParent((parent) => { return parent.Width; })
            );
            layout.Children.Add(
                passwordLabel,
                Constraint.Constant(15),
                Constraint.RelativeToView(table, (parent, sibling) => { return sibling.Y + sibling.Height - (this.IsPortrait() ? 45 : 25); }),
                Constraint.RelativeToParent((parent) => { return parent.Width - 30; })
            );
            layout.Children.Add(
                table2,
                Constraint.Constant(0),
                Constraint.RelativeToView(passwordLabel, (parent, sibling) => { return sibling.Y + sibling.Height - (this.IsPortrait() ? 15 : 10); }),
                Constraint.RelativeToParent((parent) => { return parent.Width; })
            );
            layout.Children.Add(
                hintLabel,
                Constraint.Constant(15),
                Constraint.RelativeToView(table2, (parent, sibling) => { return sibling.Y + sibling.Height - (this.IsPortrait() ? 45 : 25); }),
                Constraint.RelativeToParent((parent) => { return parent.Width - 30; })
            );

            layout.LowerChild(table2);
            layout.LowerChild(table);

            var scrollView = new ScrollView
            {
                Content = layout
            };

            var loginToolbarItem = new ToolbarItem("Submit", null, async () =>
            {
                await Register();
            }, ToolbarItemOrder.Default, 0);

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = table2.RowHeight = table2.RowHeight = -1;
                table.EstimatedRowHeight = table2.EstimatedRowHeight = table2.EstimatedRowHeight = 70;
                ToolbarItems.Add(new DismissModalToolBarItem(this, "Cancel"));
            }

            ToolbarItems.Add(loginToolbarItem);
            Title = "Create Account";
            Content = scrollView;
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

        private class FormTableView : ExtendedTableView
        {
            public FormTableView()
            {
                Intent = TableIntent.Settings;
                EnableScrolling = false;
                HasUnevenRows = true;
                EnableSelection = false;
                VerticalOptions = LayoutOptions.Start;
            }
        }
    }
}
