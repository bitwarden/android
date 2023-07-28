using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Request;

namespace Bit.Core.Services
{
    public class EventService : IEventService
    {
        private readonly IApiService _apiService;
        private readonly IStateService _stateService;
        private readonly IOrganizationService _organizationService;
        private readonly ICipherService _cipherService;

        public EventService(
            IApiService apiService,
            IStateService stateService,
            IOrganizationService organizationService,
            ICipherService cipherService)
        {
            _apiService = apiService;
            _stateService = stateService;
            _organizationService = organizationService;
            _cipherService = cipherService;
        }

        public async Task CollectAsync(EventType eventType, string cipherId = null, bool uploadImmediately = false)
        {
            var authed = await _stateService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            var organizations = await _organizationService.GetAllAsync();
            if (organizations == null)
            {
                return;
            }
            var orgIds = new HashSet<string>(organizations.Where(o => o.UseEvents).Select(o => o.Id));
            if (!orgIds.Any())
            {
                return;
            }
            if (cipherId != null)
            {
                var cipher = await _cipherService.GetAsync(cipherId);
                if (cipher?.OrganizationId == null || !orgIds.Contains(cipher.OrganizationId))
                {
                    return;
                }
            }
            var eventCollection = await _stateService.GetEventCollectionAsync();
            if (eventCollection == null)
            {
                eventCollection = new List<EventData>();
            }
            eventCollection.Add(new EventData
            {
                Type = eventType,
                CipherId = cipherId,
                Date = DateTime.UtcNow
            });
            await _stateService.SetEventCollectionAsync(eventCollection);
            if (uploadImmediately)
            {
                await UploadEventsAsync();
            }
        }

        public async Task UploadEventsAsync()
        {
            var authed = await _stateService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            var eventCollection = await _stateService.GetEventCollectionAsync();
            if (eventCollection == null || !eventCollection.Any())
            {
                return;
            }
            var request = eventCollection.Select(e => new EventRequest
            {
                Type = e.Type,
                CipherId = e.CipherId,
                Date = e.Date
            });
            try
            {
                await _apiService.PostEventsCollectAsync(request);
                await ClearEventsAsync();
            }
            catch (ApiException) { }
        }

        public async Task ClearEventsAsync()
        {
            await _stateService.SetEventCollectionAsync(null);
        }
    }
}
