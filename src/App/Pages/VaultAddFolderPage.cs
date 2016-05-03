using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Models;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultAddFolderPage : ContentPage
    {
        public VaultAddFolderPage()
        {
            var cryptoService = Resolver.Resolve<ICryptoService>();
            var folderService = Resolver.Resolve<IFolderService>();
            var userDialogs = Resolver.Resolve<IUserDialogs>();
            var connectivity = Resolver.Resolve<IConnectivity>();

            var nameEntry = new Entry();

            var stackLayout = new StackLayout();
            stackLayout.Children.Add(new Label { Text = "Name" });
            stackLayout.Children.Add(nameEntry);

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical
            };

            var saveToolBarItem = new ToolbarItem("Save", null, async () =>
            {
                if(!connectivity.IsConnected)
                {
                    AlertNoConnection();
                    return;
                }

                if(string.IsNullOrWhiteSpace(nameEntry.Text))
                {
                    await DisplayAlert("An error has occurred", "The Name field is required.", "Ok");
                    return;
                }

                var folder = new Folder
                {
                    Name = nameEntry.Text.Encrypt()
                };

                var saveTask = folderService.SaveAsync(folder);
                userDialogs.ShowLoading("Saving...", MaskType.Black);
                await saveTask;

                userDialogs.HideLoading();
                await Navigation.PopAsync();
                userDialogs.SuccessToast(nameEntry.Text, "New folder created.");
            }, ToolbarItemOrder.Default, 0);

            Title = "Add Folder";
            Content = scrollView;
            ToolbarItems.Add(saveToolBarItem);

            if(!connectivity.IsConnected)
            {
                AlertNoConnection();
            }
        }

        public void AlertNoConnection()
        {
            DisplayAlert("No internet connection", "Adding a new folder required an internet connection. Please connect to the internet before continuing.", "Ok");
        }
    }
}
