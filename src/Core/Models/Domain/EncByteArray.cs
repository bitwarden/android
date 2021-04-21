namespace Bit.Core.Models.Domain
{
    public class EncByteArray
    {
        public byte[] Buffer { get; }

        public EncByteArray(byte[] encryptedByteArray)
        {
            Buffer = encryptedByteArray;
        }
    }
}
