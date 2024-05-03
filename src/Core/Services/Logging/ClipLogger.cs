//using System.Runtime.CompilerServices;
//using System.Text;
//using Bit.Core.Abstractions;

//#if IOS
//using UIKit;
//#elif ANDROID
//using Android.Content;
//#endif

//namespace Bit.Core.Services
//{
//   /// <summary>
//   /// This logger can be used to help debug iOS extensions where we cannot use the .NET debugger yet
//   /// so we can use this that copies the logs to the clipboard so one
//   /// can paste them and analyze its output.
//   /// </summary>
//   public class ClipLogger : ILogger
//   {
//       private static readonly StringBuilder _currentBreadcrumbs = new StringBuilder();

//       static ILogger _instance;
//       public static ILogger Instance
//       {
//           get
//           {
//               if (_instance is null)
//               {
//                   _instance = new ClipLogger();
//               }
//               return _instance;
//           }
//       }

//       protected ClipLogger()
//       {
//       }

//       public static void Log(string breadcrumb)
//       {
//            var formattedText = $"{DateTime.Now.ToShortTimeString()}: {breadcrumb}";
//            _currentBreadcrumbs.AppendLine(formattedText);

//#if IOS
//            MainThread.BeginInvokeOnMainThread(() => UIPasteboard.General.String = _currentBreadcrumbs.ToString());
//#elif ANDROID
//            var clipboardManager = Android.App.Application.Context.GetSystemService(Context.ClipboardService) as ClipboardManager;
//            var clipData = ClipData.NewPlainText("bitwarden", _currentBreadcrumbs.ToString());
//            clipboardManager.PrimaryClip = clipData;
//            MainThread.BeginInvokeOnMainThread(() => UIPasteboard.General.String = _currentBreadcrumbs.ToString());
//#endif
//        }

//       public void Error(string message, IDictionary<string, string> extraData = null, [CallerMemberName] string memberName = "", [CallerFilePath] string sourceFilePath = "", [CallerLineNumber] int sourceLineNumber = 0)
//       {
//           var classAndMethod = $"{Path.GetFileNameWithoutExtension(sourceFilePath)}.{memberName}";
//           var filePathAndLineNumber = $"{Path.GetFileName(sourceFilePath)}:{sourceLineNumber}";
//           var properties = new Dictionary<string, string>
//           {
//               ["File"] = filePathAndLineNumber,
//               ["Method"] = memberName
//           };

//           Log(message ?? $"Error found in: {classAndMethod}, {filePathAndLineNumber}");
//       }

//       public void Exception(Exception ex) => Log(ex?.ToString());

//       public Task InitAsync() => Task.CompletedTask;

//       public Task<bool> IsEnabled() => Task.FromResult(true);

//       public Task SetEnabled(bool value) => Task.CompletedTask;
//   }
//}
