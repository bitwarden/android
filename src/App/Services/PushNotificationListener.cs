using PushNotification.Plugin.Abstractions;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PushNotification.Plugin;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Bit.App.Abstractions;

namespace Bit.App.Services
{
    public class PushNotificationListener : IPushNotificationListener
    {
        private bool _showNotification;
        private readonly ISyncService _syncService;
        private readonly IDeviceApiRepository _deviceApiRepository;
        private readonly IAuthService _authService;

        public PushNotificationListener(
            ISyncService syncService,
            IDeviceApiRepository deviceApiRepository,
            IAuthService authService)
        {
            _syncService = syncService;
            _deviceApiRepository = deviceApiRepository;
            _authService = authService;
        }

        public void OnMessage(JObject values, DeviceType deviceType)
        {
            _showNotification = false;
            Debug.WriteLine("Message Arrived: {0}", JsonConvert.SerializeObject(values));
        }

        public void OnRegistered(string token, DeviceType deviceType)
        {
            Debug.WriteLine(string.Format("Push Notification - Device Registered - Token : {0}", token));

            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var response = _deviceApiRepository.PostAsync(new Models.Api.DeviceRequest
            {
                Name = deviceType.ToString(),
                Type = deviceType,
                PushToken = token
            }).GetAwaiter().GetResult();

            if(response.Succeeded)
            {
                Debug.WriteLine("Registered device with server.");
            }
            else
            {
                Debug.WriteLine("Failed to register device.");
            }
        }

        public void OnUnregistered(DeviceType deviceType)
        {
            Debug.WriteLine("Push Notification - Device Unnregistered");
        }

        public void OnError(string message, DeviceType deviceType)
        {
            Debug.WriteLine(string.Format("Push notification error - {0}", message));
        }

        public bool ShouldShowNotification()
        {
            return _showNotification;
        }
    }
}
