namespace Bit.Core.Models.Domain
{
    public class CipherByteArray
    {
        public byte[] Buffer { get; }

        public CipherByteArray(byte[] encryptedByteArray)
        {
            Buffer = encryptedByteArray;
        }
    }
}
