using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;
using System.Threading.Tasks;

namespace Bit.App.Pages
{
    public class ExtensionPageViewModel : BaseViewModel
    {
        private const string StartedKey = "appExtensionStarted";
        private const string ActivatedKey = "appExtensionActivated";

        private readonly IMessagingService _messagingService;
        private readonly IStorageService _storageService;
        private readonly IPlatformUtilsService _platformUtilsService;

        private bool _started;
        private bool _activated;

        public ExtensionPageViewModel()
        {
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _storageService = ServiceContainer.Resolve<IStorageService>("storageService");
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            PageTitle = AppResources.AppExtension;
        }

        public bool Started
        {
            get => _started;
            set => SetProperty(ref _started, value, additionalPropertyNames: new string[]
            {
                nameof(NotStarted),
                nameof(StartedAndNotActivated),
                nameof(StartedAndActivated)
            });
        }

        public bool Activated
        {
            get => _activated;
            set => SetProperty(ref _activated, value, additionalPropertyNames: new string[]
            {
                nameof(StartedAndNotActivated),
                nameof(StartedAndActivated)
            });
        }

        public bool NotStarted => !Started;
        public bool StartedAndNotActivated => Started && !Activated;
        public bool StartedAndActivated => Started && Activated;

        public async Task InitAsync()
        {
            var started = await _storageService.GetAsync<bool?>(StartedKey);
            var activated = await _storageService.GetAsync<bool?>(ActivatedKey);
            Started = started.GetValueOrDefault();
            Activated = activated.GetValueOrDefault();
        }

        public void ShowExtension()
        {
            _messagingService.Send("showAppExtension", this);
        }

        public void EnabledExtension(bool enabled)
        {
            Started = true;
            if (!Activated && enabled)
            {
                Activated = enabled;
            }
        }
    }
}
