using Bit.App.Resources;
using System;
using System.Collections.Generic;
using System.Text;
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
        }

        public ICommand ButtonCommand { get; }
        public ICommand Button2Command { get; }
    }
}
