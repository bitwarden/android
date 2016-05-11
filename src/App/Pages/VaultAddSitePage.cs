using System;
using System.Linq;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Models;
using Bit.App.Resources;
using Plugin.Connectivity.Abstractions;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultAddSitePage : ContentPage
    {
        private readonly ISiteService _siteService;
        private readonly IFolderService _folderService;
        private readonly IUserDialogs _userDialogs;
        private readonly IConnectivity _connectivity;

        public VaultAddSitePage()
        {
            _siteService = Resolver.Resolve<ISiteService>();
            _folderService = Resolver.Resolve<IFolderService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _connectivity = Resolver.Resolve<IConnectivity>();

            Init();
        }

        private void Init()
        {
            var folders = _folderService.GetAllAsync().GetAwaiter().GetResult().OrderBy( f => f.Name?.Decrypt() );

            var uriEntry = new BottomBorderEntry { Keyboard = Keyboard.Url };
            var nameEntry = new BottomBorderEntry();
            var folderPicker = new ExtendedPicker
            {
                Title = AppResources.Folder,
                BottomBorderColor = Color.FromHex( "d2d6de" ),
                HasOnlyBottomBorder = true
            };
            folderPicker.Items.Add( AppResources.FolderNone );
            folderPicker.SelectedIndex = 0;
            foreach( var folder in folders )
            {
                folderPicker.Items.Add( folder.Name.Decrypt() );
            }
            var usernameEntry = new BottomBorderEntry();
            var passwordEntry = new BottomBorderEntry { IsPassword = true };
            var notesEditor = new Editor { HeightRequest = 75 };

            var stackLayout = new StackLayout { Padding = new Thickness( 15 ) };
            stackLayout.Children.Add( new EntryLabel { Text = AppResources.URI } );
            stackLayout.Children.Add( uriEntry );
            stackLayout.Children.Add( new EntryLabel { Text = AppResources.Name, Margin = new Thickness( 0, 15, 0, 0 ) } );
            stackLayout.Children.Add( nameEntry );
            stackLayout.Children.Add( new EntryLabel { Text = AppResources.Folder, Margin = new Thickness( 0, 15, 0, 0 ) } );
            stackLayout.Children.Add( folderPicker );
            stackLayout.Children.Add( new EntryLabel { Text = AppResources.Username, Margin = new Thickness( 0, 15, 0, 0 ) } );
            stackLayout.Children.Add( usernameEntry );
            stackLayout.Children.Add( new EntryLabel { Text = AppResources.Password, Margin = new Thickness( 0, 15, 0, 0 ) } );
            stackLayout.Children.Add( passwordEntry );
            stackLayout.Children.Add( new EntryLabel { Text = AppResources.Notes, Margin = new Thickness( 0, 15, 0, 0 ) } );
            stackLayout.Children.Add( notesEditor );

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical
            };

            var saveToolBarItem = new ToolbarItem( AppResources.Save, null, async () =>
             {
                 if( !_connectivity.IsConnected )
                 {
                     AlertNoConnection();
                     return;
                 }

                 if( string.IsNullOrWhiteSpace( uriEntry.Text ) )
                 {
                     await DisplayAlert( AppResources.AnErrorHasOccurred, string.Format( AppResources.ValidationFieldRequired, AppResources.URI ), AppResources.Ok );
                     return;
                 }

                 if( string.IsNullOrWhiteSpace( nameEntry.Text ) )
                 {
                     await DisplayAlert( AppResources.AnErrorHasOccurred, string.Format( AppResources.ValidationFieldRequired, AppResources.Name ), AppResources.Ok );
                     return;
                 }

                 var site = new Site
                 {
                     Uri = uriEntry.Text.Encrypt(),
                     Name = nameEntry.Text.Encrypt(),
                     Username = usernameEntry.Text?.Encrypt(),
                     Password = passwordEntry.Text?.Encrypt(),
                     Notes = notesEditor.Text?.Encrypt(),
                 };

                 if( folderPicker.SelectedIndex > 0 )
                 {
                     site.FolderId = folders.ElementAt( folderPicker.SelectedIndex - 1 ).Id;
                 }

                 var saveTask = _siteService.SaveAsync( site );
                 _userDialogs.ShowLoading( "Saving...", MaskType.Black );
                 await saveTask;

                 _userDialogs.HideLoading();
                 await Navigation.PopAsync();
                 _userDialogs.SuccessToast( nameEntry.Text, "New site created." );
             }, ToolbarItemOrder.Default, 0 );

            Title = AppResources.AddSite;
            Content = scrollView;
            ToolbarItems.Add( saveToolBarItem );

            if( !_connectivity.IsConnected )
            {
                AlertNoConnection();
            }
        }

        private void AlertNoConnection()
        {
            DisplayAlert( AppResources.InternetConnectionRequiredTitle, AppResources.InternetConnectionRequiredMessage, AppResources.Ok );
        }
    }
}

