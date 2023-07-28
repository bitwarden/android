using System.Threading.Tasks;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Pages
{
    public class ExtensionPageViewModel : BaseViewModel
    {
        private readonly IMessagingService _messagingService;

        private bool _started;
        private bool _activated;

        public ExtensionPageViewModel()
        {
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
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
            Started = false;
            Activated = false;
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
