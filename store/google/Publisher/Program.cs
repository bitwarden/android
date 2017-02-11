using System;
using System.Threading.Tasks;
using Google.Apis.AndroidPublisher.v2;
using Google.Apis.Services;
using System.Collections.Generic;
using Google.Apis.AndroidPublisher.v2.Data;
using System.IO;
using Google.Apis.Auth.OAuth2;

namespace Bit.Publisher
{
    class Program
    {
        private const string Package = "com.x8bit.bitwarden";

        private static string _apkFilePath;
        private static string _credsFilePath;
        private static EditsResource.TracksResource.UpdateRequest.TrackEnum _track;

        static void Main(string[] args)
        {
            if(args.Length < 3)
            {
                throw new ArgumentException("Not enough arguments.");
            }

            try
            {
                _credsFilePath = args[0];
                _apkFilePath = args[1];

                var track = args[2].Substring(0, 1).ToLower();
                if(args[2] == "a")
                {
                    _track = EditsResource.TracksResource.UpdateRequest.TrackEnum.Alpha;
                }
                else if(args[2] == "b")
                {
                    _track = EditsResource.TracksResource.UpdateRequest.TrackEnum.Beta;
                }
                else if(args[2] == "p")
                {
                    _track = EditsResource.TracksResource.UpdateRequest.TrackEnum.Production;
                }
                else if(args[2] == "r")
                {
                    _track = EditsResource.TracksResource.UpdateRequest.TrackEnum.Rollout;
                }

                new Program().Run().Wait();
            }
            catch(AggregateException ex)
            {
                foreach(var e in ex.InnerExceptions)
                {
                    Console.WriteLine("ERROR: " + e.Message);
                }

                throw;
            }
        }

        private async Task Run()
        {
            GoogleCredential creds;
            using(var stream = new FileStream(_credsFilePath, FileMode.Open))
            {
                creds = GoogleCredential.FromStream(stream).CreateScoped(AndroidPublisherService.Scope.Androidpublisher);
            }

            var service = new AndroidPublisherService(new BaseClientService.Initializer
            {
                HttpClientInitializer = creds
            });

            var editRequest = service.Edits.Insert(null, Package);
            var edit = await editRequest.ExecuteAsync();

            Console.WriteLine("Created edit with id {0}.", edit.Id);

            Apk apk = null;
            using(var stream = new FileStream(_apkFilePath, FileMode.Open))
            {
                var uploadMedia = service.Edits.Apks.Upload(
                    Package,
                    edit.Id,
                    stream,
                    "application/vnd.android.package-archive");

                var progress = await uploadMedia.UploadAsync();
                if(progress.Status == Google.Apis.Upload.UploadStatus.Completed)
                {
                    apk = uploadMedia.ResponseBody;
                }
                else
                {
                    throw new Exception("Upload failed.");
                }
            }

            Console.WriteLine("Version code {0} has been uploaded.", apk.VersionCode);

            var trackRequest = service.Edits.Tracks.Update(
                new Track
                {
                    VersionCodes = new List<int?> { apk.VersionCode }
                },
                Package,
                edit.Id,
                _track);
            var updatedTrack = await trackRequest.ExecuteAsync();

            Console.WriteLine("Track {0} has been updated.", updatedTrack.TrackValue);

            var commitRequest = service.Edits.Commit(Package, edit.Id);
            var commitEdit = await commitRequest.ExecuteAsync();

            Console.WriteLine("App edit with id {0} has been comitted.", commitEdit.Id);
        }
    }
}
