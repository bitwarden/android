using Bit.App.Resources;
using System;
using System.Collections.Generic;
using System.Windows.Input;
using Xamarin.Forms;

namespace Bit.App.Pages
{
    public class SettingsPageViewModel : BaseViewModel
    {
        public SettingsPageViewModel()
        {
            PageTitle = AppResources.Settings;

            ButtonCommand = new Command(() => Page.DisplayAlert("Button 1 Command", "Button 1 message", "Cancel"));
            Button2Command = new Command(() => Page.DisplayAlert("Button 2 Command", "Button 2 message", "Cancel"));
            BuildList();
        }

        public ICommand ButtonCommand { get; }
        public ICommand Button2Command { get; }
        public List<SettingsPageListGroup> GroupedItems { get; set; }

        private void BuildList()
        {
            var doUpper = Device.RuntimePlatform != Device.Android;
            var manageItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.Folders },
                new SettingsPageListItem { Name = AppResources.Sync }
            };
            var securityItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.LockOptions },
                new SettingsPageListItem { Name = string.Format(AppResources.UnlockWith, AppResources.Fingerprint) },
                new SettingsPageListItem { Name = AppResources.UnlockWithPIN },
                new SettingsPageListItem { Name = AppResources.Lock },
                new SettingsPageListItem { Name = AppResources.TwoStepLogin }
            };
            var accountItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.ChangeMasterPassword },
                new SettingsPageListItem { Name = AppResources.LogOut }
            };
            var otherItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem { Name = AppResources.Options },
                new SettingsPageListItem { Name = AppResources.About },
                new SettingsPageListItem { Name = AppResources.HelpAndFeedback },
                new SettingsPageListItem { Name = AppResources.RateTheApp }
            };
            GroupedItems = new List<SettingsPageListGroup>
            {
                new SettingsPageListGroup(manageItems, AppResources.Manage, doUpper),
                new SettingsPageListGroup(securityItems, AppResources.Security, doUpper),
                new SettingsPageListGroup(accountItems, AppResources.Account, doUpper),
                new SettingsPageListGroup(otherItems, AppResources.Other, doUpper)
            };
        }
    }
}
