using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class SettingsService : ISettingsService
    {
        private readonly ISettingsRepository _settingsRepository;
        private readonly ISettings _settings;

        public SettingsService(
            ISettingsRepository settingsRepository,
            ISettings settings)
        {
            _settingsRepository = settingsRepository;
            _settings = settings;
        }
    }
}
