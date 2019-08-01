using System;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Utilities.Steam;
using Newtonsoft.Json;

namespace Bit.App.Utilities
{
    public static class SteamTimeSyncHelper
    {
        private static bool inSync = false;
        private static int timeDifference = 0;

        public static long GetSystemUnixTime()
        {
            return (long)(DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;
        }

        public static long GetSteamUnixTime()
        {
            if (!inSync)
            {
                SyncTime();
            }
            return GetSystemUnixTime() + timeDifference;
        }

        public static async Task<long> GetSteamTimeAsync()
        {
            if (!inSync)
            {
                await SyncTimeAsync();
            }
            return GetSystemUnixTime() + timeDifference;
        }

        private static void SyncTime()
        {
            long currentTime = GetSystemUnixTime();
            using (WebClient client = new WebClient())
            {
                try
                {
                    string response = client.UploadString(SteamAPIEndpoints.TWO_FACTOR_TIME_QUERY, "steamid=0");
                    TimeQuery query = JsonConvert.DeserializeObject<TimeQuery>(response);
                    timeDifference = (int)(query.Response.ServerTime - currentTime);
                    inSync = true;
                }
                catch (WebException)
                {
                    return;
                }
            }
        }

        public static async Task SyncTimeAsync()
        {
            long currentTime = GetSystemUnixTime();
            WebClient client = new WebClient();
            try
            {
                string response = await client.UploadStringTaskAsync(new Uri(SteamAPIEndpoints.TWO_FACTOR_TIME_QUERY), "steamid=0");
                TimeQuery query = JsonConvert.DeserializeObject<TimeQuery>(response);
                timeDifference = (int)(query.Response.ServerTime - currentTime);
                inSync = true;
            }
            catch (WebException)
            {
                return;
            }
        }

        internal class TimeQuery
        {
            [JsonProperty("response")]
            internal TimeQueryResponse Response { get; set; }

            internal class TimeQueryResponse
            {
                [JsonProperty("server_time")]
                public long ServerTime { get; set; }
            }

        }
    }
}
