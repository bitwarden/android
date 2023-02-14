using Bit.App.Abstractions;
using Bit.App.Lists.ItemViewModels.CustomFields;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Utilities;

namespace Bit.App.Utilities
{
    public interface IAppSetup
    {
        void InitializeServicesLastChance();
    }

    public class AppSetup : IAppSetup
    {
        public void InitializeServicesLastChance()
        {
            var i18nService = ServiceContainer.Resolve<II18nService>("i18nService");
            var eventService = ServiceContainer.Resolve<IEventService>("eventService");

            // TODO: This could be further improved by Lazy Registration since it may not be needed at all
            ServiceContainer.Register<ICustomFieldItemFactory>("customFieldItemFactory", new CustomFieldItemFactory(i18nService, eventService));
            ServiceContainer.Register<IDeepLinkContext>(new DeepLinkContext());
        }
    }
}
