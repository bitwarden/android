using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Request;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class EventService : IEventService
    {
        private readonly IStorageService _storageService;
        private readonly IApiService _apiService;
        private readonly IUserService _userService;
        private readonly ICipherService _cipherService;

        public EventService(
            IStorageService storageService,
            IApiService apiService,
            IUserService userService,
            ICipherService cipherService)
        {
            _storageService = storageService;
            _apiService = apiService;
            _userService = userService;
            _cipherService = cipherService;
        }

        public async Task CollectAsync(EventType eventType, string cipherId = null, bool uploadImmediately = false)
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            var organizations = await _userService.GetAllOrganizationAsync();
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
            var eventCollection = await _storageService.GetAsync<List<EventData>>(Constants.EventCollectionKey);
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
            await _storageService.SaveAsync(Constants.EventCollectionKey, eventCollection);
            if (uploadImmediately)
            {
                await UploadEventsAsync();
            }
        }

        public async Task UploadEventsAsync()
        {
            var authed = await _userService.IsAuthenticatedAsync();
            if (!authed)
            {
                return;
            }
            var eventCollection = await _storageService.GetAsync<List<EventData>>(Constants.EventCollectionKey);
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
            await _storageService.RemoveAsync(Constants.EventCollectionKey);
        }
    }
}
