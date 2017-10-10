using System;
using System.Collections.Generic;
using Android.App;
using Android.Content;
using Android.OS;
using Bit.App.Abstractions;
using Bit.App.Utilities;
using System.Threading;
using Xamarin.Forms;
using Android.Gms.Gcm.Iid;
using Android.Gms.Gcm;
using Java.IO;
using Newtonsoft.Json.Linq;
using Android.Support.V4.App;
using Android.Media;
using Newtonsoft.Json;

namespace Bit.Android.Services
{
    public class AndroidPushNotificationService : IPushNotificationService
    {
        private const string GcmPreferencesKey = "GCMPreferences";
        private const string Tag = "PushNotification";

        internal static IPushNotificationListener Listener { get; set; }
        public string Token => GetRegistrationId();

        public void Register()
        {

            System.Diagnostics.Debug.WriteLine(
                $"{PushNotificationContants.DomainName} - Register -  Registering push notifications");

            if(string.IsNullOrEmpty(CrossPushNotification.SenderId))
            {
                System.Diagnostics.Debug.WriteLine(
                    $"{PushNotificationContants.DomainName} - Register - SenderId is missing.");

                CrossPushNotification.PushNotificationListener.OnError(
                    $"{PushNotificationContants.DomainName} - Register - Sender Id is missing.", Device.Android);
            }
            else
            {
                System.Diagnostics.Debug.WriteLine(
                    $"{PushNotificationContants.DomainName} - Register -  Registering for Push Notifications");

                ThreadPool.QueueUserWorkItem(state =>
                {
                    try
                    {
                        Intent intent = new Intent(global::Android.App.Application.Context,
                            typeof(PushNotificationRegistrationIntentService));

                        global::Android.App.Application.Context.StartService(intent);
                    }
                    catch(Exception ex)
                    {
                        System.Diagnostics.Debug.WriteLine($"{Tag} - Error : {ex.Message}");
                        CrossPushNotification.PushNotificationListener.OnError($"{Tag} - Register - {ex.Message}",
                            Device.Android);
                    }
                });
            }
        }

        public void Unregister()
        {
            ThreadPool.QueueUserWorkItem(state =>
            {
                System.Diagnostics.Debug.WriteLine(
                    $"{PushNotificationContants.DomainName} - Unregister -  Unregistering push notifications");
                try
                {
                    var instanceID = InstanceID.GetInstance(global::Android.App.Application.Context);
                    instanceID.DeleteToken(CrossPushNotification.SenderId, GoogleCloudMessaging.InstanceIdScope);

                    CrossPushNotification.PushNotificationListener.OnUnregistered(Device.Android);
                    StoreRegistrationId(global::Android.App.Application.Context, string.Empty);

                }
                catch(IOException ex)
                {
                    System.Diagnostics.Debug.WriteLine($"{Tag} - Error : {ex.Message}");
                    CrossPushNotification.PushNotificationListener.OnError(
                        $"{Tag} - Unregister - {ex.Message}", Device.Android);
                }
            });
        }

        private string GetRegistrationId()
        {
            var context = global::Android.App.Application.Context;
            var prefs = GetGCMPreferences(context);
            var registrationId = prefs.GetString(PushNotificationContants.Token, string.Empty);
            if(string.IsNullOrEmpty(registrationId))
            {
                System.Diagnostics.Debug.WriteLine($"{PushNotificationContants.DomainName} - Registration not found.");
                return string.Empty;
            }

            // Check if app was updated; if so, it must clear the registration ID
            // since the existing registration ID is not guaranteed to work with
            // the new app version.
            var registeredVersion = prefs.GetInt(PushNotificationContants.AppVersion, Java.Lang.Integer.MinValue);
            var currentVersion = GetAppVersion(context);
            if(registeredVersion != currentVersion)
            {
                System.Diagnostics.Debug.WriteLine($"{PushNotificationContants.DomainName} - App version changed.");
                return string.Empty;
            }

            return registrationId;
        }

        internal static ISharedPreferences GetGCMPreferences(Context context)
        {
            return context.GetSharedPreferences(GcmPreferencesKey, FileCreationMode.Private);
        }

        internal static int GetAppVersion(Context context)
        {
            try
            {
                var packageInfo = context.PackageManager.GetPackageInfo(context.PackageName, 0);
                return packageInfo.VersionCode;
            }
            catch(global::Android.Content.PM.PackageManager.NameNotFoundException e)
            {
                // should never happen
                throw new Java.Lang.RuntimeException("Could not get package name: " + e);
            }
        }

