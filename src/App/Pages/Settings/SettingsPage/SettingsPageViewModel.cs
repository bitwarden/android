using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Resources;
using Xamarin.CommunityToolkit.ObjectModel;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageViewModel : BaseViewModel
    {
        public SettingsPageViewModel()
        {
            ExecuteSettingItemCommand = new AsyncCommand<SettingsPageListItem>(item => item.ExecuteAsync(),
                onException: ex => HandleException(ex),
                allowsMultipleExecutions: false);

            SettingsItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem(nameof(AppResources.AccountSecurity), () => NavigateToAsync(new SecuritySettingsPage())),
                new SettingsPageListItem(nameof(AppResources.Autofill), () => NavigateToAsync(new AutofillSettingsPage())),
                new SettingsPageListItem(nameof(AppResources.Vault), () => NavigateToAsync(new VaultSettingsPage())),
                new SettingsPageListItem(nameof(AppResources.Appearance), () => NavigateToAsync(new AppearanceSettingsPage())),
                new SettingsPageListItem(nameof(AppResources.Other), () => NavigateToAsync(new OtherSettingsPage())),
                new SettingsPageListItem(nameof(AppResources.About), () => NavigateToAsync(new AboutSettingsPage()))
            };
        }

        public List<SettingsPageListItem> SettingsItems { get; }

        public IAsyncCommand<SettingsPageListItem> ExecuteSettingItemCommand { get; }

        private async Task NavigateToAsync(Page page)
        {
            await Page.Navigation.PushAsync(page);
        }
    }
}
