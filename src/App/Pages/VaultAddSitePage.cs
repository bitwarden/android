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
    public class VaultAddSitePage : ContentPage
    {
        public VaultAddSitePage()
        {
            var cryptoService = Resolver.Resolve<ICryptoService>();
            var siteService = Resolver.Resolve<ISiteService>();

            var uriEntry = new Entry { Keyboard = Keyboard.Url };
            var nameEntry = new Entry();
            var folderEntry = new Entry { };
            var usernameEntry = new Entry();
            var passwordEntry = new Entry { IsPassword = true };
            var notesEditor = new Editor();

            var stackLayout = new StackLayout();
            stackLayout.Children.Add(new Label { Text = "URI" });
            stackLayout.Children.Add(uriEntry);
            stackLayout.Children.Add(new Label { Text = "Name" });
            stackLayout.Children.Add(nameEntry);
            stackLayout.Children.Add(new Label { Text = "Folder" });
            stackLayout.Children.Add(folderEntry);
            stackLayout.Children.Add(new Label { Text = "Username" });
            stackLayout.Children.Add(usernameEntry);
            stackLayout.Children.Add(new Label { Text = "Password" });
            stackLayout.Children.Add(passwordEntry);
            stackLayout.Children.Add(new Label { Text = "Notes" });
            stackLayout.Children.Add(notesEditor);

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical
            };

            var saveToolBarItem = new ToolbarItem("Save", null, async () =>
            {
                if(string.IsNullOrWhiteSpace(uriEntry.Text))
                {
                    await DisplayAlert("An error has occurred", "The Uri field is required.", "Ok");
                    return;
                }

                if(string.IsNullOrWhiteSpace(nameEntry.Text))
                {
                    await DisplayAlert("An error has occurred", "The Name field is required.", "Ok");
                    return;
                }

                var site = new Site
                {
                    Uri = uriEntry.Text.Encrypt(),
                    Name = nameEntry.Text.Encrypt(),
                    Username = usernameEntry.Text?.Encrypt(),
                    Password = passwordEntry.Text?.Encrypt(),
                    Notes = notesEditor.Text?.Encrypt()
                };

                await siteService.SaveAsync(site);
                await Navigation.PopAsync();
            }, ToolbarItemOrder.Default, 0);

            Title = "Add Site";
            Content = scrollView;
            ToolbarItems.Add(saveToolBarItem);
        }
    }
}
