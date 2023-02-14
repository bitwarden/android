using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Bit.iOS.Core.Utilities;
using Foundation;
using Newtonsoft.Json;
using ObjCRuntime;

namespace WatchConnectivity
{
    public sealed class WCSessionManager : WCSessionDelegate
    {
        // Setup is converted from https://www.natashatherobot.com/watchconnectivity-say-hello-to-wcsession/ 
        // with some extra bits
        private static readonly WCSessionManager sharedManager = new WCSessionManager();
        private static WCSession session = WCSession.IsSupported ? WCSession.DefaultSession : null;

        public event WCSessionReceiveDataHandler OnApplicationContextUpdated;
        public event WCSessionReceiveDataHandler OnMessagedReceived;
        public delegate void WCSessionReceiveDataHandler(WCSession session, Dictionary<string, object> data);

        WCSessionUserInfoTransfer _transf;

        private WCSession validSession
        {
            get
            {
                Debug.WriteLine($"Paired status:{(session.Paired ? '✓' : '✗')}\n");
                Debug.WriteLine($"Watch App Installed status:{(session.WatchAppInstalled ? '✓' : '✗')}\n");
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

        public bool IsSessionActivated => validSession?.ActivationState == WCSessionActivationState.Activated;

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
                Debug.WriteLine($"Started Watch Connectivity Session");
            }
        }

        public override void SessionReachabilityDidChange(WCSession session)
        {
            Debug.WriteLine($"Watch connectivity Reachable:{(session.Reachable ? '✓' : '✗')}");
        }

        public void SendBackgroundHighPriorityMessage(Dictionary<string, object> applicationContext)
        {
            // Application context doesnt need the watch to be reachable, it will be received when opened
            if (validSession is null || validSession.ActivationState != WCSessionActivationState.Activated)
            {
                return;
            }

            try
            {
                var sendSuccessfully = validSession.UpdateApplicationContext(applicationContext.ToNSDictionary(), out var error);
                if (sendSuccessfully)
                {
                    Debug.WriteLine($"Sent App Context \nPayLoad: {applicationContext.ToNSDictionary().ToString()} \n");
                }
                else
                {
                    Debug.WriteLine($"Error Updating Application Context: {error.LocalizedDescription}");
                }
            }
            catch (Exception ex)
            {
                LoggerHelper.LogEvenIfCantBeResolved(ex);
            }
        }

        public void SendBackgroundFifoHighPriorityMessage(Dictionary<string, object> message)
        {
            if(validSession is null || validSession.ActivationState != WCSessionActivationState.Activated)
            {
                return;
            }

            _transf?.Cancel();

            Debug.WriteLine("Started transferring user info");

            _transf = session.TransferUserInfo(message.ToNSDictionary());

            Task.Run(async () =>
            {
                try
                {
                    while (_transf.Transferring)
                    {
                        await Task.Delay(1000);
                    }
                    Debug.WriteLine("Finished transferring user info");
                }
                catch (Exception ex)
                {
                    LoggerHelper.LogEvenIfCantBeResolved(ex);
                }
            });
        }

        public override void DidReceiveApplicationContext(WCSession session, NSDictionary<NSString, NSObject> applicationContext)
        {
            Debug.WriteLine($"Receiving Message");
            if (OnApplicationContextUpdated != null)
            {
                var keys = applicationContext.Keys.Select(k => k.ToString()).ToArray();
                var values = applicationContext.Values.Select(v => JsonConvert.DeserializeObject(v.ToString())).ToArray();
                var dictionary = keys.Zip(values, (k, v) => new { Key = k, Value = v })
                                     .ToDictionary(x => x.Key, x => x.Value);

                OnApplicationContextUpdated(session, dictionary);
            }
        }

        public override void DidReceiveMessage(WCSession session, NSDictionary<NSString, NSObject> message)
        {
            Debug.WriteLine($"Receiving Message");

            OnMessagedReceived?.Invoke(session, message.ToDictionary());
        }

        public override void DidReceiveMessage(WCSession session, NSDictionary<NSString, NSObject> message, WCSessionReplyHandler replyHandler)
        {
            Debug.WriteLine($"Receiving Message");

            OnMessagedReceived?.Invoke(session, message.ToDictionary());
        }
    }
}
