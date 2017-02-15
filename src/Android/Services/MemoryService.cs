using System;
using Android.Content;
using Bit.App.Abstractions;
using Xamarin.Forms;

namespace Bit.Android.Services
{
    public class MemoryService : IMemoryService
    {
        public MemoryInfo GetInfo()
        {
            return MemoryHelper.GetMemoryInfo(Forms.Context);
        }

        public void Check()
        {
            MemoryHelper.MemoryCheck(Forms.Context);
        }

        public static class MemoryHelper
        {
            public static void MemoryCheck(Context context)
            {
                Console.WriteLine("MemoryHelper.MemoryCheck.{0} - {1}", "Start", context.ToString());
                var maxMemory = Java.Lang.Runtime.GetRuntime().MaxMemory();
                var freeMemory = Java.Lang.Runtime.GetRuntime().FreeMemory();
                var percentUsed = (maxMemory - freeMemory) / (double)maxMemory;
                Console.WriteLine("Free memory: {0:N}", freeMemory);
                Console.WriteLine("Max memory: {0:N}", maxMemory);
                Console.WriteLine("% used: {0:P}", percentUsed);
                Console.WriteLine("MemoryHelper.MemoryCheck.{0} {3:P} {1} out of {2}", "End", freeMemory, maxMemory, percentUsed);
            }

            public static MemoryInfo GetMemoryInfo(Context context)
            {
                var retVal = new MemoryInfo();
                retVal.MaxMemory = Java.Lang.Runtime.GetRuntime().MaxMemory();
                retVal.FreeMemory = Java.Lang.Runtime.GetRuntime().FreeMemory();
                retVal.TotalMemory = Java.Lang.Runtime.GetRuntime().TotalMemory();
                return retVal;
            }
        }
    }
}
