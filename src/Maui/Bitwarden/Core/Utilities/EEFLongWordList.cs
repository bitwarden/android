using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace Bit.Core.Utilities
{
    public class EEFLongWordList
    {
        private static volatile EEFLongWordList _uniqueInstance;
        private static object _syncObj = new object();

        private EEFLongWordList()
        {
            List = ReadData().ToList();
        }

        public static EEFLongWordList Instance
        {
            get
            {
                if (_uniqueInstance == null)
                {
                    lock (_syncObj)
                    {
                        if (_uniqueInstance == null)
                        {
                            _uniqueInstance = new EEFLongWordList();
                        }
                    }
                }
                return (_uniqueInstance);
            }
        }

        public List<string> List { get; set; }

        private IEnumerable<string> ReadData()
        {
            var assembly = typeof(EEFLongWordList).GetTypeInfo().Assembly;
            var stream = assembly.GetManifestResourceStream("Bit.Core.Resources.eff_long_word_list.txt");
            string line;
            using (var reader = new StreamReader(stream))
            {
                while ((line = reader.ReadLine()) != null)
                {
                    yield return line;
                }
            }
        }
    }
}
