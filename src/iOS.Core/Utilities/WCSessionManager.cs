using System.Collections.Generic;
using System.Linq;
using Foundation;
using Newtonsoft.Json;
using System;

namespace WatchConnectivity
{
    public sealed class WCSessionManager : WCSessionDelegate
    {
        // Setup is converted from https://www.natashatherobot.com/watchconnectivity-say-hello-to-wcsession/ 
        // with some extra bits
        private static readonly WCSessionManager sharedManager = new WCSessionManager();
        private static WCSession session = WCSession.IsSupported ? WCSession.DefaultSession : null;

#if __IOS__
        public static string Device = "Phone";
#else
		public static string Device = "Watch";
#endif

        public event WCSessionReceiveDataHandler ApplicationContextUpdated;
        public event WCSessionReceiveDataHandler MessagedReceived;
        public delegate void WCSessionReceiveDataHandler(WCSession session, Dictionary<string, object> applicationContext);


        private WCSession validSession
        {
            get
            {
#if __IOS__
                Console.WriteLine($"Paired status:{(session.Paired ? '✓' : '✗')}\n");
                Console.WriteLine($"Watch App Installed status:{(session.WatchAppInstalled ? '✓' : '✗')}\n");
                return (session.Paired && session.WatchAppInstalled) ? session : null;
#else
				return session;
#endif
            }
        }

        private WCSession validReachableSession
        {
            get
            {
                return session.Reachable ? validSession : null;
            }
        }

        public bool IsSessionReachable => session.Reachable;

        private WCSessionManager() : base() { }

        public static WCSessionManager SharedManager
        {
            get
            {
                return sharedManager;
            }
        }

        public void StartSession()
        {
            if (session != null)
            {
                session.Delegate = this;
                session.ActivateSession();
                Console.WriteLine($"Started Watch Connectivity Session on {Device}");
            }
        }

        public override void SessionReachabilityDidChange(WCSession session)
        {
            Console.WriteLine($"Watch connectivity Reachable:{(session.Reachable ? '✓' : '✗')} from {Device}");
            // handle session reachability change
            if (session.Reachable)
            {
                // great! continue on with Interactive Messaging
            }
            else
            {
                // 😥 prompt the user to unlock their iOS device
            }
        }

        #region Application Context Methods

        public void UpdateApplicationContext(Dictionary<string, object> applicationContext)
        {
            // Application context doesnt need the watch to be reachable, it will be received when opened
            if (validSession != null)
            {
                try
                {
                    var NSValues = applicationContext.Values.Select(x => new NSString(JsonConvert.SerializeObject(x))).ToArray();
                    var NSKeys = applicationContext.Keys.Select(x => new NSString(x)).ToArray();
                    var NSApplicationContext = NSDictionary<NSString, NSObject>.FromObjectsAndKeys(NSValues, NSKeys);
                    NSError error;
                    var sendSuccessfully = validSession.UpdateApplicationContext(NSApplicationContext, out error);
                    if (sendSuccessfully)
                    {
                        Console.WriteLine($"Sent App Context from {Device} \nPayLoad: {NSApplicationContext.ToString()} \n");
                    }
                    else
                    {
                        Console.WriteLine($"Error Updating Application Context: {error.LocalizedDescription}");
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Exception Updating Application Context: {ex.Message}");
                }
            }
        }

        public void SendMessage(Dictionary<string, object> message)
        {
            if(validSession is null)
            {
                return;
            }

            var keys = message.Keys.Select(k => new NSString(k)).ToArray();
            var values = message.Values.Select(v => JsonConvert.SerializeObject(v)).ToArray();
            NSDictionary<NSString, NSObject> dic = NSDictionary<NSString, NSObject>.FromObjectsAndKeys(values, keys);
            session.SendMessage(dic, null, error =>
            {
                Console.WriteLine(error?.ToString());
            });
        }

        public override void DidReceiveApplicationContext(WCSession session, NSDictionary<NSString, NSObject> applicationContext)
        {
            Console.WriteLine($"Receiving Message on {Device}");
            if (ApplicationContextUpdated != null)
            {
                var keys = applicationContext.Keys.Select(k => k.ToString()).ToArray();
                var values = applicationContext.Values.Select(v => JsonConvert.DeserializeObject(v.ToString())).ToArray();
                var dictionary = keys.Zip(values, (k, v) => new { Key = k, Value = v })
                                     .ToDictionary(x => x.Key, x => x.Value);

                ApplicationContextUpdated(session, dictionary);
            }
        }


        public override void DidReceiveMessage(WCSession session, NSDictionary<NSString, NSObject> message)
        {
            Console.WriteLine($"Receiving Message on {Device}");

            var keys = message.Keys.Select(k => k.ToString()).ToArray();
            var values = message.Values.Select(v => v?.ToString() as object).ToArray();
            var dictionary = keys.Zip(values, (k, v) => new { Key = k, Value = v })
                                 .ToDictionary(x => x.Key, x => x.Value);

            MessagedReceived?.Invoke(session, dictionary);
        }

        #endregion
    }
}
