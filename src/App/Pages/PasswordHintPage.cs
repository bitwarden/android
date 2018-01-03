using System;
using System.Linq;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models.Api;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using System.Threading.Tasks;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class PasswordHintPage : ExtendedContentPage
    {
        private IAccountsApiRepository _accountApiRepository;
        private IDeviceActionService _deviceActionService;

        public PasswordHintPage()
            : base(updateActivity: false)
        {
            _accountApiRepository = Resolver.Resolve<IAccountsApiRepository>();
            _deviceActionService = Resolver.Resolve<IDeviceActionService>();
            Init();
        }

        public FormEntryCell EmailCell { get; set; }

        private void Init()
        {
            var padding = Helpers.OnPlatform(
                iOS: new Thickness(15, 20),
                Android: new Thickness(15, 8),
                Windows: new Thickness(10, 8));

            EmailCell = new FormEntryCell(AppResources.EmailAddress, entryKeyboard: Keyboard.Email,
                useLabelAsPlaceholder: true, imageSource: "envelope.png", containerPadding: padding);

            EmailCell.Entry.ReturnType = Enums.ReturnType.Go;

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
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
                    {
                        EmailCell
                    }
                }
            };

            var hintLabel = new Label
            {
                Text = AppResources.EnterEmailForHint,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                Margin = new Thickness(15, (this.IsLandscape() ? 5 : 0), 15, 25)
            };

            var layout = new RedrawableStackLayout
            {
                Children = { table, hintLabel },
                Spacing = 0
            };

            table.WrappingStackLayout = () => layout;
            var scrollView = new ScrollView { Content = layout };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }

            var submitToolbarItem = new ToolbarItem(AppResources.Submit, Helpers.ToolbarImage("ion_chevron_right.png"), async () =>
            {
                await SubmitAsync();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(submitToolbarItem);
            Title = AppResources.PasswordHint;
            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            EmailCell.InitEvents();
            EmailCell.Entry.Completed += Entry_Completed;
            EmailCell.Entry.FocusWithDelay();
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            EmailCell.Dispose();
            EmailCell.Entry.Completed -= Entry_Completed;
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            await SubmitAsync();
        }

        private async Task SubmitAsync()
        {
            if(string.IsNullOrWhiteSpace(EmailCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.EmailAddress), AppResources.Ok);
                return;
            }

            var request = new PasswordHintRequest
            {
                Email = EmailCell.Entry.Text
            };

            _deviceActionService.ShowLoading(AppResources.Submitting);
            var response = await _accountApiRepository.PostPasswordHintAsync(request);
            _deviceActionService.HideLoading();

            if(!response.Succeeded)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.Errors.FirstOrDefault()?.Message, AppResources.Ok);
                return;
            }
            else
            {
                await DisplayAlert(null, AppResources.PasswordHintAlert, AppResources.Ok);
            }

            await Navigation.PopAsync();
        }
    }
}
