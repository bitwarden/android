using Bit.App.Abstractions;
using System;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class ExtendedContentPage : ContentPage
    {
        private ISyncService _syncService;

        public ExtendedContentPage(bool syncIndicator = true)
        {
            _syncService = Resolver.Resolve<ISyncService>();

            BackgroundColor = Color.FromHex("efeff4");

            if(syncIndicator)
            {
                IsBusy = _syncService.SyncInProgress;

                MessagingCenter.Subscribe<Application, bool>(Application.Current, "SyncCompleted", (sender, success) =>
                {
                    IsBusy = _syncService.SyncInProgress;
                });

                MessagingCenter.Subscribe<Application>(Application.Current, "SyncStarted", (sender) =>
                {
                    IsBusy = _syncService.SyncInProgress;
                });
            }
        }
    }
}
