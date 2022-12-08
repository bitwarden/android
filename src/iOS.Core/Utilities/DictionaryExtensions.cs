using System;
using System.Collections.Generic;
using System.Linq;
using Foundation;
using Newtonsoft.Json;

namespace Bit.iOS.Core.Utilities
{
    public static class DictionaryExtensions
    {
        public static NSDictionary<NSString, NSObject> ToNSDictionary(this Dictionary<string, object> dict)
        {
            return dict.ToNSDictionary(k => new NSString(k), v => (NSObject)new NSString(JsonConvert.SerializeObject(v)));
        }

        public static NSDictionary<KTo,VTo> ToNSDictionary<KFrom,VFrom,KTo,VTo>(this Dictionary<KFrom, VFrom> dict, Func<KFrom, KTo> keyConverter, Func<VFrom, VTo> valueConverter)
            where KTo : NSObject
            where VTo : NSObject
        {
            var NSValues = dict.Values.Select(x => valueConverter(x)).ToArray();
            var NSKeys = dict.Keys.Select(x => keyConverter(x)).ToArray();
            return NSDictionary<KTo, VTo>.FromObjectsAndKeys(NSValues, NSKeys, NSKeys.Count());
        }
    }
}

