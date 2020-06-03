using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;

namespace Bit.Core.Utilities
{
    public enum EEFLongWordListOffsets
    {
        First_3_chars = 0,
        Last_3_chars = First_3_chars + 81,

        First_4_chars = Last_3_chars + 1,
        Last_4_chars = First_4_chars + 466,

        First_5_chars = Last_4_chars + 1,
        Last_5_chars = First_5_chars + 927,

        First_6_chars = Last_5_chars + 1,
        Last_6_chars = First_6_chars + 1371,

        First_7_chars = Last_6_chars + 1,
        Last_7_chars = First_7_chars + 1590,

        First_8_chars = Last_7_chars + 1,
        Last_8_chars = First_8_chars + 1778,

        First_9_chars = Last_8_chars + 1,
        Last_9_chars = First_9_chars + 1556,

        First = First_3_chars,
        Last = Last_9_chars,
        Elements = Last + 1,
        LengthShortest = 3,
        LengthLongest = 9,
    }

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
