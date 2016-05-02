using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;
using Bit.App.Abstractions;
using Bit.App.Models;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Views
{
    public class VaultAddFolderPage : ContentPage
    {
        public VaultAddFolderPage()
        {
            var cryptoService = Resolver.Resolve<ICryptoService>();
            var folderService = Resolver.Resolve<IFolderService>();

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
                if(string.IsNullOrWhiteSpace(nameEntry.Text))
                {
                    await DisplayAlert("An error has occurred", "The Name field is required.", "Ok");
                    return;
                }

                var folder = new Folder
                {
                    Name = new CipherString(nameEntry.Text)
                };
                await folderService.SaveAsync(folder);
                await Navigation.PopAsync();
            }, ToolbarItemOrder.Default, 0);

            Title = "Add Folder";
            Content = scrollView;
            ToolbarItems.Add(saveToolBarItem);
        }
    }
}
