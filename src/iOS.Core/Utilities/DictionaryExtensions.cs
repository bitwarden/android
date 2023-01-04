using System;
using System.Collections.Generic;
using System.Linq;
using Bit.Core.Models.Domain;
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

        public static Dictionary<string, object> ToDictionary(this NSDictionary<NSString, NSObject> nsDict)
        {
            return nsDict.ToDictionary(v => v?.ToString() as object);
        }

        public static Dictionary<string, object> ToDictionary(this NSDictionary<NSString, NSObject> nsDict, Func<NSObject, object> valueTransformer)
        {
            return nsDict.ToDictionary(k => k.ToString(), v => valueTransformer(v));
        }

        public static Dictionary<KTo, VTo> ToDictionary<KFrom, VFrom, KTo, VTo>(this NSDictionary<KFrom, VFrom> nsDict, Func<KFrom, KTo> keyConverter, Func<VFrom, VTo> valueConverter)
            where KFrom : NSObject
            where VFrom : NSObject
        {
            var keys = nsDict.Keys.Select(k => keyConverter(k)).ToArray();
            var values = nsDict.Values.Select(v => valueConverter(v)).ToArray();
            return keys.Zip(values, (k, v) => new { Key = k, Value = v })
                       .ToDictionary(x => x.Key, x => x.Value);
        }
    }
}