        internal static void StoreRegistrationId(Context context, string regId)
        {
            var prefs = GetGCMPreferences(context);
            var appVersion = GetAppVersion(context);

            System.Diagnostics.Debug.WriteLine(
                $"{PushNotificationContants.DomainName} - Saving token on app version {appVersion}");

            var editor = prefs.Edit();
            editor.PutString(PushNotificationContants.Token, regId);
            editor.PutInt(PushNotificationContants.AppVersion, appVersion);
            editor.Commit();
        }
    }

    [Service(Exported = false)]
    public class PushNotificationRegistrationIntentService : IntentService
    {
        private const string Tag = "PushNotificationRegistationIntentService";
        private string[] _topics = new string[] { "global" };
        private readonly object _syncLock = new object();

        protected override void OnHandleIntent(Intent intent)
        {
            try
            {
                var extras = intent.Extras;
                lock(_syncLock)
                {
                    var instanceID = InstanceID.GetInstance(global::Android.App.Application.Context);
                    var token = instanceID.GetToken(CrossPushNotification.SenderId,
                        GoogleCloudMessaging.InstanceIdScope, null);
                    CrossPushNotification.PushNotificationListener.OnRegistered(token, Device.Android);
                    AndroidPushNotificationService.StoreRegistrationId(global::Android.App.Application.Context, token);
                    SubscribeTopics(token);
                    System.Diagnostics.Debug.WriteLine($"{token} - Device registered, registration ID={Tag}");
                }

            }
            catch(Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"{ex.Message} - Error : {Tag}");
                CrossPushNotification.PushNotificationListener.OnError(
                    $"{ex.ToString()} - Register - {Tag}", Device.Android);
            }

        }

