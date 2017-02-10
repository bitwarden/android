using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using System.Collections.Generic;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace Bit.App.Services
{
    public class SettingsService : ISettingsService
    {
        private readonly ISettingsRepository _settingsRepository;
        private readonly ISettings _settings;
        private readonly IAuthService _authService;

        public SettingsService(
            ISettingsRepository settingsRepository,
            ISettings settings,
            IAuthService authService)
        {
            _settingsRepository = settingsRepository;
            _settings = settings;
            _authService = authService;
        }

        public async Task<IEnumerable<IEnumerable<string>>> GetEquivalentDomainsAsync()
        {
            var settings = await _settingsRepository.GetByIdAsync(_authService.UserId);
            if(string.IsNullOrWhiteSpace(settings?.EquivalentDomains))
            {
                return new List<string[]>();
            }

            return JsonConvert.DeserializeObject<IEnumerable<IEnumerable<string>>>(settings.EquivalentDomains);
        }
    }
}
