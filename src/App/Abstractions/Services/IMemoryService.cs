namespace Bit.App.Abstractions
{
    public interface IMemoryService
    {
        MemoryInfo GetInfo();
        void Check();
    }

    public class MemoryInfo
    {
        public long FreeMemory { get; set; }
        public long MaxMemory { get; set; }
        public long TotalMemory { get; set; }
        public long UsedMemory => TotalMemory - FreeMemory;

        public double HeapUsage()
        {
            return UsedMemory / (double)TotalMemory;
        }

        public double Usage()
        {
            return UsedMemory / (double)MaxMemory;
        }
    }
}