        private void SubscribeTopics(string token)
        {
            var pubSub = GcmPubSub.GetInstance(this);
            foreach(var topic in _topics)
            {
                pubSub.Subscribe(token, "/topics/" + topic, null);
            }
        }
    }

    [Service(Exported = false)]
    [IntentFilter(new string[] { "com.google.android.gms.iid.InstanceID" })]
    public class PushNotificationInstanceIDListenerService : InstanceIDListenerService
    {
        private const string Tag = "PushNotificationInstanceIDLS";

        public override void OnTokenRefresh()
        {
            base.OnTokenRefresh();
            ThreadPool.QueueUserWorkItem(state =>
            {
                try
                {
                    var intent = new Intent(global::Android.App.Application.Context,
                        typeof(PushNotificationRegistrationIntentService));
                    global::Android.App.Application.Context.StartService(intent);
                }
                catch(Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine($"{ex.Message} - Error : {Tag}");
                    CrossPushNotification.PushNotificationListener.OnError(
                        $"{ex.ToString()} - Register - {Tag}", Device.Android);
                }
            });
        }
    }

    [Service(Exported = false, Name = "pushnotification.plugin.PushNotificationGcmListener")]
    [IntentFilter(new string[] { "com.google.android.c2dm.intent.RECEIVE" },
        Categories = new string[] { "com.x8bit.bitwarden" })]
    public class PushNotificationGcmListener : GcmListenerService
    {
        public override void OnMessageReceived(string from, Bundle extras)
        {
            if(extras != null && !extras.IsEmpty)
            {
                System.Diagnostics.Debug.WriteLine(
                    $"{PushNotificationContants.DomainName} - GCM Listener - Push Received");

                var parameters = new Dictionary<string, object>();
                var values = new JObject();
                foreach(var key in extras.KeySet())
                {
                    var value = extras.Get(key).ToString();

                    if(ValidateJSON(value))
                    {
                        values.Add(key, JObject.Parse(value));
                    }
                    else
                    {
                        values.Add(key, value);
                    }

                    parameters.Add(key, extras.Get(key));
                    System.Diagnostics.Debug.WriteLine(
                        $"{PushNotificationContants.DomainName} - GCM Listener - Push Params {key} : {extras.Get(key)}");
                }

                var context = global::Android.App.Application.Context;
                CrossPushNotification.PushNotificationListener.OnMessage(values, Device.Android);

                try
                {
                    var notifyId = 0;
                    var title = context.ApplicationInfo.LoadLabel(context.PackageManager);
                    var message = string.Empty;
                    var tag = string.Empty;

                    if(!string.IsNullOrEmpty(CrossPushNotification.NotificationContentTextKey) &&
                        parameters.ContainsKey(CrossPushNotification.NotificationContentTextKey))
                    {
                        message = parameters[CrossPushNotification.NotificationContentTextKey].ToString();
                    }
                    else if(parameters.ContainsKey(PushNotificationContants.Alert))
                    {
                        message = parameters[PushNotificationContants.Alert].ToString();
                    }
                    else if(parameters.ContainsKey(PushNotificationContants.Message))
                    {
                        message = parameters[PushNotificationContants.Message].ToString();
                    }
                    else if(parameters.ContainsKey(PushNotificationContants.Subtitle))
                    {
                        message = parameters[PushNotificationContants.Subtitle].ToString();
                    }
                    else if(parameters.ContainsKey(PushNotificationContants.Text))
                    {
                        message = parameters[PushNotificationContants.Text].ToString();
                    }

                    if(!string.IsNullOrEmpty(CrossPushNotification.NotificationContentTitleKey) &&
                        parameters.ContainsKey(CrossPushNotification.NotificationContentTitleKey))
                    {
                        title = parameters[CrossPushNotification.NotificationContentTitleKey].ToString();
                    }
                    else if(parameters.ContainsKey(PushNotificationContants.Title))
                    {
                        if(!string.IsNullOrEmpty(message))
                        {
                            title = parameters[PushNotificationContants.Title].ToString();
                        }
                        else
                        {
                            message = parameters[PushNotificationContants.Title].ToString();
                        }
                    }

                    if(string.IsNullOrEmpty(message))
                    {
                        var data = (
                            !string.IsNullOrEmpty(CrossPushNotification.NotificationContentDataKey) &&
                            values[CrossPushNotification.NotificationContentDataKey] != null) ?
                                values[CrossPushNotification.NotificationContentDataKey] :
                                values[PushNotificationContants.Data];

                        if(data != null)
                        {
                            if(!string.IsNullOrEmpty(CrossPushNotification.NotificationContentTextKey) &&
                                data[CrossPushNotification.NotificationContentTextKey] != null)
                            {
                                message = data[CrossPushNotification.NotificationContentTextKey].ToString();
                            }
                            else if(data[PushNotificationContants.Alert] != null)
                            {
                                message = data[PushNotificationContants.Alert].ToString();
                            }
                            else if(data[PushNotificationContants.Message] != null)
                            {
                                message = data[PushNotificationContants.Message].ToString();
                            }
                            else if(data[PushNotificationContants.Subtitle] != null)
                            {
                                message = data[PushNotificationContants.Subtitle].ToString();
                            }
                            else if(data[PushNotificationContants.Text] != null)
                            {
                                message = data[PushNotificationContants.Text].ToString();
                            }

                            if(!string.IsNullOrEmpty(CrossPushNotification.NotificationContentTitleKey) &&
                                data[CrossPushNotification.NotificationContentTitleKey] != null)
                            {
                                title = data[CrossPushNotification.NotificationContentTitleKey].ToString();
                            }
                            else if(data[PushNotificationContants.Title] != null)
                            {
                                if(!string.IsNullOrEmpty(message))
                                {
                                    title = data[PushNotificationContants.Title].ToString();
                                }
                                else
                                {
                                    message = data[PushNotificationContants.Title].ToString();
                                }
                            }
                        }
                    }

                    if(parameters.ContainsKey(PushNotificationContants.Id))
                    {
                        var str = parameters[PushNotificationContants.Id].ToString();
                        try
                        {
                            notifyId = Convert.ToInt32(str);
                        }
                        catch(Exception)
                        {
                            // Keep the default value of zero for the notify_id, but log the conversion problem.
                            System.Diagnostics.Debug.WriteLine("Failed to convert {0} to an integer", str);
                        }
                    }

                    if(parameters.ContainsKey(PushNotificationContants.Tag))
                    {
                        tag = parameters[PushNotificationContants.Tag].ToString();
                    }

                    if(!parameters.ContainsKey(PushNotificationContants.Silent) ||
                        !System.Boolean.Parse(parameters[PushNotificationContants.Silent].ToString()))
                    {
                        if(CrossPushNotification.PushNotificationListener.ShouldShowNotification())
                        {
                            CreateNotification(title, message, notifyId, tag, extras);
                        }
                    }

                }
                catch(Java.Lang.Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine(ex.ToString());
                }
                catch(Exception ex1)
                {
                    System.Diagnostics.Debug.WriteLine(ex1.ToString());
                }
            }
        }

        private void CreateNotification(string title, string message, int notifyId, string tag, Bundle extras)
        {
            System.Diagnostics.Debug.WriteLine(
                $"{PushNotificationContants.DomainName} - PushNotification - Message {title} : {message}");

            NotificationCompat.Builder builder = null;
            var context = global::Android.App.Application.Context;

            if(CrossPushNotification.SoundUri == null)
            {
                CrossPushNotification.SoundUri = RingtoneManager.GetDefaultUri(RingtoneType.Notification);
            }

            try
            {
                if(CrossPushNotification.IconResource == 0)
                {
                    CrossPushNotification.IconResource = context.ApplicationInfo.Icon;
                }
                else
                {
                    var name = context.Resources.GetResourceName(CrossPushNotification.IconResource);
                    if(name == null)
                    {
                        CrossPushNotification.IconResource = context.ApplicationInfo.Icon;
                    }
                }
            }
            catch(global::Android.Content.Res.Resources.NotFoundException ex)
            {
                CrossPushNotification.IconResource = context.ApplicationInfo.Icon;
                System.Diagnostics.Debug.WriteLine(ex.ToString());
            }

            var resultIntent = context.PackageManager.GetLaunchIntentForPackage(context.PackageName);
            if(extras != null)
            {
                resultIntent.PutExtras(extras);
            }

            // Create a PendingIntent; we're only using one PendingIntent (ID = 0):
            const int pendingIntentId = 0;
            var resultPendingIntent = PendingIntent.GetActivity(context, pendingIntentId,
                resultIntent, PendingIntentFlags.OneShot);

            // Build the notification
            builder = new NotificationCompat.Builder(context)
                      .SetAutoCancel(true) // dismiss the notification from the notification area when the user clicks on it
                      .SetContentIntent(resultPendingIntent) // start up this activity when the user clicks the intent.
                      .SetContentTitle(title) // Set the title
                      .SetSound(CrossPushNotification.SoundUri)
                      .SetSmallIcon(CrossPushNotification.IconResource) // This is the icon to display
                      .SetContentText(message); // the message to display.

            if(Build.VERSION.SdkInt >= BuildVersionCodes.JellyBean)
            {
                // Using BigText notification style to support long message
                var style = new NotificationCompat.BigTextStyle();
                style.BigText(message);
                builder.SetStyle(style);
            }

            var notificationManager = (NotificationManager)context.GetSystemService(NotificationService);
            notificationManager.Notify(tag, notifyId, builder.Build());
        }

        private static bool ValidateJSON(string s)
        {
            try
            {
                JObject.Parse(s);
                return true;
            }
            catch(JsonReaderException ex)
            {
                System.Diagnostics.Debug.WriteLine(ex.ToString());
                return false;
            }
        }
    }

    [BroadcastReceiver(Exported = true, Permission = "com.google.android.c2dm.permission.SEND")]
    [IntentFilter(new string[] { "com.google.android.c2dm.intent.RECEIVE" },
        Categories = new string[] { "com.x8bit.bitwarden" })]
    public class PushNotificationsReceiver : GcmReceiver
    { }

    [Service]
    public class AndroidPushService : Service
    {
        public override void OnCreate()
        {
            base.OnCreate();
            System.Diagnostics.Debug.WriteLine("Push Notification Service - Created");
        }

        public override StartCommandResult OnStartCommand(Intent intent, StartCommandFlags flags, int startId)
        {
            System.Diagnostics.Debug.WriteLine("Push Notification Service - Started");
            return StartCommandResult.Sticky;
        }

        public override IBinder OnBind(Intent intent)
        {
            System.Diagnostics.Debug.WriteLine("Push Notification Service - Binded");
            return null;
        }

        public override void OnDestroy()
        {
            System.Diagnostics.Debug.WriteLine("Push Notification Service - Destroyed");
            base.OnDestroy();
        }
    }

    internal class CrossPushNotification
    {
        private static Lazy<IPushNotificationService> Implementation = new Lazy<IPushNotificationService>(
            () => new AndroidPushNotificationService(),
            LazyThreadSafetyMode.PublicationOnly);
        public static bool IsInitialized => PushNotificationListener != null;
        public static IPushNotificationListener PushNotificationListener { get; private set; }

        public static string SenderId { get; set; }
        public static string NotificationContentTitleKey { get; set; }
        public static string NotificationContentTextKey { get; set; }
        public static string NotificationContentDataKey { get; set; }
        public static int IconResource { get; set; }
        public static global::Android.Net.Uri SoundUri { get; set; }

        public static void Initialize<T>(T listener, string senderId) where T : IPushNotificationListener
        {
            SenderId = senderId;

            if(PushNotificationListener == null)
            {
                PushNotificationListener = listener;
                System.Diagnostics.Debug.WriteLine("PushNotification plugin initialized.");
            }
            else
            {
                System.Diagnostics.Debug.WriteLine("PushNotification plugin already initialized.");
            }
        }

        public static void Initialize<T>(string senderId) where T : IPushNotificationListener, new()
        {
            Initialize(new T(), senderId);
        }

        public static IPushNotificationService Current
        {
            get
            {
                if(!IsInitialized)
                {
                    throw new Exception("Not initialized.");
                }

                var ret = Implementation.Value;
                if(ret == null)
                {
                    throw new Exception("Not in PCL");
                }

                return ret;
            }
        }
    }
}
