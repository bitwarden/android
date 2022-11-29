using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.iOS.Core.Utilities;
using Foundation;
using Newtonsoft.Json;

namespace WatchConnectivity
{
    public sealed class WCSessionManager : WCSessionDelegate
    {
        // Setup is converted from https://www.natashatherobot.com/watchconnectivity-say-hello-to-wcsession/ 
        // with some extra bits
        private static readonly WCSessionManager sharedManager = new WCSessionManager();
        private static WCSession session = WCSession.IsSupported ? WCSession.DefaultSession : null;

        public static string Device = "Phone";

        public event WCSessionReceiveDataHandler ApplicationContextUpdated;
        public event WCSessionReceiveDataHandler MessagedReceived;
        public delegate void WCSessionReceiveDataHandler(WCSession session, Dictionary<string, object> applicationContext);


        private WCSession validSession
        {
            get
            {
                Console.WriteLine($"Paired status:{(session.Paired ? '✓' : '✗')}\n");
                Console.WriteLine($"Watch App Installed status:{(session.WatchAppInstalled ? '✓' : '✗')}\n");
                return (session.Paired && session.WatchAppInstalled) ? session : null;
            }
        }

        private WCSession validReachableSession
        {
            get
            {
                return session.Reachable ? validSession : null;
            }
        }

        public bool IsValidSession => validSession != null;

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

        public void SendBackgroundHighPriorityMessage(Dictionary<string, object> applicationContext)
        {
            // Application context doesnt need the watch to be reachable, it will be received when opened
            if (validSession is null || validSession.ActivationState != WCSessionActivationState.Activated)
            {
                return;
            }

            Xamarin.Forms.Device.BeginInvokeOnMainThread(() =>
            {
                try
                {
                    var sendSuccessfully = validSession.UpdateApplicationContext(applicationContext.ToNSDictionary(), out var error);
                    if (sendSuccessfully)
                    {
                        Console.WriteLine($"Sent App Context from {Device} \nPayLoad: {applicationContext.ToNSDictionary().ToString()} \n");
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
            });
        }
        WCSessionUserInfoTransfer _transf;
        public void SendBackgroundFifoHighPriorityMessage(Dictionary<string, object> message)
        {
            if(validSession is null || validSession.ActivationState != WCSessionActivationState.Activated)
            {
                return;
            }

            _transf?.Cancel();

            Console.WriteLine("Started transferring user info");

            _transf = session.TransferUserInfo(message.ToNSDictionary());


            Task.Run(async () =>
            {
                try
                {
                    while (_transf.Transferring)
                    {
                        await Task.Delay(1000);
                    }
                    Console.WriteLine("Finished transferring user info");
                }
                catch (Exception ex)
                {
                    Console.WriteLine("Error transferring user info " + ex);
                }
            });

            //session.SendMessage(dic,
            //    (dd) =>
            //    {
            //        Console.WriteLine(dd?.ToString());
            //    },
            //    error =>
            //    {
            //        Console.WriteLine(error?.ToString());
            //    }
            //);
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
