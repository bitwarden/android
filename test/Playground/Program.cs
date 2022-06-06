using System;
using System.Threading.Tasks;
using Bit.Core.Services;

namespace Bit.Playground
{
    public class Program
    {
        public static void Main(string[] args)
        {
            MainAsync(args).Wait();
            Console.ReadLine();
        }

        public static async Task MainAsync(string[] args)
        {
            var db = new LiteDbStorageService("test.db");
            await db.SaveAsync("testkey", new { val = 1 });
            Console.WriteLine(await db.GetAsync<object>("testkey"));
            Console.ReadLine();
        }
    }
}
