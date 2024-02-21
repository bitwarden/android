using System.Runtime.InteropServices;
using Foundation;

namespace Bit.iOS.Core.Utilities
{
    public static class NSDataExtensions
    {
        public static byte[] ToByteArray(this NSData data)
        {
            var bytes = new byte[data.Length];
            Marshal.Copy(data.Bytes, bytes, 0, Convert.ToInt32(data.Length));
            return bytes;
        }
    }
}
