using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Api;
using Bit.App.Resources;
using Plugin.DeviceInfo.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class PasswordHintPage : ExtendedContentPage
    {
        private IUserDialogs _userDialogs;
        private IAccountsApiRepository _accountApiRepository;

        public PasswordHintPage()
            : base(updateActivity: false)
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _accountApiRepository = Resolver.Resolve<IAccountsApiRepository>();

            Init();
        }

        public FormEntryCell EmailCell { get; set; }

        private void Init()
        {
            EmailCell = new FormEntryCell(AppResources.EmailAddress, entryKeyboard: Keyboard.Email,
                useLabelAsPlaceholder: true, imageSource: "envelope", containerPadding: new Thickness(15, 20));

            EmailCell.Entry.ReturnType = Enums.ReturnType.Go;
            EmailCell.Entry.Completed += Entry_Completed;

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = false,
                HasUnevenRows = true,
                EnableSelection = true,
                NoFooter = true,
                VerticalOptions = LayoutOptions.Start,
                Root = new TableRoot
                {
                    new TableSection()
                    {
                        EmailCell
                    }
                }
            };

            var hintLabel = new Label
            {
                Text = "Enter your account email address to receive your master password hint.",
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                Margin = new Thickness(15, (this.IsLandscape() ? 5 : 0), 15, 25)
            };

            var layout = new StackLayout
            {
                Children = { table, hintLabel },
                Spacing = 0
            };

            var scrollView = new ScrollView { Content = layout };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }

            var submitToolbarItem = new ToolbarItem("Submit", null, async () =>
            {
                await SubmitAsync();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(submitToolbarItem);
            Title = "Password Hint";
            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            EmailCell.Entry.Focus();
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await SubmitAsync();
        }

        private async Task SubmitAsync()
        {
            if(string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.EmailAddress), AppResources.Ok);
                return;
            }

            var request = new PasswordHintRequest
            {
                Email = EmailCell.Entry.Text
            };

            var responseTask = _accountApiRepository.PostPasswordHintAsync(request);
            _userDialogs.ShowLoading("Submitting...", MaskType.Black);
            var response = await responseTask;
            _userDialogs.HideLoading();
            if(!response.Succeeded)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.FirstOrDefault()?.Message, AppResources.Ok);
                return;
            }
            else
            {
                await DisplayAlert(null, "We've sent you an email with your master password hint. ", AppResources.Ok);
            }

            await Navigation.PopAsync();
        }
    }
}
